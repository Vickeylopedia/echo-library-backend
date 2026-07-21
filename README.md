# Echo Library Backend V2

A modular, production-ready backend designed with Kotlin & Ktor for the **Echo Library** Android ecosystem. Prepared for immediate deployment to **Railway** and integration with **Supabase**.

---

## 🏗️ Architecture Design

The project uses a clean modular architecture based on the **Repository & Service Pattern**, separating layers cleanly:

```
echo-library-backend/
├── src/
│   └── main/
│       ├── kotlin/com/example/
│       │   ├── config/          # Environment Configurations & Simple Dependency Injection
│       │   ├── models/          # Serializable Request & Response DTOs
│       │   ├── repositories/    # Database Abstraction / Live Mock repositories
│       │   ├── services/        # Service/Business Logic Layer & Input Validation
│       │   ├── plugins/         # Ktor Feature Configs (Routing, Serialization, StatusPages)
│       │   ├── routes/          # REST Endpoint handlers / Controllers
│       │   └── Application.kt   # Netty Entry Point
│       └── resources/
│           └── logback.xml      # Console Log Formatters
├── Dockerfile                   # Railway compatible multi-stage Docker build
├── railway.json                 # Railway service behavior profile
├── .env.example                 # Self-documenting environment configurations
└── build.gradle.kts             # Dependency management and task scripts
```

---

## 🛠️ Tech Stack & Capabilities

- **Framework**: [Ktor (Kotlin Server Framework)](https://ktor.io)
- **Engine**: Netty (High-performance embedded asynchronous web server)
- **Serialization**: Kotlinx.serialization (JSON support)
- **Deployment**: Railway Container Engine (multi-stage Gradle build via Dockerfile)
- **Logging**: SLF4J with Logback
- **CORS**: Fully configured to handle multiple origin policies smoothly
- **Error Handling**: StatusPages plugin mapped to validation, resource, and internal exceptions

---

## 🚀 API Endpoints

All functional endpoints are nested under the `/api/v1` namespace (except `/health` check):

| Method | Endpoint | Description | Public |
|--------|----------|-------------|--------|
| `GET` | `/health` | Server status and diagnostic uptime check | Yes |
| `POST` | `/api/v1/auth/register` | Register a new account (returns JWT user token) | Yes |
| `POST` | `/api/v1/auth/login` | Log in and retrieve credentials | Yes |
| `POST` | `/api/v1/auth/logout` | Revoke token credentials locally | Yes |
| `GET` | `/api/v1/search` | Search online recordings with keywords (`?q=`) | Yes |
| `GET` | `/api/v1/search/suggestions` | Fetch context suggestions as the user types (`?q=`) | Yes |
| `GET` | `/api/v1/speakers` | Fetch paginated speakers (`?page=1&limit=20`) | Yes |
| `GET` | `/api/v1/speakers/{id}` | Get detailed profile of a speaker | Yes |
| `GET` | `/api/v1/speakers/{id}/tracks` | Get all tracks uploaded by a speaker | Yes |
| `GET` | `/api/v1/podcasts` | Get all featured podcasts | Yes |
| `GET` | `/api/v1/podcasts/{id}/episodes` | Get all episodes belonging to a podcast | Yes |

---

## ⚙️ Environment Variables

Copy `.env.example` to `.env` and fill out corresponding values:

```bash
PORT=8080
KTOR_ENV=development

# JWT Configuration
JWT_SECRET=super_secret_jwt_key_change_me_in_production
JWT_ISSUER=https://api.echolibrary.example.com
JWT_AUDIENCE=echo-library-users

# Supabase Credentials (Ready for live DB integration)
SUPABASE_URL=https://your-supabase-project.supabase.co
SUPABASE_KEY=your-supabase-anon-key
SUPABASE_JWT_SECRET=your-supabase-jwt-secret
```

---

## 🚂 Railway Deployment Instructions

1. **Install Railway CLI** (if you prefer CLI deployments):
   ```bash
   railway login
   railway init
   ```
2. **Setup Environment Variables**:
   In your Railway Dashboard, navigate to **Variables** and add the configuration parameters outlined in `.env.example`.
3. **Deploy**:
   ```bash
   railway up
   ```
   Railway will automatically pick up the custom `Dockerfile` defined in the root folder, run a multi-stage gradle jar compilation, and expose port `8080`.
