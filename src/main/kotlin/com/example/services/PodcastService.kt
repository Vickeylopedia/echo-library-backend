package com.example.services

import com.example.models.DiscoverPodcast
import com.example.models.DiscoverAudioItem
import com.example.repositories.PodcastRepository

interface PodcastService {
    suspend fun getFeaturedPodcasts(): List<DiscoverPodcast>
    suspend fun getPodcastEpisodes(id: String): List<DiscoverAudioItem>
}

class PodcastServiceImpl(private val podcastRepository: PodcastRepository) : PodcastService {
    override suspend fun getFeaturedPodcasts(): List<DiscoverPodcast> {
        return podcastRepository.getFeaturedPodcasts()
    }

    override suspend fun getPodcastEpisodes(id: String): List<DiscoverAudioItem> {
        require(id.isNotBlank()) { "Podcast ID cannot be blank" }
        return podcastRepository.getPodcastEpisodes(id)
    }
}
