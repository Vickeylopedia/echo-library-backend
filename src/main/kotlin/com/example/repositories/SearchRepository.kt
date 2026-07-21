package com.example.repositories

import com.example.models.DiscoverAudioItem
import com.example.services.SearchEngine

interface SearchRepository {
    suspend fun search(query: String): List<DiscoverAudioItem>
    suspend fun suggestions(query: String): List<String>
}

class SupabaseSearchRepository(private val searchEngine: SearchEngine) : SearchRepository {
    override suspend fun search(query: String): List<DiscoverAudioItem> {
        val response = searchEngine.search(q = query, limit = 50)
        return response.messages.map { episode ->
            val podcast = response.podcasts.find { it.id == episode.podcastId }
            val speaker = response.speakers.find { it.id == episode.speakerId }
            val hostOrSpeaker = speaker?.name ?: podcast?.host ?: "Unknown Speaker"
            DiscoverAudioItem(
                id = episode.id,
                title = episode.title,
                speaker = hostOrSpeaker,
                durationString = episode.durationString,
                coverUrl = episode.coverUrl,
                genre = episode.genre ?: "Sermon",
                playCount = episode.playCount,
                description = episode.description
            )
        }
    }

    override suspend fun suggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val response = searchEngine.search(q = query, limit = 10)
        val suggestions = mutableListOf<String>()
        suggestions.addAll(response.speakers.map { it.name })
        suggestions.addAll(response.podcasts.map { it.title })
        suggestions.addAll(response.categories.map { it.name })
        return suggestions.distinct().take(10)
    }
}

class MockSearchRepository : SearchRepository {
    private val mockTracks = listOf(
        DiscoverAudioItem(
            id = "trend_1",
            title = "The Wisdom of Insecurity",
            speaker = "Alan Watts",
            durationString = "12:45",
            coverUrl = null,
            genre = "Philosophy",
            playCount = 12543,
            description = "Exploring how our constant anxiety for security prevents us from experiencing the present moment."
        ),
        DiscoverAudioItem(
            id = "trend_2",
            title = "The Inner Citadel and Marcus Aurelius",
            speaker = "Pierre Hadot",
            durationString = "24:10",
            coverUrl = null,
            genre = "Stoicism",
            playCount = 9821,
            description = "A deep reading of Meditations and the spiritual exercises that kept the Roman Emperor resilient."
        ),
        DiscoverAudioItem(
            id = "trend_3",
            title = "Vibrations and Harmonic Resonance",
            speaker = "Nikola Tesla",
            durationString = "08:15",
            coverUrl = null,
            genre = "Physics",
            playCount = 8299,
            description = "Understanding how frequencies dictate the physical structure of matter and the grid of electrical resonance."
        ),
        DiscoverAudioItem(
            id = "late_1",
            title = "Introduction to the Enchiridion",
            speaker = "Epictetus",
            durationString = "15:30",
            coverUrl = null,
            genre = "Stoicism",
            playCount = 1420,
            description = "The handbook of daily Stoic practices, beginning with the fundamental distinction of what is in our control."
        ),
        DiscoverAudioItem(
            id = "late_2",
            title = "The Eternal Now",
            speaker = "Alan Watts",
            durationString = "18:22",
            coverUrl = null,
            genre = "Philosophy",
            playCount = 3201,
            description = "Explaining how the past is only a memory, the future a projection, and the present is all that exists."
        ),
        DiscoverAudioItem(
            id = "late_3",
            title = "Experiments with Alternate Currents",
            speaker = "Nikola Tesla",
            durationString = "11:45",
            coverUrl = null,
            genre = "Physics",
            playCount = 890,
            description = "A reading of Tesla's historic address before the Institution of Electrical Engineers in London."
        )
    )

    override suspend fun search(query: String): List<DiscoverAudioItem> {
        if (query.isBlank()) return mockTracks
        return mockTracks.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.speaker.contains(query, ignoreCase = true) ||
            it.genre.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }

    override suspend fun suggestions(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        return listOf("Alan Watts", "Stoicism", "Marcus Aurelius", "Zen Meditation", "Physics Resonance")
            .filter { it.contains(query, ignoreCase = true) }
    }
}
