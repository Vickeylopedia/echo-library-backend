package com.example.routes

import com.example.config.DependencyContainer
import com.example.indexing.IndexingScheduler
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class IndexingConfigUpdateRequest(
    val intervalMs: Long?,
    val isEnabled: Boolean?
)

@Serializable
data class IndexingStatusResponse(
    val isEnabled: Boolean,
    val intervalSeconds: Long,
    val providers: List<String>
)

fun Route.indexingRoutes(scheduler: IndexingScheduler) {
    route("/api/v1/indexing") {
        
        // Get the current status and config of the indexing scheduler
        get("/status") {
            val response = IndexingStatusResponse(
                isEnabled = scheduler.isEnabled,
                intervalSeconds = scheduler.intervalMs / 1000,
                providers = listOf("NaijaSermon", "SpiritNerds", "ListenNotes")
            )
            call.respond(HttpStatusCode.OK, response)
        }

        // Trigger manual execution of the pipeline for ALL active providers
        post("/run") {
            try {
                val stats = scheduler.triggerManualAll()
                call.respond(HttpStatusCode.OK, mapOf("message" to "Manual indexing completed", "results" to stats))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed executing manual indexing")))
            }
        }

        // Trigger manual execution for a SPECIFIC provider
        post("/run/{provider}") {
            val providerName = call.parameters["provider"]
            if (providerName.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Provider name parameter is required"))
                return@post
            }

            try {
                val stats = scheduler.triggerManualForProvider(providerName)
                if (stats == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Provider '$providerName' not found"))
                } else {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Manual indexing for $providerName completed", "result" to stats))
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Failed executing manual provider indexing")))
            }
        }

        // Dynamically update the scheduler configuration at runtime
        post("/config") {
            try {
                val request = call.receive<IndexingConfigUpdateRequest>()
                
                request.isEnabled?.let {
                    scheduler.isEnabled = it
                    if (it) scheduler.start() else scheduler.stop()
                }
                request.intervalMs?.let {
                    scheduler.intervalMs = it
                    // Cycle scheduler to apply new intervals
                    if (scheduler.isEnabled) {
                        scheduler.stop()
                        scheduler.start()
                    }
                }

                call.respond(HttpStatusCode.OK, mapOf(
                    "message" to "Configuration updated successfully",
                    "isEnabled" to scheduler.isEnabled,
                    "intervalSeconds" to scheduler.intervalMs / 1000
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid config update request: ${e.message}"))
            }
        }
    }
}
