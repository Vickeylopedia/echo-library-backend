package com.example.services

import com.example.config.SupabaseClient
import com.example.models.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.Locale
import java.time.Instant
import java.time.temporal.ChronoUnit

@Serializable
data class SearchResponse(
    val messages: List<PodcastEpisode>,
    val speakers: List<Speaker>,
    val podcasts: List<Podcast>,
    val categories: List<Category>,
    val totalResults: Int,
    val searchTimeMs: Long
)

@Serializable
data class TrendingSearch(
    val query: String,
    val count: Int
)

class SearchEngine(private val supabase: SupabaseClient) {
    private val logger = LoggerFactory.getLogger(SearchEngine::class.java)
    private val jsonHelper = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // Analytics: In-memory tracker for query logging
    private val queryTracker = ConcurrentHashMap<String, AtomicInteger>()
    private val searchDurations = ConcurrentHashMap<String, Long>()

    // Local Query Cache
    private val cache = ConcurrentHashMap<String, CachedResult>()
    private val cacheTtlMs = 60000L // 1 minute Cache Time To Live

    private data class CachedResult(
        val response: SearchResponse,
        val timestamp: Long
    )

    /**
     * Compute Levenshtein distance for fuzzy matching / typo tolerance.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Determine if there is a close match (typo-tolerant) or substring match.
     */
    private fun isMatch(text: String, query: String, threshold: Int = 2): Boolean {
        val t = text.lowercase(Locale.getDefault())
        val q = query.lowercase(Locale.getDefault())
        if (t.contains(q)) return true

        val words = t.split(" ", "-", "_", ",", ".")
        for (word in words) {
            if (word.length >= q.length - 1 && word.length <= q.length + 1) {
                if (levenshteinDistance(word, q) <= threshold) return true
            }
        }
        return false
    }

    /**
     * Score relevance based on occurrences of the search query in titles, hosts, names, descriptions.
     */
    private fun calculateRelevance(text: String, query: String): Int {
        val t = text.lowercase(Locale.getDefault())
        val q = query.lowercase(Locale.getDefault())
        if (t == q) return 100
        if (t.startsWith(q)) return 80
        if (t.contains(q)) return 50
        
        // Sum Levenshtein similarities for parts of words
        var score = 0
        val words = t.split(" ")
        for (word in words) {
            if (word == q) score += 40
            else if (word.contains(q)) score += 20
        }
        return score
    }

    /**
     * Global Search with full filter context, pagination, caching, and custom sort mechanisms.
     */
    suspend fun search(
        q: String,
        providerId: String? = null,
        speakerId: String? = null,
        categoryId: String? = null,
        durationRange: String? = null, // "short" (<15m), "medium" (15m-45m), "long" (>45m)
        dateAddedFilter: String? = null, // "today", "week", "month"
        podcastId: String? = null,
        genre: String? = null,
        sort: String? = "relevance", // relevance, newest, oldest, most_downloaded, most_played
        page: Int = 1,
        limit: Int = 20
    ): SearchResponse {
        val startTime = System.currentTimeMillis()
        val queryLower = q.trim().lowercase(Locale.getDefault())

        // 1. Generate Cache Key
        val cacheKey = "q=$queryLower&prov=$providerId&spk=$speakerId&cat=$categoryId&dur=$durationRange&date=$dateAddedFilter&pod=$podcastId&genre=$genre&sort=$sort&page=$page&limit=$limit"
        val cached = cache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < cacheTtlMs) {
            logger.info("Serving search request from Query Cache (Key: $cacheKey)")
            return cached.response
        }

        // 2. Log Query Analytics
        if (q.isNotBlank()) {
            queryTracker.computeIfAbsent(q.trim()) { AtomicInteger(0) }.incrementAndGet()
        }

        // 3. Fetch base lists from Supabase
        val rawMessages = fetchAllEpisodes()
        val rawSpeakers = fetchAllSpeakers()
        val rawPodcasts = fetchAllPodcasts()
        val rawCategories = fetchAllCategories()

