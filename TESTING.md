# API Testing Guide

This document provides a comprehensive set of examples to test all major API endpoints in the Auth Service.

> [!NOTE]
> All examples assume the server is running on `http://localhost:8080`.
> For PowerShell users, we recommend using `Invoke-RestMethod` to avoid complex escaping of JSON strings.

---

## 🔑 1. Authentication

### Login (Obtain Tokens)
**Endpoint:** `POST /api/v1/auth/login`

> [!IMPORTANT]
> The `totpCode` field is now **mandatory**. If you don't have 2FA enabled, use `"000000"`.

**PowerShell:**
```powershell
$body = @{
    tenantSlug = "default"
    username = "admin"
    password = "Admin@123"
    totpCode = "000000"
}
$auth = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -Body ($body | ConvertTo-Json) -ContentType "application/json"
$accessToken = $auth.accessToken
$refreshToken = $auth.refreshToken
$auth
```

**curl (bash/cmd):**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"tenantSlug":"default","username":"admin","password":"Admin@123","totpCode":"000000"}'
```

### Refresh Access Token
**Endpoint:** `POST /api/v1/auth/refresh`

**PowerShell:**
```powershell
$body = @{ refreshToken = $refreshToken }
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/refresh" -Method Post -Body ($body | ConvertTo-Json) -ContentType "application/json"
```

### Logout (Revoke Refresh Token)
**Endpoint:** `POST /api/v1/auth/logout`

**PowerShell:**
```powershell
$body = @{ refreshToken = $refreshToken }
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/logout" -Method Post -Body ($body | ConvertTo-Json) -ContentType "application/json"
```

---

## 👤 2. User Management

> [!TIP]
> Use the `$accessToken` obtained from the login step in the headers.

### Get Current User Profile (Me)
**Endpoint:** `GET /api/v1/users/me`

**PowerShell:**
```powershell
$headers = @{ Authorization = "Bearer $accessToken" }
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/users/me" -Method Get -Headers $headers
```

### List All Users (ADMIN)
**Endpoint:** `GET /api/v1/users?page=0&size=10`

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/users" -Method Get -Headers $headers
```

### Create a New User (ADMIN)
**Endpoint:** `POST /api/v1/users`

**PowerShell:**
```powershell
$userBody = @{
    tenantSlug = "default"
    username = "testuser"
    email = "test@example.local"
    password = "User@123"
    firstName = "Test"
    lastName = "User"
}
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/users" -Method Post -Body ($userBody | ConvertTo-Json) -ContentType "application/json" -Headers $headers
```

---

## 🗝️ 3. API Key Management

### Create an API Key
**Endpoint:** `POST /api/v1/api-keys`

**PowerShell:**
```powershell
$keyBody = @{
    name = "My Development Key"
    scopes = @("USER_READ", "AUDIT_READ")
    expiryDays = 30
}
$newKey = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/api-keys" -Method Post -Body ($keyBody | ConvertTo-Json) -ContentType "application/json" -Headers $headers
$apiKey = $newKey.apiKey
$newKey
```

### Use an API Key
**Endpoint:** Any protected endpoint with `X-API-Key` header.

**PowerShell:**
```powershell
$keyHeaders = @{ "X-API-Key" = $apiKey }
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/users/me" -Method Get -Headers $keyHeaders
```

---

## 🛡️ 4. 2FA Setup Flow

1. **Setup**: `POST /api/v1/auth/2fa/setup` (returns secret + QR link)
2. **Verify**: `POST /api/v1/auth/2fa/verify` (send 6-digit code + secret to enable)

---

## 📊 5. Audit Logs (ADMIN)

**Endpoint:** `GET /api/v1/audit`

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/audit" -Method Get -Headers $headers
```

---

## 🛠️ Troubleshooting

- **401 Unauthorized**: Check if your token has expired. Re-login or refresh.
- **403 Forbidden**: Your user doesn't have the required Permission (e.g., `USER_READ`).
- **500 Error**: Check the server logs at `logs/auth-service.log` for details.
