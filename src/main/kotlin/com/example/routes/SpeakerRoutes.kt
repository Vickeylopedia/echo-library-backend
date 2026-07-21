package com.example.routes

import com.example.services.SpeakerService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.speakerRoutes(speakerService: SpeakerService) {
    route("/api/v1/speakers") {
        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val speakers = speakerService.getSpeakers(page, limit)
            call.respond(HttpStatusCode.OK, speakers)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing Speaker ID"))
            val details = speakerService.getSpeakerDetails(id)
            call.respond(HttpStatusCode.OK, details)
        }

        get("/{id}/tracks") {
            val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing Speaker ID"))
            val tracks = speakerService.getSpeakerTracks(id)
            call.respond(HttpStatusCode.OK, tracks)
        }
    }
}
