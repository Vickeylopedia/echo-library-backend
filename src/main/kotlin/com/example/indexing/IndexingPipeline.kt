package com.example.indexing

import com.example.config.SupabaseClient
import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.UUID

@kotlinx.serialization.Serializable
data class IndexingStats(
    val providerName: String,
    val fetched: Int,
    val validated: Int,
    val duplicatesSkipped: Int,
    val inserted: Int,
    val status: String,
    val error: String? = null
)

class IndexingPipeline(private val supabase: SupabaseClient) {
    private val logger = LoggerFactory.getLogger(IndexingPipeline::class.java)
    private val duplicateDetector = DuplicateDetector(supabase)
    
    val activeIndexersCount = java.util.concurrent.atomic.AtomicInteger(0)

    // Map of active providers
    private val providers = listOf(
        NaijaSermonProvider(),
        SpiritNerdsProvider(),
        ListenNotesProvider()
    )

    /**
     * Executes indexing across all registered providers.
     */
    suspend fun executeAll(): List<IndexingStats> {
        logger.info("Starting global content indexing pipeline execution...")
        val results = mutableListOf<IndexingStats>()
        for (provider in providers) {
            results.add(executeForProvider(provider))
        }
        logger.info("Global content indexing pipeline completed: $results")
        return results
    }

    /**
     * Executes indexing for a single provider specified by name.
     */
    suspend fun executeForProviderByName(providerName: String): IndexingStats? {
        val provider = providers.find { it.name.equals(providerName, ignoreCase = true) }
        if (provider == null) {
            logger.error("Provider '$providerName' not found in registered indexing providers.")
            return null
        }
        return executeForProvider(provider)
    }

    private suspend fun executeForProvider(provider: ContentProvider): IndexingStats {
        activeIndexersCount.incrementAndGet()
        try {
            logger.info("Starting indexing execution for provider: ${provider.name}")
        
        // 1. Health check
        val isHealthy = try {
            provider.healthCheck()
        } catch (e: Exception) {
            logger.error("Health check failed for provider ${provider.name}: ${e.message}")
            false
        }

        if (!isHealthy) {
            return IndexingStats(
                providerName = provider.name,
                fetched = 0,
                validated = 0,
                duplicatesSkipped = 0,
                inserted = 0,
                status = "FAILED",
                error = "Provider health check failed"
            )
        }

        // 2. Fetch raw records
        val rawRecords = try {
            provider.fetchLatest()
        } catch (e: Exception) {
            logger.error("Failed to fetch records from provider ${provider.name}: ${e.message}", e)
            return IndexingStats(
                providerName = provider.name,
                fetched = 0,
                validated = 0,
                duplicatesSkipped = 0,
                inserted = 0,
                status = "FAILED",
                error = "Fetch error: ${e.message}"
            )
        }

        var validatedCount = 0
        var duplicatesSkippedCount = 0
        var insertedCount = 0

        // 3. Process each record
        for (rawRecord in rawRecords) {
            try {
                // Validation layer
                val validation = RecordValidator.validate(rawRecord)
                if (!validation.isValid) {
                    continue
                }
                validatedCount++

                // Normalization layer
                val normalized = ContentNormalizer.normalize(rawRecord)

                // Duplicate Detection layer
                if (duplicateDetector.isDuplicate(normalized)) {
                    duplicatesSkippedCount++
                    continue
                }

                // Database Integration
                val success = persistRecord(normalized)
                if (success) {
                    insertedCount++
                }
            } catch (e: Exception) {
                logger.error("Failed processing individual record: ${rawRecord.title} from ${provider.name}. Error: ${e.message}", e)
            }
        }

        val stats = IndexingStats(
            providerName = provider.name,
            fetched = rawRecords.size,
            validated = validatedCount,
            duplicatesSkipped = duplicatesSkippedCount,
            inserted = insertedCount,
            status = "SUCCESS"
        )
        return stats
        } finally {
            activeIndexersCount.decrementAndGet()
        }
    }

