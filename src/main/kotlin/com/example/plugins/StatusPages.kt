package com.example.plugins

import com.example.models.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory

fun Application.configureStatusPages() {
    val logger = LoggerFactory.getLogger("com.example.plugins.StatusPages")

    install(StatusPages) {
        // Handle validation errors
        exception<IllegalArgumentException> { call, cause ->
            logger.warn("Validation error: ${cause.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(status = HttpStatusCode.BadRequest.value, message = cause.message ?: "Invalid request input")
            )
        }

        // Handle item not found exceptions
        exception<NoSuchElementException> { call, cause ->
            logger.warn("Resource not found: ${cause.message}")
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(status = HttpStatusCode.NotFound.value, message = cause.message ?: "Requested resource not found")
            )
        }

        // Handle all fallback exceptions (Global handler)
        exception<Throwable> { call, cause ->
            logger.error("Unhandled runtime exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(status = HttpStatusCode.InternalServerError.value, message = "An internal server error occurred.")
            )
        }
    }
}
