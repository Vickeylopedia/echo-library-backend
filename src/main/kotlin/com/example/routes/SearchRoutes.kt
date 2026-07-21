package com.example.routes

import com.example.config.DependencyContainer
import com.example.services.SearchEngine
import com.example.services.SearchService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.searchRoutes(searchService: SearchService) {
    val searchEngine = DependencyContainer.searchEngine

    route("/api/v1/search") {
        // Global search endpoint with extensive filters, sort, and pagination
        get {
            val q = call.request.queryParameters["q"] ?: ""
            val providerId = call.request.queryParameters["provider"]
            val speakerId = call.request.queryParameters["speaker"]
            val categoryId = call.request.queryParameters["category"]
            val duration = call.request.queryParameters["duration"]
            val dateAdded = call.request.queryParameters["date_added"]
            val podcastId = call.request.queryParameters["podcast"]
            val genre = call.request.queryParameters["genre"]
            val sort = call.request.queryParameters["sort"] ?: "relevance"
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

            try {
                val results = searchEngine.search(
                    q = q,
                    providerId = providerId,
                    speakerId = speakerId,
                    categoryId = categoryId,
                    durationRange = duration,
                    dateAddedFilter = dateAdded,
                    podcastId = podcastId,
                    genre = genre,
                    sort = sort,
                    page = page,
                    limit = limit
                )
                call.respond(HttpStatusCode.OK, results)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed executing global search")))
            }
        }

        // Search suggestions legacy compatibility route
        get("/suggestions") {
            val query = call.request.queryParameters["q"] ?: ""
            val suggestions = searchService.getSearchSuggestions(query)
            call.respond(HttpStatusCode.OK, suggestions)
        }

        // Get popular / trending searches
        get("/trending") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val trending = searchEngine.getTrendingSearches(limit)
            call.respond(HttpStatusCode.OK, trending)
        }

        // Get recently added episodes
        get("/recent") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            try {
                val results = searchEngine.search(
                    q = "",
                    sort = "newest",
                    page = 1,
                    limit = limit
                )
                call.respond(HttpStatusCode.OK, results.messages)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed fetching recently added content")))
            }
        }

        // Get popular speakers
        get("/popular-speakers") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            try {
                val results = searchEngine.search(
                    q = "",
                    sort = "relevance",
                    page = 1,
                    limit = limit
                )
                call.respond(HttpStatusCode.OK, results.speakers)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed fetching popular speakers")))
            }
        }
    }
}
