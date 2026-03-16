# Auth & Account Management Service

A production-grade **Spring Boot 3.2 / Java 17** backend for authentication, user management, roles, permissions, API keys, 2FA, and audit logging — backed by PostgreSQL.

---

## ✨ Features

| Feature | Technology |
|---|---|
| JWT Authentication (RS256) | JJWT 0.12 |
| API Key Authentication | SHA-256 hashed keys |
| Password Hashing | BCrypt (strength 12) |
| Two-Factor Authentication | TOTP (Google Authenticator) |
| Rate Limiting | Bucket4j (per-IP & per-key) |
| Multi-tenancy | Row-level isolation via `tenant_id` |
| Audit Logging | Async, non-blocking with JSONB metadata |
| Email Notifications | Spring Mail (Mailtrap/SMTP) |
| API Documentation | Springdoc OpenAPI 3 / Swagger UI |
| Database Migrations | Flyway |

---

## 🚀 Quick Start

### Prerequisites
- **Java 21+** (Current project Target)
- **Node.js 18+** (for Frontend Dashboard)
- **Maven 3.8+**
- **PostgreSQL 15+** running locally
- **OpenSSL** (for RSA key generation)

### 1. Clone the Repository
```bash
git clone https://github.com/Hikaru203/auth.git
cd auth
```

### 2. Configure Environment
Sensitive data has been gitignored. You must set up your local environment configuration:
1. Copy the example environment file:
   ```bash
   cp .env.example .env
   ```
2. Open `.env` and fill in your actual PostgreSQL database credentials.

### 3. Database Setup
Create a new database in PostgreSQL:
```sql
CREATE DATABASE authdb;
```

### 4. Generate RSA Keypair
The application uses **RS256** for JWT signing. You must generate a keypair before starting.

- **Windows**: Run `generate-keys.bat`
- **Linux/Mac**: Run `chmod +x generate-keys.sh && ./generate-keys.sh`

Keys will be saved to `src/main/resources/keys/` (excluded from Git).

### 5. Run the Application
#### Backend (Java Spring Boot)
```bash
mvn spring-boot:run
```
- **API URL**: `http://localhost:8080/api/v1`
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`

#### Frontend (Dashboard Interface)
Navigate to the frontend directory and start the Vite dev server:
```bash
cd dashboard-web
npm install
npm run dev
```

---

## 🔑 Authentication

### JWT Bearer Token

> [!IMPORTANT]
> The `totpCode` field is now **mandatory** in the login request. Use `"000000"` if 2FA is not enabled for the user.

```bash
# 1. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "tenantSlug": "default",
    "username": "admin",
    "password": "Admin@123",
    "totpCode": "000000"
  }'

# 2. Use Access Token
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <accessToken>"
```

### Full API Guide
For detailed instructions and examples for every endpoint (User Management, Roles, API Keys, etc.), please refer to:
👉 **[TESTING.md](./TESTING.md)**

---

## 📋 Default Credentials

| Field | Value |
|---|---|
| Tenant Slug | `default` |
| Username | `admin` |
| Password | `Admin@123` |

---

## 📡 API Endpoints

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/api/v1/auth/login` | Login | Public |
| POST | `/api/v1/auth/refresh` | Refresh token | Public |
| POST | `/api/v1/auth/logout` | Logout | Bearer |
| POST | `/api/v1/auth/password/reset-request` | Password reset email | Public |
| POST | `/api/v1/auth/password/reset` | Complete reset | Public |
| POST | `/api/v1/auth/password/change` | Change password | Bearer |
| POST | `/api/v1/auth/2fa/setup` | Setup TOTP 2FA | Bearer |
| GET | `/api/v1/users` | List users | ADMIN |
| POST | `/api/v1/users` | Create user | ADMIN |
| GET | `/api/v1/users/me` | My profile | Bearer |
| POST | `/api/v1/users/{id}/lock` | Lock user | ADMIN |
| GET | `/api/v1/roles` | List roles | Bearer |
| POST | `/api/v1/roles` | Create role | ADMIN |
| GET | `/api/v1/api-keys` | My API keys | Bearer |
| POST | `/api/v1/api-keys` | Generate key | Bearer |
| POST | `/api/v1/api-keys/{id}/revoke` | Revoke key | Bearer |
| GET | `/api/v1/audit` | Audit logs | ADMIN |
| GET | `/api/v1/tenants` | List tenants | SUPER_ADMIN |

Full interactive docs: `http://localhost:8080/swagger-ui/index.html`

---

## 🔐 Security Architecture

```
Request
  │
  ├── RateLimitingFilter  (IP blacklist + login/API rate limits)
  │
  ├── ApiKeyAuthenticationFilter  (X-API-Key header → SHA-256 hash lookup)
  │
  ├── JwtAuthenticationFilter     (Authorization: Bearer → RS256 verify)
  │
  └── Spring Security
        └── @PreAuthorize("hasAuthority('USER_READ')")
```

### Password Policy
- Minimum 8 characters
- At least 1 uppercase, 1 lowercase, 1 digit, 1 special character
- Maximum age: 90 days

### Account Lockout
- Locks after **5 failed attempts**
- Lockout duration: **30 minutes**
- Email notification sent on lock

---

## 🗄️ Database Schema

```
tenants ─┬─ users ──────── user_roles ── roles ── role_permissions ── permissions
          │                    │
          ├─ api_keys          └── refresh_tokens
          │
          ├─ audit_logs
          └─ ip_rules
```

---

## ⚙️ Configuration Reference

| Property | Default | Description |
|---|---|---|
| `jwt.access-token-expiry-ms` | 900000 (15m) | Access token lifetime |
| `jwt.refresh-token-expiry-ms` | 604800000 (7d) | Refresh token lifetime |
| `app.security.account-lockout.max-failed-attempts` | 5 | Login failures before lock |
| `app.security.rate-limit.login-requests-per-minute` | 10 | Max login attempts/min/IP |
| `app.security.rate-limit.api-requests-per-minute` | 100 | Max API calls/min/key |
| `app.security.api-key.max-keys-per-user` | 10 | Max active API keys per user |

---

## 🧪 Tests

The project includes unit tests for Service, Controller, and Utility layers.

```bash
# Run all tests
mvn test

# Run specific tests
mvn test -Dtest=AuthServiceTest
mvn test -Dtest=AuthControllerTest
mvn test -Dtest=UserServiceTest

# Skip tests for faster build
mvn package -DskipTests
```

---

## 📧 Email (Notifications)

Configure SMTP in `application.yml`:
```yaml
spring:
  mail:
    host: smtp.mailtrap.io
    port: 587
    username: YOUR_USERNAME
    password: YOUR_PASSWORD
```

For local development, use [Mailtrap](https://mailtrap.io) (free tier).

---

## 📁 Project Structure

```
src/main/java/com/auth/
├── config/          # Security, JWT, Swagger, Async configs
├── controller/      # REST controllers
├── domain/          # JPA entities
├── dto/             # Request/Response DTOs
├── exception/       # ApiException + GlobalExceptionHandler
├── repository/      # Spring Data JPA repositories
├── security/        # JWT utils, filters, UserDetails
├── service/         # Business logic
└── util/            # HashUtils, RequestUtils

src/main/resources/
├── db/migration/    # Flyway SQL migrations (V1, V2)
├── keys/            # RSA keypair (gitignored)
└── application.yml  # Main configuration
```
