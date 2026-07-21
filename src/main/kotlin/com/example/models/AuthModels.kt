package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: String,
    val email: String,
    val username: String,
    val token: String,
    val displayName: String?,
    val avatarUrl: String?
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String
)

@Serializable
data class ErrorResponse(
    val status: Int,
    val message: String
)
