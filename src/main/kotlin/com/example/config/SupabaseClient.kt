package com.example.config

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class SupabaseClient {
    private val logger = LoggerFactory.getLogger(SupabaseClient::class.java)

    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    private val baseUrl = if (AppConfig.supabaseUrl.endsWith("/")) AppConfig.supabaseUrl else "${AppConfig.supabaseUrl}/"
    private val restUrl = "${baseUrl}rest/v1/"
    private val anonKey = AppConfig.supabaseKey

    suspend fun get(table: String, queryParams: Map<String, String> = emptyMap()): HttpResponse {
        val url = "$restUrl$table"
        logger.info("Supabase GET request to table: $table, params: $queryParams")
        return client.get(url) {
            header("apikey", anonKey)
            header("Authorization", "Bearer $anonKey")
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
        }
    }

    suspend fun post(table: String, body: Any, preferReturn: String = "representation"): HttpResponse {
        val url = "$restUrl$table"
        logger.info("Supabase POST request to table: $table")
        return client.post(url) {
            header("apikey", anonKey)
            header("Authorization", "Bearer $anonKey")
            header("Prefer", "return=$preferReturn")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun patch(table: String, body: Any, queryParams: Map<String, String> = emptyMap()): HttpResponse {
        val url = "$restUrl$table"
        logger.info("Supabase PATCH request to table: $table, params: $queryParams")
        return client.patch(url) {
            header("apikey", anonKey)
            header("Authorization", "Bearer $anonKey")
            contentType(ContentType.Application.Json)
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
            setBody(body)
        }
    }

    suspend fun delete(table: String, queryParams: Map<String, String> = emptyMap()): HttpResponse {
        val url = "$restUrl$table"
        logger.info("Supabase DELETE request to table: $table, params: $queryParams")
        return client.delete(url) {
            header("apikey", anonKey)
            header("Authorization", "Bearer $anonKey")
            queryParams.forEach { (key, value) ->
                parameter(key, value)
            }
        }
    }
}
