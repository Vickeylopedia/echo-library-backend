package com.example.config

object AppConfig {
    val port: Int by lazy {
        System.getenv("PORT")?.toIntOrNull() ?: 8080
    }

    val env: String by lazy {
        System.getenv("KTOR_ENV") ?: "development"
    }

    val jwtSecret: String by lazy {
        System.getenv("JWT_SECRET") ?: "super_secret_jwt_key_change_me_in_production"
    }

    val backendBaseUrl: String by lazy {
        System.getenv("BACKEND_BASE_URL") ?: "http://localhost:8080"
    }

    val jwtIssuer: String by lazy {
        System.getenv("JWT_ISSUER") ?: "https://api.echolibrary.example.com"
    }

    val jwtAudience: String by lazy {
        System.getenv("JWT_AUDIENCE") ?: "echo-library-users"
    }

    // Supabase Credentials
    val supabaseUrl: String by lazy {
        System.getenv("SUPABASE_URL") ?: "https://your-supabase-project.supabase.co"
    }

    val supabaseKey: String by lazy {
        System.getenv("SUPABASE_ANON_KEY") ?: System.getenv("SUPABASE_KEY") ?: "your-supabase-anon-key"
    }

    val supabaseServiceRole: String by lazy {
        System.getenv("SUPABASE_SERVICE_ROLE") ?: "your-supabase-service-role-key"
    }

    val supabaseJwtSecret: String by lazy {
        System.getenv("SUPABASE_JWT_SECRET") ?: "your-supabase-jwt-secret"
    }

    val isDevelopment: Boolean
        get() = env.equals("development", ignoreCase = true)
}
