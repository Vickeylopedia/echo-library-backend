package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class DiscoverAudioItem(
    val id: String,
    val title: String,
    val speaker: String,
    val durationString: String,
    val coverUrl: String?,
    val genre: String,
    val playCount: Int,
    val description: String
)

@Serializable
data class DiscoverSpeaker(
    val id: String,
    val name: String,
    val bio: String,
    val avatarUrl: String?,
    val followersCount: Int,
    val audioCount: Int
)

@Serializable
data class DiscoverPodcast(
    val id: String,
    val title: String,
    val host: String,
    val episodesCount: Int,
    val coverUrl: String?,
    val description: String
)

@Serializable
data class DiscoverCategory(
    val id: String,
    val name: String,
    val count: Int
)
