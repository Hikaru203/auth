# SecurityHub — Enterprise Auth Service

A production-grade **Spring Boot 3.2 / Java 21** backend and **Vite / Vanilla JS** premium dashboard for authentication, identity management, and security orchestration.

---

## ✨ System Capabilities

| Capability | Implementation |
|---|---|
| **Identity Protection** | JWT (RS256) + Mandatory MFA Enforcement |
| **Identity Management** | Full CRUD + Secure Profile Editing (Phone, Names) |
| **Access Control** | RBAC/PBAC with JPA identity stability (equals/hashCode) |
| **MFA Orchestration** | TOTP (Google Authenticator) with Setup/Verification Flow |
| **Audit Compliance** | Multi-tenant Async Logging with IP & Geo-tracing |
| **Premium UI** | Glassmorphism Dashboard + Custom Notification System |
| **Resilience** | Rate Limiting (Bucket4j) + Soft-Delete Safety |

---

## 🚀 Deployment Guide

### Prerequisites
- **Java 21** (Required for modern security features)
- **Node.js 18+** (Vite development environment)
- **PostgreSQL 15+**
- **OpenSSL** (For RSA keypair generation)

### 1. Repository Setup
```bash
git clone https://github.com/Hikaru203/auth.git
cd auth
```

### 2. Environment Configuration
1. Initialize your environment:
   ```bash
   cp .env.example .env
   ```
2. Configure your PostgreSQL connection and SMTP settings in `.env`.

### 3. Security Hardening (Keys)
You must generate private/public RSA keys for JWT signing:
- **Windows**: `generate-keys.bat`
- **Linux/Unix**: `chmod +x generate-keys.sh && ./generate-keys.sh`

### 4. System Launch
#### Security Backend
```bash
mvn spring-boot:run
```
- **API Mainframe**: `http://localhost:8080/api/v1`
- **OpenAPI Schema**: `http://localhost:8080/v3/api-docs`

#### Security Dashboard
```bash
cd dashboard-web
npm install
npm run dev
```
- **Interface**: `http://localhost:5173`

---

## 🔒 Security Operations

### Identity Initialization
The system enforces a strict identity flow. New identities can be initialized with assigned roles immediately.

### MFA Flow
1. **Setup**: Call `/auth/2fa/setup` to receive a QR code.
2. **Verification**: Confirm with a 6-digit code to activate protection.
3. **Enforcement**: Once enabled, the system requires a valid TOTP code for every login session.

### Premium Notifications
Standard browser `alert()` and `confirm()` have been deprecated. The system now uses a **Success/Error/Warning notification engine** with premium transitions and glassmorphism styling.

---

## 📋 Standard Protocol

| Context | Default Identity |
|---|---|
| **Tenant** | `default` |
| **Username** | `admin` |
| **Credential** | `Admin@123` |

---

## 📊 Identity Hierarchy
```
Mainframe ──┬─ Tenants (Enterprise Boundaries)
            │
            ├─ Identities (Users) ── Roles ── Permissions
            │
            ├─ Security Keys (API Tokens)
            │
            └─ Audit Vault (Security Events)
```

---

## 🧪 Verification & Testing
Detailed API interaction guides, PowerShell automation scripts, and cURL examples for manual security testing are available in:
👉 **[TESTING.md](./TESTING.md)**

---

## 📁 Intelligence Structure
```
src/main/java/com/auth/
├── config/          # Enterprise Security Configuration
├── controller/      # API Interface Layer
├── domain/          # Identity Entities (JPA)
├── security/        # JWT, Filters, & MFA Logic
├── service/         # Security Business Logic
└── util/            # Cryptographic Utilities
```

