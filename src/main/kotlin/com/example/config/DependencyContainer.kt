package com.example.config

import com.example.repositories.*
import com.example.services.*
import com.example.indexing.*

object DependencyContainer {
    // Supabase Core Client
    val supabaseClient: SupabaseClient by lazy { SupabaseClient() }

    // Indexing Pipeline & Scheduler
    val indexingPipeline: IndexingPipeline by lazy { IndexingPipeline(supabaseClient) }
    val indexingScheduler: IndexingScheduler by lazy { IndexingScheduler(indexingPipeline) }

    // Search Engine
    val searchEngine: SearchEngine by lazy { SearchEngine(supabaseClient) }

    // Repositories (Supabase-backed with graceful mock fallbacks)
    val authRepository: AuthRepository by lazy { SupabaseAuthRepository(supabaseClient) }
    val searchRepository: SearchRepository by lazy { SupabaseSearchRepository(searchEngine) }
    val speakerRepository: SpeakerRepository by lazy { SupabaseSpeakerRepository(supabaseClient) }
    val podcastRepository: PodcastRepository by lazy { SupabasePodcastRepository(supabaseClient) }

    // Specialized Database Relational Repositories
    val userRepository: UserRepository by lazy { SupabaseUserRepository(supabaseClient) }
    val messageRepository: MessageRepository by lazy { SupabaseMessageRepository(supabaseClient) }
    val categoryRepository: CategoryRepository by lazy { SupabaseCategoryRepository(supabaseClient) }
    val collectionRepository: CollectionRepository by lazy { SupabaseCollectionRepository(supabaseClient) }
    val favoriteRepository: FavoriteRepository by lazy { SupabaseFavoriteRepository(supabaseClient) }
    val listeningProgressRepository: ListeningProgressRepository by lazy { SupabaseListeningProgressRepository(supabaseClient) }
    val noteRepository: NoteRepository by lazy { SupabaseNoteRepository(supabaseClient) }
    val insightRepository: InsightRepository by lazy { SupabaseInsightRepository(supabaseClient) }
    val searchHistoryRepository: SearchHistoryRepository by lazy { SupabaseSearchHistoryRepository(supabaseClient) }
    val downloadRepository: DownloadRepository by lazy { SupabaseDownloadRepository(supabaseClient) }
    val providerRepository: ProviderRepository by lazy { SupabaseProviderRepository(supabaseClient) }

    // Services (Lazy & Thread-safe)
    val authService: AuthService by lazy { AuthServiceImpl(authRepository) }
    val searchService: SearchService by lazy { SearchServiceImpl(searchRepository) }
    val speakerService: SpeakerService by lazy { SpeakerServiceImpl(speakerRepository) }
    val podcastService: PodcastService by lazy { PodcastServiceImpl(podcastRepository) }
}
