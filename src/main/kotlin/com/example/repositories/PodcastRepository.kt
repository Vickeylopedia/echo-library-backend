package com.example.repositories

import com.example.models.DiscoverPodcast
import com.example.models.DiscoverAudioItem

interface PodcastRepository {
    suspend fun getFeaturedPodcasts(): List<DiscoverPodcast>
    suspend fun getPodcastEpisodes(id: String): List<DiscoverAudioItem>
}

class MockPodcastRepository : PodcastRepository {
    private val mockPodcasts = listOf(
        DiscoverPodcast(
            id = "pod_1",
            title = "The Zen Mind",
            host = "Sarah Jenkins",
            episodesCount = 42,
            coverUrl = null,
            description = "A bi-weekly exploration of Zen Buddhism, mindfulness practice, and daily meditations for busy professionals."
        ),
        DiscoverPodcast(
            id = "pod_2",
            title = "Stoic Coffee Break",
            host = "Chris Fisher",
            episodesCount = 128,
            coverUrl = null,
            description = "Bite-sized reflections on ancient philosophy applied to modern chaos. Perfect for your morning routine."
        ),
        DiscoverPodcast(
            id = "pod_3",
            title = "The Resonance Podcast",
            host = "Dr. Helen Vance",
            episodesCount = 19,
            coverUrl = null,
            description = "Delving into historical science documents, frequency therapies, and the minds of great inventors."
        )
    )

    private val mockEpisodes = listOf(
        DiscoverAudioItem(
            id = "pod_1_ep_1",
            title = "Episode 1: Breath Awareness",
            speaker = "Sarah Jenkins",
            durationString = "15:00",
            coverUrl = null,
            genre = "Mindfulness Podcast",
            playCount = 1250,
            description = "Learn how to anchor your attention on the breath and quiet the active chatter of the default mode network."
        ),
        DiscoverAudioItem(
            id = "pod_2_ep_1",
            title = "Episode 1: Control Dichotomy",
            speaker = "Chris Fisher",
            durationString = "10:30",
            coverUrl = null,
            genre = "Stoic Podcast",
            playCount = 3800,
            description = "Bite-sized breakdown of Epictetus's control dichotomy. What is within your power vs what is outside your power."
        )
    )

    override suspend fun getFeaturedPodcasts(): List<DiscoverPodcast> {
        return mockPodcasts
    }

    override suspend fun getPodcastEpisodes(id: String): List<DiscoverAudioItem> {
        return mockEpisodes.filter { it.id.startsWith(id) }
    }
}
