package com.example.repositories

import com.example.config.SupabaseClient
import com.example.models.*
import com.example.models.Collection
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.UUID

// Global Serialization Helper
private val jsonHelper = Json { ignoreUnknownKeys = true; coerceInputValues = true }

// Helper extension to check success of a response
private fun HttpResponse.isSuccess(): Boolean = this.status.value in 200..299

// Helper extension to safely decode lists with logging and fallback
private suspend inline fun <reified T> HttpResponse.decodeListOrEmpty(logger: org.slf4j.Logger): List<T> {
    return try {
        if (this.isSuccess()) {
            val text = this.bodyAsText()
            jsonHelper.decodeFromString<List<T>>(text)
        } else {
            logger.warn("Supabase returned non-success code: ${this.status}. Content: ${this.bodyAsText()}")
            emptyList()
        }
    } catch (e: Exception) {
        logger.error("Failed to decode Supabase response to list: ${e.message}", e)
        emptyList()
    }
}

// Helper extension to safely decode single objects
private suspend inline fun <reified T> HttpResponse.decodeObjectOrNull(logger: org.slf4j.Logger): T? {
    return try {
        if (this.isSuccess()) {
            val text = this.bodyAsText()
            // PostgREST returns a JSON array when querying with representation
            if (text.trim().startsWith("[")) {
                val list = jsonHelper.decodeFromString<List<T>>(text)
                list.firstOrNull()
            } else {
                jsonHelper.decodeFromString<T>(text)
            }
        } else {
            logger.warn("Supabase returned non-success code: ${this.status}. Content: ${this.bodyAsText()}")
            null
        }
    } catch (e: Exception) {
        logger.error("Failed to decode Supabase response to object: ${e.message}", e)
        null
    }
}

// ---------------------------------------------------------
// -- 1. USER PROFILE REPO
// ---------------------------------------------------------
interface UserRepository {
    suspend fun getUserProfile(id: String): UserProfile?
    suspend fun createUserProfile(profile: UserProfile): UserProfile?
    suspend fun updateUserProfile(profile: UserProfile): UserProfile?
}

class SupabaseUserRepository(private val supabase: SupabaseClient) : UserRepository {
    private val logger = LoggerFactory.getLogger(SupabaseUserRepository::class.java)

    override suspend fun getUserProfile(id: String): UserProfile? {
        return try {
            val response = supabase.get("users", mapOf("id" to "eq.$id"))
            response.decodeObjectOrNull<UserProfile>(logger)
        } catch (e: Exception) {
            logger.error("Error fetching user profile", e)
            null
        }
    }

    override suspend fun createUserProfile(profile: UserProfile): UserProfile? {
        return try {
            val response = supabase.post("users", profile)
            response.decodeObjectOrNull<UserProfile>(logger)
        } catch (e: Exception) {
            logger.error("Error creating user profile", e)
            null
        }
    }

    override suspend fun updateUserProfile(profile: UserProfile): UserProfile? {
        return try {
            val response = supabase.patch("users", profile, mapOf("id" to "eq.${profile.id}"))
            response.decodeObjectOrNull<UserProfile>(logger)
        } catch (e: Exception) {
            logger.error("Error updating user profile", e)
            null
        }
    }
}

// ---------------------------------------------------------
// -- 2. SPEAKER REPO
// ---------------------------------------------------------
class SupabaseSpeakerRepository(private val supabase: SupabaseClient) : SpeakerRepository {
    private val logger = LoggerFactory.getLogger(SupabaseSpeakerRepository::class.java)
    private val mockRepo = MockSpeakerRepository()

    override suspend fun getSpeakers(page: Int, limit: Int): List<DiscoverSpeaker> {
        return try {
            val offset = (page - 1) * limit
            val response = supabase.get("speakers", mapOf(
                "select" to "*",
                "limit" to limit.toString(),
                "offset" to offset.toString()
            ))
            val list = response.decodeListOrEmpty<Speaker>(logger)
            if (list.isEmpty()) {
                mockRepo.getSpeakers(page, limit)
            } else {
                list.map { DiscoverSpeaker(it.id, it.name, it.bio ?: "", it.avatarUrl, it.followersCount, it.audioCount) }
            }
        } catch (e: Exception) {
            logger.error("Error in getSpeakers, falling back to mock", e)
            mockRepo.getSpeakers(page, limit)
        }
    }

