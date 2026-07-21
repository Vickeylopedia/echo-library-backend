package com.example.repositories

import com.example.models.DiscoverSpeaker
import com.example.models.DiscoverAudioItem

interface SpeakerRepository {
    suspend fun getSpeakers(page: Int, limit: Int): List<DiscoverSpeaker>
    suspend fun getSpeakerDetails(id: String): DiscoverSpeaker
    suspend fun getSpeakerTracks(id: String): List<DiscoverAudioItem>
}

class MockSpeakerRepository : SpeakerRepository {
    private val mockSpeakers = listOf(
        DiscoverSpeaker(
            id = "sp_1",
            name = "Alan Watts",
            bio = "East-West comparative philosopher, famous for 'The Way of Zen' and 'The Art of Flow'.",
            avatarUrl = null,
            followersCount = 45200,
            audioCount = 142
        ),
        DiscoverSpeaker(
            id = "sp_2",
            name = "Marcus Aurelius",
            bio = "Stoic Philosopher and Roman Emperor, writer of the famous private journal 'Meditations'.",
            avatarUrl = null,
            followersCount = 38900,
            audioCount = 48
        ),
        DiscoverSpeaker(
            id = "sp_3",
            name = "Nikola Tesla",
            bio = "Visionary inventor, physicist, and mechanical engineer whose lectures outline frequency resonance.",
            avatarUrl = null,
            followersCount = 29400,
            audioCount = 12
        ),
        DiscoverSpeaker(
            id = "sp_4",
            name = "Epictetus",
            bio = "Enslaved philosopher who taught that while we cannot control external events, we control our reaction to them.",
            avatarUrl = null,
            followersCount = 18500,
            audioCount = 31
        )
    )

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

    override suspend fun getSpeakers(page: Int, limit: Int): List<DiscoverSpeaker> {
        val start = (page - 1) * limit
        if (start >= mockSpeakers.size) return emptyList()
        val end = (start + limit).coerceAtMost(mockSpeakers.size)
        return mockSpeakers.subList(start, end)
    }

    override suspend fun getSpeakerDetails(id: String): DiscoverSpeaker {
        return mockSpeakers.firstOrNull { it.id == id } 
            ?: throw NoSuchElementException("Speaker not found with ID: $id")
    }

    override suspend fun getSpeakerTracks(id: String): List<DiscoverAudioItem> {
        val speakerName = getSpeakerDetails(id).name
        return mockTracks.filter { it.speaker.contains(speakerName, ignoreCase = true) }
    }
}