    /**
     * Inserts master data first (Provider, Category, Speaker, Podcast)
     * and finally the Episode.
     */
    private suspend fun persistRecord(record: RawContentRecord): Boolean {
        try {
            // 1. Resolve Provider
            val providerId = resolveProvider(record.providerName) ?: return false

            // 2. Resolve Category
            val categoryId = resolveCategory(record.categoryName ?: "Uncategorized") ?: return false

            // 3. Resolve Speaker
            val speakerId = resolveSpeaker(record.speakerName, record.coverUrl) ?: return false

            // 4. Resolve Podcast
            val podcastTitle = record.podcastTitle ?: "${record.speakerName} Podcast"
            val podcastId = resolvePodcast(
                title = podcastTitle,
                host = record.speakerName,
                coverUrl = record.coverUrl,
                categoryId = categoryId,
                providerId = providerId
            ) ?: return false

            // 5. Create and Insert Podcast Episode
            val episodeBody = buildJsonObject {
                put("podcast_id", podcastId)
                put("speaker_id", speakerId)
                put("title", record.title)
                put("duration_string", record.durationString)
                put("cover_url", record.coverUrl ?: "")
                put("genre", record.genre ?: "Sermon")
                put("play_count", 0)
                put("description", record.description)
                put("audio_url", record.audioUrl)
            }

            val response = supabase.post("podcast_episodes", episodeBody)
            if (response.status.value in 200..299) {
                logger.info("Successfully persisted episode: ${record.title}")
                return true
            } else {
                logger.error("Failed to insert episode: ${record.title}. Response: ${response.bodyAsText()}")
            }
        } catch (e: Exception) {
            logger.error("Exception during record persistence: ${record.title}. Error: ${e.message}", e)
        }
        return false
    }

    private suspend fun resolveProvider(name: String): String? {
        val getRes = supabase.get("providers", mapOf("name" to "eq.$name"))
        if (getRes.status.value in 200..299) {
            val arr = Json.parseToJsonElement(getRes.bodyAsText()).jsonArray
            if (arr.isNotEmpty()) {
                return arr[0].jsonObject["id"]?.jsonPrimitive?.content
            }
        }

        // Create provider
        val provBody = buildJsonObject {
            put("name", name)
            put("api_base_url", "")
            put("is_active", true)
            put("metadata", buildJsonObject {})
        }
        val postRes = supabase.post("providers", provBody)
        if (postRes.status.value in 200..299) {
            val arr = Json.parseToJsonElement(postRes.bodyAsText()).jsonArray
            return arr.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
        }
        return null
    }

    private suspend fun resolveCategory(name: String): String? {
        val getRes = supabase.get("categories", mapOf("name" to "eq.$name"))
        if (getRes.status.value in 200..299) {
            val arr = Json.parseToJsonElement(getRes.bodyAsText()).jsonArray
            if (arr.isNotEmpty()) {
                return arr[0].jsonObject["id"]?.jsonPrimitive?.content
            }
        }

        // Create category
        val catBody = buildJsonObject {
            put("name", name)
            put("count", 1)
        }
        val postRes = supabase.post("categories", catBody)
        if (postRes.status.value in 200..299) {
            val arr = Json.parseToJsonElement(postRes.bodyAsText()).jsonArray
            return arr.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
        }
        return null
    }

    private suspend fun resolveSpeaker(name: String, avatarUrl: String?): String? {
        val getRes = supabase.get("speakers", mapOf("name" to "eq.$name"))
        if (getRes.status.value in 200..299) {
            val arr = Json.parseToJsonElement(getRes.bodyAsText()).jsonArray
            if (arr.isNotEmpty()) {
                return arr[0].jsonObject["id"]?.jsonPrimitive?.content
            }
        }

        // Create speaker
        val speakerBody = buildJsonObject {
            put("name", name)
            put("bio", "Biography for $name.")
            put("avatar_url", avatarUrl ?: "")
            put("followers_count", 0)
            put("audio_count", 1)
        }
        val postRes = supabase.post("speakers", speakerBody)
        if (postRes.status.value in 200..299) {
            val arr = Json.parseToJsonElement(postRes.bodyAsText()).jsonArray
            return arr.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
        }
        return null
    }

    private suspend fun resolvePodcast(
        title: String,
        host: String,
        coverUrl: String?,
        categoryId: String,
        providerId: String
    ): String? {
        val getRes = supabase.get("podcasts", mapOf("title" to "eq.$title"))
        if (getRes.status.value in 200..299) {
            val arr = Json.parseToJsonElement(getRes.bodyAsText()).jsonArray
            if (arr.isNotEmpty()) {
                return arr[0].jsonObject["id"]?.jsonPrimitive?.content
            }
        }

        // Create podcast
        val podBody = buildJsonObject {
            put("title", title)
            put("host", host)
            put("episodes_count", 1)
            put("cover_url", coverUrl ?: "")
            put("description", "Podcast hosted by $host containing sermons and talks.")
            put("category_id", categoryId)
            put("provider_id", providerId)
        }
        val postRes = supabase.post("podcasts", podBody)
        if (postRes.status.value in 200..299) {
            val arr = Json.parseToJsonElement(postRes.bodyAsText()).jsonArray
            return arr.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
        }
        return null
    }
}