    override suspend fun getSpeakerDetails(id: String): DiscoverSpeaker {
        return try {
            val response = supabase.get("speakers", mapOf("id" to "eq.$id"))
            val item = response.decodeObjectOrNull<Speaker>(logger)
            if (item != null) {
                DiscoverSpeaker(item.id, item.name, item.bio ?: "", item.avatarUrl, item.followersCount, item.audioCount)
            } else {
                mockRepo.getSpeakerDetails(id)
            }
        } catch (e: Exception) {
            logger.error("Error in getSpeakerDetails, falling back to mock", e)
            mockRepo.getSpeakerDetails(id)
        }
    }

    override suspend fun getSpeakerTracks(id: String): List<DiscoverAudioItem> {
        return try {
            val response = supabase.get("podcast_episodes", mapOf("speaker_id" to "eq.$id"))
            val list = response.decodeListOrEmpty<PodcastEpisode>(logger)
            if (list.isEmpty()) {
                mockRepo.getSpeakerTracks(id)
            } else {
                val speakerDetails = getSpeakerDetails(id)
                list.map {
                    DiscoverAudioItem(it.id, it.title, speakerDetails.name, it.durationString, it.coverUrl, it.genre ?: "Uncategorized", it.playCount, it.description)
                }
            }
        } catch (e: Exception) {
            logger.error("Error in getSpeakerTracks, falling back to mock", e)
            mockRepo.getSpeakerTracks(id)
        }
    }
}

// ---------------------------------------------------------
// -- 3. MESSAGES REPO
// ---------------------------------------------------------
interface MessageRepository {
    suspend fun getMessagesForUser(userId: String): List<ChatMessage>
    suspend fun createMessage(message: ChatMessage): ChatMessage?
}

class SupabaseMessageRepository(private val supabase: SupabaseClient) : MessageRepository {
    private val logger = LoggerFactory.getLogger(SupabaseMessageRepository::class.java)

    override suspend fun getMessagesForUser(userId: String): List<ChatMessage> {
        return try {
            val response = supabase.get("messages", mapOf(
                "user_id" to "eq.$userId",
                "order" to "created_at.asc"
            ))
            response.decodeListOrEmpty(logger)
        } catch (e: Exception) {
            logger.error("Error fetching messages", e)
            emptyList()
        }
    }

    override suspend fun createMessage(message: ChatMessage): ChatMessage? {
        return try {
            val response = supabase.post("messages", message)
            response.decodeObjectOrNull<ChatMessage>(logger)
        } catch (e: Exception) {
            logger.error("Error creating message", e)
            null
        }
    }
}

// ---------------------------------------------------------
// -- 4. PODCAST REPO
// ---------------------------------------------------------
class SupabasePodcastRepository(private val supabase: SupabaseClient) : PodcastRepository {
    private val logger = LoggerFactory.getLogger(SupabasePodcastRepository::class.java)
    private val mockRepo = MockPodcastRepository()

    override suspend fun getFeaturedPodcasts(): List<DiscoverPodcast> {
        return try {
            val response = supabase.get("podcasts", mapOf("select" to "*"))
            val list = response.decodeListOrEmpty<Podcast>(logger)
            if (list.isEmpty()) {
                mockRepo.getFeaturedPodcasts()
            } else {
                list.map { DiscoverPodcast(it.id, it.title, it.host, it.episodesCount, it.coverUrl, it.description ?: "") }
            }
        } catch (e: Exception) {
            logger.error("Error in getFeaturedPodcasts, falling back to mock", e)
            mockRepo.getFeaturedPodcasts()
        }
    }

    override suspend fun getPodcastEpisodes(id: String): List<DiscoverAudioItem> {
        return try {
            val response = supabase.get("podcast_episodes", mapOf("podcast_id" to "eq.$id"))
            val list = response.decodeListOrEmpty<PodcastEpisode>(logger)
            if (list.isEmpty()) {
                mockRepo.getPodcastEpisodes(id)
            } else {
                val podcastResponse = supabase.get("podcasts", mapOf("id" to "eq.$id"))
                val podcast = podcastResponse.decodeObjectOrNull<Podcast>(logger)
                val hostName = podcast?.host ?: "Unknown Host"
                list.map {
                    DiscoverAudioItem(it.id, it.title, hostName, it.durationString, it.coverUrl, it.genre ?: "Podcast", it.playCount, it.description)
                }
            }
        } catch (e: Exception) {
            logger.error("Error in getPodcastEpisodes, falling back to mock", e)
            mockRepo.getPodcastEpisodes(id)
        }
    }
}

