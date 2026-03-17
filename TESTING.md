# 🧪 SecurityHub — The Ultimate Development & Verification Field Guide

This document is the absolute authority on interacting with, testing, and verifying the **SecurityHub Mainframe**. It is designed for security auditors, backend engineers, and QA automation specialists who require deep, programmatic control over the system's identity and access protocols.

---

## 📖 Tactical Table of Contents
1. [Core Testing Philosophy](#-core-testing-philosophy)
2. [Global Parameters & Environment](#-global-parameters--environment)
3. [Phase 1: Identity Access (The Auth Chain)](#-phase-1-identity-access-the-auth-chain)
4. [Phase 2: Registry Operations (Identity CRUD)](#-phase-2-registry-operations-identity-crud)
5. [Phase 3: Privilege Orchestration (RBAC/PBAC)](#-phase-3-privilege-orchestration-rbac-pbac)
6. [Phase 4: Programmatic Access (API Keys)](#-phase-4-programmatic-access-api-keys)
7. [Phase 5: Multi-tenant Surveillance (Audit Logs)](#-phase-5-multi-tenant-surveillance-audit-logs)
8. [Phase 6: MFA Induction & Enforcement](#-phase-6-mfa-induction--enforcement)
9. [Advanced Automation & Stress Testing](#-advanced-automation--stress-testing)
10. [Edge-Case Security Matrix](#-edge-case-security-matrix)
11. [Cross-Tenant Isolation Drills](#-cross-tenant-isolation-drills)
12. [Troubleshooting Intelligence](#-troubleshooting-intelligence)
13. [Verification Checklist](#-verification-checklist)

---

## 🏛️ Core Testing Philosophy

Security testing at SecurityHub is driven by the **Zero Trust Principle**. Every script in this document is designed to challenge the system's defensive filters (Rate Limiting, JWT Validation, MFA Enforcement, and Tenant Isolation). We recommend running these tests in a dedicated `STAGING` environment before promoting any security configuration changes to `PRODUCTION`.

---

## ⚙️ Global Parameters & Environment

Ensure your local variables are initialized before running the PowerShell automation scripts.

```powershell
$domain = "localhost:8080"
$api_v1 = "http://$domain/api/v1"
$tenant = "default"
$admin_user = "admin"
$admin_pass = "Admin@123"
```

---

## 🔑 Phase 1: Identity Access (The Auth Chain)

### 1.1 Secure Login (Obtain Bearer Tokens)
The login endpoint is the gateway to the system. It requires a `totpCode` for every request.

**PowerShell Logic:**
```powershell
$loginBody = @{
    tenantSlug = $tenant
    username = $admin_user
    password = $admin_pass
    totpCode = "000000" # Use 6-digit code if MFA is active
}

$authRes = Invoke-RestMethod -Uri "$api_v1/auth/login" -Method Post -Body ($loginBody | ConvertTo-Json) -ContentType "application/json"
$access_token = $authRes.accessToken
$refresh_token = $authRes.refreshToken

Write-Host "✅ ACCESS GRANTED. Sessions established." -ForegroundColor Green
```

### 1.2 Session Persistence (Token Refresh)
Verify that the system can extend sessions without re-authenticating credentials.

```powershell
$refreshBody = @{ refreshToken = $refresh_token }
$newTokens = Invoke-RestMethod -Uri "$api_v1/auth/refresh" -Method Post -Body ($refreshBody | ConvertTo-Json) -ContentType "application/json"
$access_token = $newTokens.accessToken
Write-Host "🔄 Token Synchronized."
```

### 1.3 Session Termination (Logout)
Ensure the refresh token is immediately invalidated in the server-side registry.

---

## 👤 Phase 2: Registry Operations (Identity CRUD)

### 2.1 Identity Discovery (Profile Me)
Retrieve the full object representing the current authenticated actor.

```powershell
$hdrs = @{ Authorization = "Bearer $access_token" }
Invoke-RestMethod -Uri "$api_v1/users/me" -Method Get -Headers $hdrs
```

### 2.2 Profile Synchronization (Partial Update)
Verify that identity metadata can be updated without impacting security credentials.

```powershell
$patch = @{
    firstName = "Security"
    lastName = "Officer"
    phone = "+1-555-SECURITY"
}
Invoke-RestMethod -Uri "$api_v1/users/me" -Method Put -Body ($patch | ConvertTo-Json) -ContentType "application/json" -Headers $hdrs
```

### 2.3 Identity Initialization (Create User)
Test the administrative path for creating new identities with pre-assigned roles.

```powershell
$newUser = @{
    tenantSlug = $tenant
    username = "field-agent-01"
    email = "agent01@security-hub.io"
    password = "User@456"
}
$res = Invoke-RestMethod -Uri "$api_v1/users" -Method Post -Body ($newUser | ConvertTo-Json) -ContentType "application/json" -Headers $hdrs
$newUserId = $res.id
```

---

## 🛡️ Phase 3: Privilege Orchestration (RBAC/PBAC)

### 3.1 Role Linkage
Verify that an identity can be promoted to a specific security role.

```powershell
$roleLink = @{ roleId = "UUID_OF_ROLE" }
Invoke-RestMethod -Uri "$api_v1/users/$newUserId/roles" -Method Post -Body ($roleLink | ConvertTo-Json) -ContentType "application/json" -Headers $hdrs
```

### 3.2 Permission Granularity
Test the mapping of atomic permissions to a role. This is critical for preventing "Permission Creep".

---

## 🗝️ Phase 4: Programmatic Access (API Keys)

### 4.1 Secret Key Generation
Verify that the system generates a high-entropy secret only once.

```powershell
$keyParams = @{ name = "Integration-Pipe-01"; expiresInDays = 30 }
$keyRes = Invoke-RestMethod -Uri "$api_v1/api-keys" -Method Post -Body ($keyParams | ConvertTo-Json) -ContentType "application/json" -Headers $hdrs
$secret_key = $keyRes.secretKey
```

### 4.2 Key-Based Authentication
Verify that headless systems can bypass JWT if a valid `X-API-Key` is provided.

```powershell
$keyHdr = @{ "X-API-Key" = $secret_key }
Invoke-RestMethod -Uri "$api_v1/users/me" -Method Get -Headers $keyHdr
```

---

## 📊 Phase 5: Multi-tenant Surveillance (Audit Logs)

### 5.1 Real-time Investigation
Retrieve the most recent security events with deep metadata.

```bash
curl -G "http://localhost:8080/api/v1/audit?size=10" \
  -H "Authorization: Bearer $access_token"
```

### 5.2 Filtered Forensics
Search for specific failure modes or actor activity.

```bash
# Investigation: All 401 (Unauthorized) attempts in the last hour
curl -G "http://localhost:8080/api/v1/audit" \
  -H "Authorization: Bearer $access_token" \
  -d "statusCode=401" \
  -d "action=LOGIN_FAILURE"
```

---

## 📱 Phase 6: MFA Induction & Enforcement

### 6.1 The MFA Handshake
1.  **Request Setup**: `POST /auth/2fa/setup` -> Receive Secret + QR string.
2.  **Verify & Enable**: Send a 6-digit TOTP from your app + the Secret to `POST /auth/2fa/verify`.
3.  **Validate State**: Verify that `users/me` now shows `is2faEnabled: true`.

---

## 🚀 Advanced Automation & Stress Testing

### Brute-Force Shield Verification
Test the **Bucket4j** rate-limiting filter. Run this loop; the server should return `429 Too Many Requests` after the 10th attempt.

```powershell
for ($i=1; $i -le 15; $i++) {
    try {
        Invoke-RestMethod -Uri "$api_v1/auth/login" -Method Post -Body ($loginBody | ConvertTo-Json) -ContentType "application/json"
        Write-Host "Attempt $i: PASSED"
    } catch {
        Write-Host "Attempt $i: BLOCKED ($($_.Exception.Response.StatusCode))" -ForegroundColor Red
    }
}
```

---

## 🔍 Edge-Case Security Matrix

| Scenario | Expected Outcome | Verification Step |
|---|---|---|
| Invalid JWT Signature | `401 Unauthorized` | Modify the last character of the `$access_token` and retry. |
| Expired Token | `401 Unauthorized` | Wait for 15 mins (or lower TTL in config) and retry. |
| Role Escalation Attempt | `403 Forbidden` | Try to call `/audit` with a `ROLE_USER` token. |
| Soft-Delete Check | `404 Not Found` | Delete a user, then try to fetch its ID. |
| Password Entropy | `400 Bad Request` | Try creating a user with password `"123"`. |

---

## 🏢 Cross-Tenant Isolation Drills

This is the most critical test for SaaS deployments.
1.  **Identity A (Tenant A)** logs in and gets a token.
2.  **Identity A** attempts to fetch a User ID from **Tenant B**.
3.  **Result**: The system must return `404 Not Found` or `403 Forbidden`, even if the ID exists in the database.

---

## 🆘 Troubleshooting Intelligence

- **SSL/TLS Errors**: SecurityHub requires HTTPS in production. If testing locally, ensure you are using `http` and not `https` unless a certificate is configured.
- **Clock Skew**: TOTP MFA relies on synchronized time. Ensure your server and mobile device are within 30 seconds of each other.
- **Database Deadlocks**: If running massive parallel tests, ensure your `DB_POOL_SIZE` is sufficient (min 10).

---

## ✅ Verification Checklist

- [ ] Successful Login with TOTP.
- [ ] JWT Signature Validation (RS256).
- [ ] Role and Permission correctly mapped in JPA.
- [ ] Audit log captured for every `/auth` endpoint.
- [ ] API Key provides scope-limited access.
- [ ] Rate limiting triggers on excessive failures.
- [ ] Multi-tenant data remains isolated.

---

**SecurityHub Testing Protocol** — *Verify. Harden. Deploy.*
