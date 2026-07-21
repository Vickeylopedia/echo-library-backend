package com.example.services

import com.example.models.DiscoverAudioItem
import com.example.repositories.SearchRepository

interface SearchService {
    suspend fun searchTracks(query: String): List<DiscoverAudioItem>
    suspend fun getSearchSuggestions(query: String): List<String>
}

class SearchServiceImpl(private val searchRepository: SearchRepository) : SearchService {
    override suspend fun searchTracks(query: String): List<DiscoverAudioItem> {
        return searchRepository.search(query.trim())
    }

    override suspend fun getSearchSuggestions(query: String): List<String> {
        return searchRepository.suggestions(query.trim())
    }
}
