package com.example.routes

import com.example.services.PodcastService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.podcastRoutes(podcastService: PodcastService) {
    route("/api/v1/podcasts") {
        get {
            val podcasts = podcastService.getFeaturedPodcasts()
            call.respond(HttpStatusCode.OK, podcasts)
        }

        get("/{id}/episodes") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing Podcast ID"))
            val episodes = podcastService.getPodcastEpisodes(id)
            call.respond(HttpStatusCode.OK, episodes)
        }
    }
}
