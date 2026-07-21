package com.example.config

import io.ktor.client.statement.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

object DatabaseHealthTracker {
    private val logger = LoggerFactory.getLogger(DatabaseHealthTracker::class.java)
    
    val databaseStatus = AtomicReference("UNKNOWN") // "CONNECTED", "DISCONNECTED", "UNKNOWN"
    val tableStatus = ConcurrentHashMap<String, String>()
    val lastCheckLatencyMs = AtomicLong(-1)
    val lastCheckTimestamp = AtomicLong(-1)
    val lastError = AtomicReference<String?>(null)

    private val requiredTables = listOf(
        "users",
        "speakers",
        "categories",
        "providers",
        "podcasts",
        "podcast_episodes",
        "messages",
        "collections",
        "collection_items",
        "favorites",
        "listening_progress",
        "completed_history",
        "notes",
        "insights",
        "search_history",
        "downloads"
    )

    fun runStartupValidation(scope: CoroutineScope, supabase: SupabaseClient) {
        scope.launch(Dispatchers.IO) {
            logger.info("DatabaseHealthTracker: Starting production database table validation checks...")
            verifyAllTables(supabase)
        }
    }

    suspend fun verifyAllTables(supabase: SupabaseClient): Boolean {
        var allOk = true
        val startTime = System.currentTimeMillis()
        var lastErrStr: String? = null

        for (table in requiredTables) {
            try {
                val tableStartTime = System.currentTimeMillis()
                val response = supabase.get(table, mapOf("limit" to "1"))
                val latency = System.currentTimeMillis() - tableStartTime
                
                if (response.status.value in 200..299) {
                    tableStatus[table] = "EXISTS"
                    logger.info("DatabaseHealthTracker: Table '$table' successfully validated. Latency: ${latency}ms")
                } else {
                    tableStatus[table] = "ERROR (${response.status.value})"
                    val body = response.bodyAsText()
                    allOk = false
                    lastErrStr = "Table '$table' returned status ${response.status.value}: $body"
                    logger.error("DatabaseHealthTracker: Table '$table' validation failed! $lastErrStr")
                }
            } catch (e: Exception) {
                tableStatus[table] = "UNREACHABLE"
                allOk = false
                lastErrStr = e.message ?: e.toString()
                logger.error("DatabaseHealthTracker: Table '$table' validation failed with exception", e)
            }
        }

        val totalLatency = System.currentTimeMillis() - startTime
        lastCheckLatencyMs.set(totalLatency)
        lastCheckTimestamp.set(System.currentTimeMillis())

        if (allOk) {
            databaseStatus.set("CONNECTED")
            lastError.set(null)
            logger.info("DatabaseHealthTracker: All required database tables successfully verified! Total Latency: ${totalLatency}ms")
        } else {
            databaseStatus.set("DISCONNECTED")
            lastError.set(lastErrStr)
            logger.error("DatabaseHealthTracker: Database validation checks finished with errors.")
        }
        return allOk
    }
}
