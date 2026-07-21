package com.example

import com.example.config.AppConfig
import com.example.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = AppConfig.port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val supabaseClient = com.example.config.DependencyContainer.supabaseClient
    
    // Run startup database table validation checks
    com.example.config.DatabaseHealthTracker.runStartupValidation(this, supabaseClient)

    // Start background content indexing scheduler
    com.example.config.DependencyContainer.indexingScheduler.start()

    configureMonitoring()
    configureHTTP()
    configureSerialization()
    configureStatusPages()
    configureRouting()
}
