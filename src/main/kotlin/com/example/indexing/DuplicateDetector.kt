package com.example.indexing

import com.example.config.SupabaseClient
import io.ktor.client.statement.*
import org.slf4j.LoggerFactory

class DuplicateDetector(private val supabase: SupabaseClient) {
    private val logger = LoggerFactory.getLogger(DuplicateDetector::class.java)

    /**
     * Checks if a normalized content record already exists in the database.
     * Combines multiple criteria for robust duplicate matching:
     * 1. Match by exact Audio URL.
     * 2. Match by combination of Title + Duration.
     */
    suspend fun isDuplicate(record: RawContentRecord): Boolean {
        try {
            // Check by exact Audio URL first (most reliable)
            val audioResponse = supabase.get("podcast_episodes", mapOf("audio_url" to "eq.${record.audioUrl}"))
            if (audioResponse.status.value in 200..299 && audioResponse.bodyAsText().trim() != "[]") {
                logger.info("Duplicate found (by audio URL) for record: ${record.title}")
                return true
            }

            // Check by Title + Duration combination
            val titleResponse = supabase.get(
                "podcast_episodes",
                mapOf(
                    "title" to "eq.${record.title}",
                    "duration_string" to "eq.${record.durationString}"
                )
            )
            if (titleResponse.status.value in 200..299 && titleResponse.bodyAsText().trim() != "[]") {
                logger.info("Duplicate found (by Title + Duration) for record: ${record.title}")
                return true
            }

        } catch (e: Exception) {
            logger.error("Error checking duplicate status for record ${record.title}: ${e.message}", e)
        }

        return false
    }
}