// ---------------------------------------------------------
// -- 5. CATEGORIES REPO
// ---------------------------------------------------------
interface CategoryRepository {
    suspend fun getCategories(): List<Category>
    suspend fun createCategory(category: Category): Category?
}

class SupabaseCategoryRepository(private val supabase: SupabaseClient) : CategoryRepository {
    private val logger = LoggerFactory.getLogger(SupabaseCategoryRepository::class.java)

    override suspend fun getCategories(): List<Category> {
        return try {
            val response = supabase.get("categories", mapOf("select" to "*"))
            response.decodeListOrEmpty(logger)
        } catch (e: Exception) {
            logger.error("Error fetching categories", e)
            emptyList()
        }
    }

    override suspend fun createCategory(category: Category): Category? {
        return try {
            val response = supabase.post("categories", category)
            response.decodeObjectOrNull(logger)
        } catch (e: Exception) {
            logger.error("Error creating category", e)
            null
        }
    }
}

// ---------------------------------------------------------
// -- 6. COLLECTIONS REPO
// ---------------------------------------------------------
interface CollectionRepository {
    suspend fun getCollectionsForUser(userId: String): List<Collection>
    suspend fun getCollectionItems(collectionId: String): List<CollectionItem>
    suspend fun createCollection(collection: Collection): Collection?
    suspend fun addEpisodeToCollection(item: CollectionItem): CollectionItem?
    suspend fun removeEpisodeFromCollection(collectionId: String, episodeId: String): Boolean
}

class SupabaseCollectionRepository(private val supabase: SupabaseClient) : CollectionRepository {
    private val logger = LoggerFactory.getLogger(SupabaseCollectionRepository::class.java)

    override suspend fun getCollectionsForUser(userId: String): List<Collection> {
        return try {
            val response = supabase.get("collections", mapOf("user_id" to "eq.$userId"))
            response.decodeListOrEmpty(logger)
        } catch (e: Exception) {
            logger.error("Error fetching user collections", e)
            emptyList()
        }
    }

    override suspend fun getCollectionItems(collectionId: String): List<CollectionItem> {
        return try {
            val response = supabase.get("collection_items", mapOf("collection_id" to "eq.$collectionId"))
            response.decodeListOrEmpty(logger)
        } catch (e: Exception) {
            logger.error("Error fetching collection items", e)
            emptyList()
        }
    }

    override suspend fun createCollection(collection: Collection): Collection? {
        return try {
            val response = supabase.post("collections", collection)
            response.decodeObjectOrNull(logger)
        } catch (e: Exception) {
            logger.error("Error creating collection", e)
            null
        }
    }

    override suspend fun addEpisodeToCollection(item: CollectionItem): CollectionItem? {
        return try {
            val response = supabase.post("collection_items", item)
            response.decodeObjectOrNull(logger)
        } catch (e: Exception) {
            logger.error("Error adding episode to collection", e)
            null
        }
    }

    override suspend fun removeEpisodeFromCollection(collectionId: String, episodeId: String): Boolean {
        return try {
            val response = supabase.delete("collection_items", mapOf(
                "collection_id" to "eq.$collectionId",
                "episode_id" to "eq.$episodeId"
            ))
            response.isSuccess()
        } catch (e: Exception) {
            logger.error("Error removing episode from collection", e)
            false
        }
    }
}

// ---------------------------------------------------------
// -- 7. FAVORITES REPO
// ---------------------------------------------------------
interface FavoriteRepository {
    suspend fun getFavoritesForUser(userId: String): List<Favorite>
    suspend fun addFavorite(favorite: Favorite): Favorite?
    suspend fun removeFavorite(userId: String, episodeId: String): Boolean
}

class SupabaseFavoriteRepository(private val supabase: SupabaseClient) : FavoriteRepository {
    private val logger = LoggerFactory.getLogger(SupabaseFavoriteRepository::class.java)

    override suspend fun getFavoritesForUser(userId: String): List<Favorite> {
        return try {
            val response = supabase.get("favorites", mapOf("user_id" to "eq.$userId"))
            response.decodeListOrEmpty(logger)
        } catch (e: Exception) {
            logger.error("Error fetching user favorites", e)
            emptyList()
        }
    }