        // 4. Apply filter layer to Episodes/Messages
        var filteredMessages = rawMessages.filter { episode ->
            var keep = true
            
            // Text matching (if query is present)
            if (queryLower.isNotBlank()) {
                val titleMatch = isMatch(episode.title, queryLower)
                val descMatch = isMatch(episode.description, queryLower)
                keep = titleMatch || descMatch
            }

            // Filters
            if (keep && speakerId != null) {
                keep = episode.speakerId == speakerId
            }
            if (keep && podcastId != null) {
                keep = episode.podcastId == podcastId
            }
            if (keep && categoryId != null) {
                // Find parent podcast to verify category
                val podcast = rawPodcasts.find { it.id == episode.podcastId }
                keep = podcast?.categoryId == categoryId
            }
            if (keep && providerId != null) {
                val podcast = rawPodcasts.find { it.id == episode.podcastId }
                keep = podcast?.providerId == providerId
            }
            if (keep && genre != null) {
                keep = episode.genre?.equals(genre, ignoreCase = true) == true
            }

            // Duration filter
            if (keep && durationRange != null) {
                val totalSecs = parseDurationToSeconds(episode.durationString)
                keep = when (durationRange.lowercase(Locale.getDefault())) {
                    "short" -> totalSecs < 15 * 60
                    "medium" -> totalSecs in (15 * 60)..(45 * 60)
                    "long" -> totalSecs > 45 * 60
                    else -> true
                }
            }

            // Date Added filter
            if (keep && dateAddedFilter != null) {
                try {
                    val addedInstant = Instant.parse(episode.createdAt)
                    val now = Instant.now()
                    keep = when (dateAddedFilter.lowercase(Locale.getDefault())) {
                        "today" -> addedInstant.isAfter(now.minus(1, ChronoUnit.DAYS))
                        "week" -> addedInstant.isAfter(now.minus(7, ChronoUnit.DAYS))
                        "month" -> addedInstant.isAfter(now.minus(30, ChronoUnit.DAYS))
                        else -> true
                    }
                } catch (e: Exception) {
                    // Fallback if timestamp parse fails
                }
            }

            keep
        }

        // Filter speakers
        var filteredSpeakers = rawSpeakers.filter { speaker ->
            queryLower.isBlank() || isMatch(speaker.name, queryLower) || (speaker.bio != null && isMatch(speaker.bio, queryLower))
        }

        // Filter podcasts
        var filteredPodcasts = rawPodcasts.filter { podcast ->
            var keep = queryLower.isBlank() || isMatch(podcast.title, queryLower) || isMatch(podcast.host, queryLower) || (podcast.description != null && isMatch(podcast.description, queryLower))
            if (keep && categoryId != null) {
                keep = podcast.categoryId == categoryId
            }
            if (keep && providerId != null) {
                keep = podcast.providerId == providerId
            }
            keep
        }

        // Filter categories
        val filteredCategories = rawCategories.filter { category ->
            queryLower.isBlank() || isMatch(category.name, queryLower)
        }

        // 5. Apply sorting layers
        filteredMessages = when (sort?.lowercase(Locale.getDefault())) {
            "newest" -> filteredMessages.sortedByDescending { it.createdAt }
            "oldest" -> filteredMessages.sortedBy { it.createdAt }
            "most_played" -> filteredMessages.sortedByDescending { it.playCount }
            "relevance" -> {
                if (queryLower.isNotBlank()) {
                    filteredMessages.sortedByDescending { calculateRelevance(it.title, queryLower) }
                } else {
                    filteredMessages.sortedByDescending { it.createdAt }
                }
            }
            else -> filteredMessages.sortedByDescending { it.createdAt }
        }

        filteredSpeakers = when (sort?.lowercase(Locale.getDefault())) {
            "most_played" -> filteredSpeakers.sortedByDescending { it.audioCount }
            "relevance" -> {
                if (queryLower.isNotBlank()) {
                    filteredSpeakers.sortedByDescending { calculateRelevance(it.name, queryLower) }
                } else {
                    filteredSpeakers.sortedByDescending { it.followersCount }
                }
            }
            else -> filteredSpeakers.sortedByDescending { it.followersCount }
        }

