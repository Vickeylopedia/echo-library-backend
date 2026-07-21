package com.example.plugins

import com.example.config.DependencyContainer
import com.example.routes.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        // Core health check
        healthRoutes()

        // Feature APIs (Injected with lazy services)
        authRoutes(DependencyContainer.authService)
        searchRoutes(DependencyContainer.searchService)
        speakerRoutes(DependencyContainer.speakerService)
        podcastRoutes(DependencyContainer.podcastService)
        indexingRoutes(DependencyContainer.indexingScheduler)
    }
}
