package com.example.services

import com.example.models.DiscoverSpeaker
import com.example.models.DiscoverAudioItem
import com.example.repositories.SpeakerRepository

interface SpeakerService {
    suspend fun getSpeakers(page: Int, limit: Int): List<DiscoverSpeaker>
    suspend fun getSpeakerDetails(id: String): DiscoverSpeaker
    suspend fun getSpeakerTracks(id: String): List<DiscoverAudioItem>
}

class SpeakerServiceImpl(private val speakerRepository: SpeakerRepository) : SpeakerService {
    override suspend fun getSpeakers(page: Int, limit: Int): List<DiscoverSpeaker> {
        val validPage = if (page < 1) 1 else page
        val validLimit = if (limit < 1 || limit > 100) 20 else limit
        return speakerRepository.getSpeakers(validPage, validLimit)
    }

    override suspend fun getSpeakerDetails(id: String): DiscoverSpeaker {
        require(id.isNotBlank()) { "Speaker ID cannot be blank" }
        return speakerRepository.getSpeakerDetails(id)
    }

    override suspend fun getSpeakerTracks(id: String): List<DiscoverAudioItem> {
        require(id.isNotBlank()) { "Speaker ID cannot be blank" }
        return speakerRepository.getSpeakerTracks(id)
    }
}