        filteredPodcasts = when (sort?.lowercase(Locale.getDefault())) {
            "newest" -> filteredPodcasts.sortedByDescending { it.createdAt }
            "most_played" -> filteredPodcasts.sortedByDescending { it.episodesCount }
            "relevance" -> {
                if (queryLower.isNotBlank()) {
                    filteredPodcasts.sortedByDescending { calculateRelevance(it.title, queryLower) }
                } else {
                    filteredPodcasts.sortedByDescending { it.episodesCount }
                }
            }
            else -> filteredPodcasts.sortedByDescending { it.episodesCount }
        }

        // 6. Pagination offset and limits
        val offset = (page - 1) * limit
        val paginatedMessages = filteredMessages.drop(offset).take(limit)
        val paginatedSpeakers = filteredSpeakers.drop(offset).take(limit)
        val paginatedPodcasts = filteredPodcasts.drop(offset).take(limit)
        val paginatedCategories = filteredCategories.drop(offset).take(limit)

        val totalResults = filteredMessages.size + filteredSpeakers.size + filteredPodcasts.size + filteredCategories.size
        val duration = System.currentTimeMillis() - startTime

        // Log search duration performance
        searchDurations[q] = duration
        logger.info("Search query '$q' took ${duration}ms. Filters: provider=$providerId, speaker=$speakerId, category=$categoryId. Result counts: messages=${paginatedMessages.size}, speakers=${paginatedSpeakers.size}, podcasts=${paginatedPodcasts.size}")

        val response = SearchResponse(
            messages = paginatedMessages,
            speakers = paginatedSpeakers,
            podcasts = paginatedPodcasts,
            categories = paginatedCategories,
            totalResults = totalResults,
            searchTimeMs = duration
        )

        // Cache the query results
        cache[cacheKey] = CachedResult(response, System.currentTimeMillis())

        return response
    }

    /**
     * Get popular search queries.
     */
    fun getTrendingSearches(limit: Int = 10): List<TrendingSearch> {
        return queryTracker.entries
            .sortedByDescending { it.value.get() }
            .take(limit)
            .map { TrendingSearch(it.key, it.value.get()) }
    }

    /**
     * Clear search query analytics and local memory cache.
     */
    fun clearCache() {
        cache.clear()
        logger.info("Search engine query cache cleared.")
    }

    // Helper functions to fetch and parse base data from Supabase
    private suspend fun fetchAllEpisodes(): List<PodcastEpisode> {
        return try {
            val res = supabase.get("podcast_episodes")
            if (res.status.value in 200..299) {
                jsonHelper.decodeFromString<List<PodcastEpisode>>(res.bodyAsText())
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchAllSpeakers(): List<Speaker> {
        return try {
            val res = supabase.get("speakers")
            if (res.status.value in 200..299) {
                jsonHelper.decodeFromString<List<Speaker>>(res.bodyAsText())
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchAllPodcasts(): List<Podcast> {
        return try {
            val res = supabase.get("podcasts")
            if (res.status.value in 200..299) {
                jsonHelper.decodeFromString<List<Podcast>>(res.bodyAsText())
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchAllCategories(): List<Category> {
        return try {
            val res = supabase.get("categories")
            if (res.status.value in 200..299) {
                jsonHelper.decodeFromString<List<Category>>(res.bodyAsText())
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseDurationToSeconds(duration: String): Int {
        val parts = duration.split(":")
        return when (parts.size) {
            1 -> parts[0].toIntOrNull() ?: 0
            2 -> {
                val m = parts[0].toIntOrNull() ?: 0
                val s = parts[1].toIntOrNull() ?: 0
                (m * 60) + s
            }
            3 -> {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                val s = parts[2].toIntOrNull() ?: 0
                (h * 3600) + (m * 60) + s
            }
            else -> 0
        }
    }
}
