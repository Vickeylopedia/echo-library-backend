package com.example.indexing

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

class IndexingScheduler(private val pipeline: IndexingPipeline) {
    private val logger = LoggerFactory.getLogger(IndexingScheduler::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null
    
    val isRunning = AtomicBoolean(false)
    
    // Configurable parameters (non-hardcoded, defaults provided)
    var intervalMs: Long = 3600_000 * 12 // Default to every 12 hours
    var isEnabled: Boolean = true

    /**
     * Starts the periodic background runner.
     */
    fun start() {
        if (!isEnabled) {
            logger.info("Indexing Scheduler is disabled by configuration.")
            return
        }
        
        if (isRunning.getAndSet(true)) {
            logger.warn("Indexing Scheduler is already running.")
            return
        }

        logger.info("Starting Indexing Scheduler background job (interval: ${intervalMs / 1000}s)...")
        job = scope.launch {
            while (isActive) {
                try {
                    logger.info("Scheduler: Triggering periodic background content indexing pipeline...")
                    val stats = pipeline.executeAll()
                    logger.info("Scheduler: Content indexing finished with results: $stats")
                } catch (e: Exception) {
                    logger.error("Scheduler: Exception during background content indexing execution", e)
                }
                delay(intervalMs)
            }
        }
    }

    /**
     * Stops the periodic background runner.
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            return
        }
        logger.info("Stopping Indexing Scheduler background job...")
        job?.cancel()
        job = null
    }

    /**
     * Triggers manual complete execution.
     */
    suspend fun triggerManualAll(): List<IndexingStats> {
        logger.info("Scheduler: Manual global indexing execution triggered.")
        return pipeline.executeAll()
    }

    /**
     * Triggers manual single-provider execution.
     */
    suspend fun triggerManualForProvider(providerName: String): IndexingStats? {
        logger.info("Scheduler: Manual indexing execution triggered for provider: $providerName")
        return pipeline.executeForProviderByName(providerName)
    }
}
