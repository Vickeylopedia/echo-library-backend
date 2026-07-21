package com.example.services

import com.example.models.AuthUser
import com.example.models.LoginRequest
import com.example.models.RegisterRequest
import com.example.repositories.AuthRepository

interface AuthService {
    suspend fun login(request: LoginRequest): AuthUser
    suspend fun register(request: RegisterRequest): AuthUser
    suspend fun logout(): Unit
}

class AuthServiceImpl(private val authRepository: AuthRepository) : AuthService {
    override suspend fun login(request: LoginRequest): AuthUser {
        require(request.email.isNotBlank()) { "Email cannot be empty" }
        require(request.password.isNotBlank()) { "Password cannot be empty" }
        return authRepository.login(request)
    }

    override suspend fun register(request: RegisterRequest): AuthUser {
        require(request.email.isNotBlank()) { "Email cannot be empty" }
        require(request.email.contains("@")) { "Please enter a valid email address" }
        require(request.username.isNotBlank()) { "Username cannot be empty" }
        require(request.password.length >= 6) { "Password must be at least 6 characters long" }
        return authRepository.register(request)
    }

    override suspend fun logout() {
        authRepository.logout()
    }
}
