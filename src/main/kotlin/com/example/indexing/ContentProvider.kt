package com.example.indexing

import kotlinx.serialization.Serializable

/**
 * Raw metadata structure fetched directly from external content providers.
 */
@Serializable
data class RawContentRecord(
    val externalId: String,
    val title: String,
    val speakerName: String,
    val durationString: String,
    val coverUrl: String?,
    val genre: String?,
    val description: String,
    val audioUrl: String,
    val providerName: String,
    val categoryName: String? = null,
    val podcastTitle: String? = null
)

/**
 * Generic content provider interface that must be implemented by any streaming,
 * RSS, API, or permissioned metadata source.
 */
interface ContentProvider {
    val name: String

    suspend fun fetchLatest(): List<RawContentRecord>
    suspend fun fetchByPage(page: Int, limit: Int): List<RawContentRecord>
    suspend fun fetchByCategory(category: String, page: Int, limit: Int): List<RawContentRecord>
    suspend fun fetchBySpeaker(speaker: String, page: Int, limit: Int): List<RawContentRecord>
    suspend fun healthCheck(): Boolean
}