    override suspend fun addFavorite(favorite: Favorite): Favorite? {
        return try {
            val response = supabase.post("favorites", favorite)
            response.decodeObjectOrNull(logger)
        } catch (e: Exception) {
            logger.error("Error adding favorite", e)
            null
        }
    }

    override suspend fun removeFavorite(userId: String, episodeId: String): Boolean {
        return try {
            val response = supabase.delete("favorites", mapOf(
                "user_id" to "eq.$userId",
                "episode_id" to "eq.$episodeId"
            ))
            response.isSuccess()
        } catch (e: Exception) {
            logger.error("Error removing favorite", e)
            false
        }
    }
}

// ---------------------------------------------------------
// -- 8. LISTENING PROGRESS & COMPLETED HISTORY REPO
// ---------------------------------------------------------
interface ListeningProgressRepository {
    suspend fun getProgress(userId: String, episodeId: String): ListeningProgress?
    suspend fun updateProgress(progress: ListeningProgress): ListeningProgress?
    suspend fun getCompletedHistory(userId: String): List<CompletedHistory>
    suspend fun addCompletedHistory(history: CompletedHistory): CompletedHistory?
}

class SupabaseListeningProgressRepository(private val supabase: SupabaseClient) : ListeningProgressRepository {
    private val logger = LoggerFactory.getLogger(SupabaseListeningProgressRepository::class.java)

    override suspend fun getProgress(userId: String, episodeId: String): ListeningProgress? {
        return try {
            val response = supabase.get("listening_progress", mapOf(
                "user_id" to "eq.$userId",
                "episode_id" to "eq.$episodeId"
            ))
            response.decodeObjectOrNull(logger)
        } catch (e: Exception) {
            logger.error("Error getting listening progress", e)
            null
        }
    }

    override suspend fun updateProgress(progress: ListeningProgress): ListeningProgress? {
        return try {
            val response = supabase.post("listening_progress", progress, preferReturn = "representation")
            response.decodeObjectOrNull(logger)
        } catch (e: Exception) {
            logger.error("Error updating listening progress", e)
            null
        }
    }

    override suspend fun getCompletedHistory(userId: String): List<CompletedHistory> {
        return try {
            val response = supabase.get("completed_history", mapOf("user_id" to "eq.$userId"))
            response.decodeListOrEmpty(logger)
        } catch (e: Exception) {
            logger.error("Error fetching completed history", e)
            emptyList()
        }
    }

    override suspend fun addCompletedHistory(history: CompletedHistory): CompletedHistory? {
        return try {
            val response = supabase.post("completed_history", history)
            response.decodeObjectOrNull(logger)
        } catch (e: Exception) {
            logger.error("Error adding completed history", e)
            null
        }
    }
}

// ---------------------------------------------------------
// -- 9. NOTES REPO
// ---------------------------------------------------------
interface NoteRepository {
    suspend fun getNotesForUserAndEpisode(userId: String, episodeId: String): List<Note>
    suspend fun createNote(note: Note): Note?
    suspend fun deleteNote(id: String): Boolean
}

class SupabaseNoteRepository(private val supabase: SupabaseClient) : NoteRepository {
    private val logger = LoggerFactory.getLogger(SupabaseNoteRepository::class.java)

    override suspend fun getNotesForUserAndEpisode(userId: String, episodeId: String): List<Note> {
        return try {
            val response = supabase.get("notes", mapOf(
                "user_id" to "eq.$userId",
                "episode_id" to "eq.$episodeId"
            ))
            response.decodeListOrEmpty(logger)
        } catch (e: Exception) {
            logger.error("Error getting user notes", e)
            emptyList()
        }
    }

    override suspend fun createNote(note: Note): Note? {
        return try {
            val response = supabase.post("notes", note)
            response.decodeObjectOrNull(logger)
        } catch (e: Exception) {
            logger.error("Error creating note", e)
            null
        }
    }

    override suspend fun deleteNote(id: String): Boolean {
        return try {
            val response = supabase.delete("notes", mapOf("id" to "eq.$id"))
            response.isSuccess()
        } catch (e: Exception) {
            logger.error("Error deleting note", e)
            false
        }
    }
}

// ---------------------------------------------------------
// -- 10. INSIGHTS REPO
// ---------------------------------------------------------
interface InsightRepository {
    suspend fun getInsightForEpisode(episodeId: String): Insight?
    suspend fun createInsight(insight: Insight): Insight?
}

