package com.example.indexing

import org.slf4j.LoggerFactory

class NaijaSermonProvider : ContentProvider {
    private val logger = LoggerFactory.getLogger(NaijaSermonProvider::class.java)
    override val name: String = "NaijaSermon"

    override suspend fun fetchLatest(): List<RawContentRecord> {
        logger.info("$name: Fetching latest sermons")
        return listOf(
            RawContentRecord(
                externalId = "ns-001",
                title = "The Power of Spiritual Diligence   ",
                speakerName = "Apostle Joshua Selman ",
                durationString = "1:45:20",
                coverUrl = "https://images.unsplash.com/photo-1518173946687-a4c8a383392c",
                genre = "Sermon",
                description = "Deep teaching on growing spiritually through consistent prayers and diligence.",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                providerName = name,
                categoryName = "Faith & Prayer",
                podcastTitle = "Koinonia Global Weekly"
            ),
            RawContentRecord(
                externalId = "ns-002",
                title = "Understanding Divine Timing!!",
                speakerName = "Dr. Paul Enenche",
                durationString = "58:40",
                coverUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c",
                genre = "Sermon",
                description = "Understanding how God schedules events in your life.",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                providerName = name,
                categoryName = "Wisdom & Destiny",
                podcastTitle = "Dunamis Messages"
            )
        )
    }

    override suspend fun fetchByPage(page: Int, limit: Int): List<RawContentRecord> = fetchLatest().take(limit)

    override suspend fun fetchByCategory(category: String, page: Int, limit: Int): List<RawContentRecord> {
        return fetchLatest().filter { it.categoryName?.equals(category, ignoreCase = true) == true }.take(limit)
    }

    override suspend fun fetchBySpeaker(speaker: String, page: Int, limit: Int): List<RawContentRecord> {
        return fetchLatest().filter { it.speakerName.contains(speaker, ignoreCase = true) }.take(limit)
    }

    override suspend fun healthCheck(): Boolean {
        logger.info("$name: Performing health check")
        return true
    }
}

class SpiritNerdsProvider : ContentProvider {
    private val logger = LoggerFactory.getLogger(SpiritNerdsProvider::class.java)
    override val name: String = "SpiritNerds"

    override suspend fun fetchLatest(): List<RawContentRecord> {
        logger.info("$name: Fetching latest audio messages")
        return listOf(
            RawContentRecord(
                externalId = "sn-001",
                title = "  Atmosphere of Miracles & Healing  ",
                speakerName = " Apostle Joshua Selman",
                durationString = "2:10:15",
                coverUrl = "https://images.unsplash.com/photo-1516280440614-37939bbacd6a",
                genre = "Worship & Word",
                description = "A powerful atmosphere of healing and worship recording.",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                providerName = name,
                categoryName = "Miracles & Healing",
                podcastTitle = "Koinonia Audio Archive"
            ),
            RawContentRecord(
                externalId = "sn-002",
                title = "The Principle of Honour",
                speakerName = "Pastor  E.A. Adeboye",
                durationString = "45:00",
                coverUrl = "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae",
                genre = "Sermon",
                description = "Keys to receiving blessings and opening closed doors through gratitude and respect.",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                providerName = name,
                categoryName = "Christian Living",
                podcastTitle = "RCCG Holy Ghost Service"
            )
        )
    }

    override suspend fun fetchByPage(page: Int, limit: Int): List<RawContentRecord> = fetchLatest().take(limit)

    override suspend fun fetchByCategory(category: String, page: Int, limit: Int): List<RawContentRecord> {
        return fetchLatest().filter { it.categoryName?.equals(category, ignoreCase = true) == true }.take(limit)
    }

    override suspend fun fetchBySpeaker(speaker: String, page: Int, limit: Int): List<RawContentRecord> {
        return fetchLatest().filter { it.speakerName.contains(speaker, ignoreCase = true) }.take(limit)
    }

    override suspend fun healthCheck(): Boolean {
        logger.info("$name: Performing health check")
        return true
    }
}

class ListenNotesProvider : ContentProvider {
    private val logger = LoggerFactory.getLogger(ListenNotesProvider::class.java)
    override val name: String = "ListenNotes"

    override suspend fun fetchLatest(): List<RawContentRecord> {
        logger.info("$name: Fetching latest trending podcasts")
        return listOf(
            RawContentRecord(
                externalId = "ln-001",
                title = "Developing Unshakeable Focus",
                speakerName = "Dr. Andrew Huberman",
                durationString = "1:15:00",
                coverUrl = "https://images.unsplash.com/photo-1512428559087-560fa5ceab42",
                genre = "Education",
                description = "Neurobiology of human focus, tools to enhance concentration and neuroplasticity.",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                providerName = name,
                categoryName = "Mental Health & Science",
                podcastTitle = "The Huberman Lab Digest"
            ),
            RawContentRecord(
                externalId = "ln-002",
                title = "Modern Stoic Philosophies for Turbulent Times",
                speakerName = "Ryan Holiday",
                durationString = "32:15",
                coverUrl = "https://images.unsplash.com/photo-1506157786151-b8491531f063",
                genre = "Philosophy",
                description = "Practical wisdom from Marcus Aurelius and Seneca for managing everyday chaos.",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                providerName = name,
                categoryName = "Self-Improvement",
                podcastTitle = "The Daily Stoic podcast"
            )
        )
    }

    override suspend fun fetchByPage(page: Int, limit: Int): List<RawContentRecord> = fetchLatest().take(limit)

    override suspend fun fetchByCategory(category: String, page: Int, limit: Int): List<RawContentRecord> {
        return fetchLatest().filter { it.categoryName?.equals(category, ignoreCase = true) == true }.take(limit)
    }

    override suspend fun fetchBySpeaker(speaker: String, page: Int, limit: Int): List<RawContentRecord> {
        return fetchLatest().filter { it.speakerName.contains(speaker, ignoreCase = true) }.take(limit)
    }

    override suspend fun healthCheck(): Boolean {
        logger.info("$name: Performing health check")
        return true
    }
}
