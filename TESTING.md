# 🧪 SecurityHub Intelligence & Testing Protocols

This document serves as the tactical field guide for verifying the SecurityHub mainframe. It contains exhaustive API blueprints, automation scripts, and edge-case verification protocols.

---

## 🛠️ Global Parameters
- **Base Host**: `http://localhost:8080/api/v1`
- **Auth Scheme**: JWT Bearer + X-API-Key

---

## 🔑 1. Phase One: Identity Access (Auth)

### Unified Login Protocol
All identities must provide a `totpCode` (use `"000000"` if MFA is not yet active).

**PowerShell Automation:**
```powershell
$credentials = @{
    tenantSlug = "default"
    username = "admin"
    password = "Admin@123"
    totpCode = "000000"
}
$authData = Invoke-RestMethod -Uri "$host/auth/login" -Method Post -Body ($credentials | ConvertTo-Json) -ContentType "application/json"
$access_token = $authData.accessToken
$refresh_token = $authData.refreshToken

Write-Host "✅ Authentication Secure. Access Token synthesized." -ForegroundColor Green
```

### Token Lifecycle Management
- **Refresh**: Interchange the expired access token with the refresh token.
- **Logout**: Blacklist the current refresh token from the active session pool.

---

## 🛡️ 2. Phase Two: Identity Management (Users)

### Profile Synthesis & Identification
Retrieve and verify the current identity structure.

```powershell
$headers = @{ Authorization = "Bearer $access_token" }
Invoke-RestMethod -Uri "$host/users/me" -Method Get -Headers $headers
```

### Profile Synchronization (Updating)
Modify non-immutable identity fields (FirstName, LastName, Phone).

```powershell
$profileUpdate = @{ firstName = "Security"; phone = "+1-555-9000" }
Invoke-RestMethod -Uri "$host/users/me" -Method Put -Body ($profileUpdate | ConvertTo-Json) -ContentType "application/json" -Headers $headers
```

### Administrative Listing
List all identities within the current tenant boundary with pagination support.

```bash
curl -X GET "http://localhost:8080/api/v1/users?page=0&size=20" \
  -H "Authorization: Bearer $access_token"
```

---

## 🗝️ 3. Phase Three: Privilege Orchestration (RBAC)

### Role Assignment
Link a security role identity to a user account.

**Endpoint**: `POST /users/{id}/roles`
- **Payload**: `{ "roleId": "ROLE_ID_HERE" }`

### Permission Logic Verification
Grant or revoke atomic permissions to/from a role.

**Endpoint**: `POST /roles/{id}/permissions`
- **Payload**: `{ "permissionId": "PERM_ID_HERE" }`

---

## 📊 4. Phase Four: Surveillance (Audit Logs)

The Audit Engine records every high-value transaction. Use the following filters for deep investigation.

### Investigating Failure Patterns
Search for blocked login attempts from specific origins.

```bash
curl -G "http://localhost:8080/api/v1/audit" \
  -H "Authorization: Bearer $access_token" \
  -d "action=LOGIN_FAILURE" \
  -d "statusCode=401"
```

### Tracing Identity Actions
Track all modifications performed by a specific username.

```powershell
Invoke-RestMethod -Uri "$host/audit?username=admin&size=50" -Method Get -Headers $headers
```

---

## 🚀 5. Advanced Automation (Stress Testing)

### Login Rate-Limit Breach Test
Run this PowerShell loop to verify the **Bucket4j** rate-limiting implementation (expecting 429 status).

```powershell
foreach ($i in 1..15) {
    try {
        Invoke-RestMethod -Uri "$host/auth/login" -Method Post -Body ($credentials | ConvertTo-Json) -ContentType "application/json"
        Write-Host "Attempt $i: Allowed"
    } catch {
        Write-Host "Attempt $i: Blocked ($($_.Exception.Response.StatusCode))" -ForegroundColor Red
    }
}
```

---

## 📦 6. API Key Intelligence (Programmatic Access)

API Keys allow headless systems to interact with the mainframe.

1. **Issue Key**: `POST /api-keys` (Provide `name` and `expiresInDays`).
2. **Verify Header**: Send `X-API-Key` with the secret received only once during generation.
3. **Revoke Key**: `POST /api-keys/{id}/revoke` to immediately sever access.

---

## 🔍 Edge-Case Verification Checklist

- [ ] **MFA Bypass Denied**: Attempt login with empty `totpCode` for an MFA-enabled user.
- [ ] **Tenant Crosstalk**: Verify that `Tenant A` cannot see identities from `Tenant B`.
- [ ] **Stale Tokens**: Attempt to use an access token after calling `/auth/logout`.
- [ ] **Permission Escalation**: Verify a `ROLE_USER` cannot access `/api/v1/audit`.
- [ ] **Database Integrity**: Verify `equals/hashCode` stability by updating a role and checking if the user-to-role relation remains intact.

---

## 🆘 Troubleshooting Intelligence
- **Connection Refused**: Verify the PostgreSQL service is active and `application.yml` points to the correct port.
- **Key Rejected**: Ensure the RSA keypair was generated using the provided scripts and resides in the deployment path.
- **JSON Error**: Ensure your request headers include `Content-Type: application/json`.