class SupabaseInsightRepository(private val supabase: SupabaseClient) : InsightRepository {
    private val logger = LoggerFactory.getLogger(SupabaseInsightRepository::class.java)

    override suspend fun getInsightForEpisode(episodeId: String): Insight? {
        return try {
            val response = supabase.get("insights", mapOf("episode_id" to "eq.$episodeId"))
            response.decodeObjectOrNull(logger)
        } catch (e: Exception) {
            logger.error("Error getting insights", e)
            null
        }
    }

    override suspend fun createInsight(insight: Insight): Insight? {
        return try {
            val response = supabase.post("insights", insight)
            response.decodeObjectOrNull(logger)
        } catch (e: Exception) {
            logger.error("Error creating insights", e)
            null
        }
    }
}

// ---------------------------------------------------------
// -- 11. SEARCH HISTORY REPO
// ---------------------------------------------------------
interface SearchHistoryRepository {
    suspend fun getSearchHistory(userId: String): List<SearchHistory>
    suspend fun addSearchQuery(history: SearchHistory): SearchHistory?
    suspend fun clearSearchHistory(userId: String): Boolean
}

class SupabaseSearchHistoryRepository(private val supabase: SupabaseClient) : SearchHistoryRepository {
    private val logger = LoggerFactory.getLogger(SupabaseSearchHistoryRepository::class.java)

    override suspend fun getSearchHistory(userId: String): List<SearchHistory> {
        return try {
            val response = supabase.get("search_history", mapOf(
                "user_id" to "eq.$userId",
                "order" to "created_at.desc"
            ))
            response.decodeListOrEmpty(logger)
        } catch (e: Exception) {
            logger.error("Error getting search history", e)
            emptyList()
        }
    }

    override suspend fun addSearchQuery(history: SearchHistory): SearchHistory? {
        return try {
            val response = supabase.post("search_history", history)
            response.decodeObjectOrNull(logger)
        } catch (e: Exception) {
            logger.error("Error adding search query", e)
            null
        }
    }

    override suspend fun clearSearchHistory(userId: String): Boolean {
        return try {
            val response = supabase.delete("search_history", mapOf("user_id" to "eq.$userId"))
            response.isSuccess()
        } catch (e: Exception) {
            logger.error("Error clearing search history", e)
            false
        }
    }
}

// ---------------------------------------------------------
// -- 12. DOWNLOAD METADATA REPO
// ---------------------------------------------------------
interface DownloadRepository {
    suspend fun getDownloadsForUser(userId: String): List<DownloadMetadata>
    suspend fun updateDownloadMetadata(meta: DownloadMetadata): DownloadMetadata?
}

class SupabaseDownloadRepository(private val supabase: SupabaseClient) : DownloadRepository {
    private val logger = LoggerFactory.getLogger(SupabaseDownloadRepository::class.java)

    override suspend fun getDownloadsForUser(userId: String): List<DownloadMetadata> {
        return try {
            val response = supabase.get("downloads", mapOf("user_id" to "eq.$userId"))
            response.decodeListOrEmpty(logger)
        } catch (e: Exception) {
            logger.error("Error getting downloads", e)
            emptyList()
        }
    }

    override suspend fun updateDownloadMetadata(meta: DownloadMetadata): DownloadMetadata? {
        return try {
            val response = supabase.post("downloads", meta, preferReturn = "representation")
            response.decodeObjectOrNull(logger)
        } catch (e: Exception) {
            logger.error("Error updating download metadata", e)
            null
        }
    }
}

// ---------------------------------------------------------
// -- 13. PROVIDERS REPO
// ---------------------------------------------------------
interface ProviderRepository {
    suspend fun getProviders(): List<Provider>
    suspend fun createProvider(provider: Provider): Provider?
}

class SupabaseProviderRepository(private val supabase: SupabaseClient) : ProviderRepository {
    private val logger = LoggerFactory.getLogger(SupabaseProviderRepository::class.java)

    override suspend fun getProviders(): List<Provider> {
        return try {
            val response = supabase.get("providers", mapOf("select" to "*"))
            response.decodeListOrEmpty(logger)
        } catch (e: Exception) {
            logger.error("Error getting providers", e)
            emptyList()
        }
    }

    override suspend fun createProvider(provider: Provider): Provider? {
        return try {
            val response = supabase.post("providers", provider)
            response.decodeObjectOrNull(logger)
        } catch (e: Exception) {
            logger.error("Error creating provider", e)
            null
        }
    }
}
