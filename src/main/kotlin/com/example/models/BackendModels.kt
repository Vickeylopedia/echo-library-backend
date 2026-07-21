package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String?,
    val avatarUrl: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class Speaker(
    val id: String,
    val name: String,
    val bio: String?,
    val avatarUrl: String?,
    val followersCount: Int,
    val audioCount: Int,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class Category(
    val id: String,
    val name: String,
    val count: Int,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class Provider(
    val id: String,
    val name: String,
    val apiBaseUrl: String?,
    val isActive: Boolean,
    val metadataJson: String, // Stringified JSON or serialized structure
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class Podcast(
    val id: String,
    val title: String,
    val host: String,
    val episodesCount: Int,
    val coverUrl: String?,
    val description: String?,
    val categoryId: String?,
    val providerId: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class PodcastEpisode(
    val id: String,
    val podcastId: String?,
    val speakerId: String?,
    val title: String,
    val durationString: String,
    val coverUrl: String?,
    val genre: String?,
    val playCount: Int,
    val description: String,
    val audioUrl: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ChatMessage(
    val id: String,
    val userId: String,
    val content: String,
    val sender: String, // "user" or "assistant"
    val createdAt: String
)

@Serializable
data class Collection(
    val id: String,
    val userId: String,
    val name: String,
    val description: String?,
    val isPublic: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CollectionItem(
    val id: String,
    val collectionId: String,
    val episodeId: String,
    val createdAt: String
)

@Serializable
data class Favorite(
    val id: String,
    val userId: String,
    val episodeId: String,
    val createdAt: String
)

@Serializable
data class ListeningProgress(
    val id: String,
    val userId: String,
    val episodeId: String,
    val lastPositionSeconds: Int,
    val durationSeconds: Int,
    val completed: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CompletedHistory(
    val id: String,
    val userId: String,
    val episodeId: String,
    val completedAt: String
)

@Serializable
data class Note(
    val id: String,
    val userId: String,
    val episodeId: String,
    val noteText: String,
    val timestampSeconds: Int?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class Insight(
    val id: String,
    val episodeId: String,
    val summary: String,
    val keyTakeawaysJson: String, // Stringified JSON array
    val generatedAt: String
)

@Serializable
data class SearchHistory(
    val id: String,
    val userId: String,
    val query: String,
    val createdAt: String
)

@Serializable
data class DownloadMetadata(
    val id: String,
    val userId: String,
    val episodeId: String,
    val downloadStatus: String, // 'pending', 'downloading', 'completed', 'failed'
    val filePath: String?,
    val fileSize_bytes: Long?,
    val createdAt: String,
    val updatedAt: String
)
