package com.example.routes

import com.example.config.AppConfig
import com.example.config.DatabaseHealthTracker
import com.example.config.DependencyContainer
import io.ktor.client.statement.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*

fun Route.healthRoutes() {
    get("/health") {
        val dbStatus = DatabaseHealthTracker.databaseStatus.get()
        val schedulerRunning = DependencyContainer.indexingScheduler.isRunning.get()
        
        val healthStatus = if (dbStatus == "CONNECTED") "HEALTHY" else "UNHEALTHY"
        
        call.respond(mapOf(
            "status" to healthStatus,
            "databaseStatus" to dbStatus,
            "supabaseConnection" to if (dbStatus == "CONNECTED") "CONNECTED" else "DISCONNECTED",
            "schedulerStatus" to if (schedulerRunning) "RUNNING" else "STOPPED",
            "version" to "1.0.0",
            "buildTime" to "2026-07-21T03:36:28-07:00",
            "environment" to AppConfig.env,
            "timestamp" to System.currentTimeMillis()
        ))
    }

    get("/diagnostics") {
        // JVM memory stats
        val runtime = Runtime.getRuntime()
        val freeMemory = runtime.freeMemory()
        val totalMemory = runtime.totalMemory()
        val maxMemory = runtime.maxMemory()
        val usedMemory = totalMemory - freeMemory
        
        val memoryMap = mapOf(
            "free_bytes" to freeMemory,
            "total_bytes" to totalMemory,
            "max_bytes" to maxMemory,
            "used_bytes" to usedMemory,
            "used_percentage" to if (totalMemory > 0) (usedMemory.toDouble() / totalMemory.toDouble() * 100.0) else 0.0
        )
        
        // Query active downloads from Supabase
        val activeDownloadsCount = try {
            val res = DependencyContainer.supabaseClient.get("downloads", mapOf("download_status" to "eq.downloading"))
            if (res.status.value in 200..299) {
                val arr = Json.parseToJsonElement(res.bodyAsText()).jsonArray
                arr.size
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
        
        val activeIndexers = DependencyContainer.indexingPipeline.activeIndexersCount.get()
        val schedulerRunning = DependencyContainer.indexingScheduler.isRunning.get()
        val lastDbLatency = DatabaseHealthTracker.lastCheckLatencyMs.get()
        
        call.respond(mapOf(
            "latency_ms" to lastDbLatency,
            "memory" to memoryMap,
            "active_downloads" to activeDownloadsCount,
            "active_indexers" to activeIndexers,
            "scheduler_status" to if (schedulerRunning) "RUNNING" else "STOPPED",
            "cache_status" to "NOT_CONFIGURED"
        ))
    }
}

