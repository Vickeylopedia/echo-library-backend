package com.example.repositories

import com.example.config.AppConfig
import com.example.config.SupabaseClient
import com.example.models.AuthUser
import com.example.models.LoginRequest
import com.example.models.RegisterRequest
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

interface AuthRepository {
    suspend fun login(request: LoginRequest): AuthUser
    suspend fun register(request: RegisterRequest): AuthUser
    suspend fun logout(): Unit
}

class SupabaseAuthRepository(private val supabase: SupabaseClient) : AuthRepository {
    private val logger = LoggerFactory.getLogger(SupabaseAuthRepository::class.java)
    private val jsonHelper = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val authUrl = if (AppConfig.supabaseUrl.endsWith("/")) "${AppConfig.supabaseUrl}auth/v1" else "${AppConfig.supabaseUrl}/auth/v1"

    override suspend fun login(request: LoginRequest): AuthUser {
        logger.info("Performing Supabase GoTrue login for email: ${request.email}")
        try {
            val response = supabase.client.post("$authUrl/token?grant_type=password") {
                header("apikey", AppConfig.supabaseKey)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("email", request.email)
                    put("password", request.password)
                })
            }

            if (response.status.value in 200..299) {
                val json = jsonHelper.parseToJsonElement(response.bodyAsText()).jsonObject
                val accessToken = json["access_token"]?.jsonPrimitive?.content ?: ""
                val user = json["user"]!!.jsonObject
                val id = user["id"]!!.jsonPrimitive.content
                val email = user["email"]?.jsonPrimitive?.content ?: request.email
                
                // Get or create user profile in public.users table to ensure database consistency
                val username = email.substringBefore("@")
                ensureUserProfileExists(id, email, username)

                return AuthUser(
                    id = id,
                    email = email,
                    username = username,
                    token = accessToken,
                    displayName = username,
                    avatarUrl = null
                )
            } else {
                val errBody = response.bodyAsText()
                logger.error("Supabase login failed: $errBody")
                throw IllegalArgumentException("Invalid login credentials: $errBody")
            }
        } catch (e: Exception) {
            logger.error("Error during login", e)
            throw e
        }
    }

    override suspend fun register(request: RegisterRequest): AuthUser {
        logger.info("Performing Supabase GoTrue signup for email: ${request.email}")
        try {
            val response = supabase.client.post("$authUrl/signup") {
                header("apikey", AppConfig.supabaseKey)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("email", request.email)
                    put("password", request.password)
                    put("data", buildJsonObject {
                        put("username", request.username)
                    })
                })
            }

            if (response.status.value in 200..299) {
                val json = jsonHelper.parseToJsonElement(response.bodyAsText()).jsonObject
                val user = json["user"]?.jsonObject ?: json
                val id = user["id"]!!.jsonPrimitive.content
                val email = user["email"]?.jsonPrimitive?.content ?: request.email
                val accessToken = json["access_token"]?.jsonPrimitive?.content ?: "requires_email_confirmation_or_empty"

                // Create user profile in public.users table
                ensureUserProfileExists(id, email, request.username)

                return AuthUser(
                    id = id,
                    email = email,
                    username = request.username,
                    token = accessToken,
                    displayName = request.username,
                    avatarUrl = null
                )
            } else {
                val errBody = response.bodyAsText()
                logger.error("Supabase registration failed: $errBody")
                throw IllegalArgumentException("Registration failed: $errBody")
            }
        } catch (e: Exception) {
            logger.error("Error during registration", e)
            throw e
        }
    }

    override suspend fun logout() {
        logger.info("Performing Supabase GoTrue logout")
    }

    private suspend fun ensureUserProfileExists(id: String, email: String, username: String) {
        try {
            // Check if user already exists in public.users table
            val checkRes = supabase.get("users", mapOf("id" to "eq.$id"))
            if (checkRes.status.value in 200..299) {
                val arr = jsonHelper.parseToJsonElement(checkRes.bodyAsText()).jsonArray
                if (arr.isNotEmpty()) {
                    logger.info("User profile already exists in public.users for id: $id")
                    return
                }
            }

            // Create user profile
            val profileBody = buildJsonObject {
                put("id", id)
                put("email", email)
                put("username", username)
                put("display_name", username)
                put("avatar_url", "")
            }
            val insertRes = supabase.post("users", profileBody)
            if (insertRes.status.value !in 200..299) {
                logger.error("Failed to insert user profile in public.users: ${insertRes.bodyAsText()}")
            } else {
                logger.info("Successfully created user profile in public.users for id: $id")
            }
        } catch (e: Exception) {
            logger.error("Exception in ensureUserProfileExists for id: $id", e)
        }
    }
}

class MockAuthRepository : AuthRepository {
    override suspend fun login(request: LoginRequest): AuthUser {
        if (request.email.isBlank() || request.password.isBlank()) {
            throw IllegalArgumentException("Email and password are required")
        }
        return AuthUser(
            id = "usr_94821",
            email = request.email,
            username = request.email.substringBefore("@"),
            token = "jwt_mock_token_7128a38b",
            displayName = "Echo Explorer",
            avatarUrl = null
        )
    }

    override suspend fun register(request: RegisterRequest): AuthUser {
        if (request.email.isBlank() || request.username.isBlank() || request.password.length < 6) {
            throw IllegalArgumentException("Invalid details. Password must be at least 6 characters.")
        }
        return AuthUser(
            id = "usr_94821",
            email = request.email,
            username = request.username,
            token = "jwt_mock_token_7128a38b",
            displayName = request.username,
            avatarUrl = null
        )
    }

    override suspend fun logout() {
        // Mock success
    }
}
