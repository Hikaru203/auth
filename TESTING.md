# SecurityHub Development & Testing Guide

This guide provides technical blueprints for interacting with the SecurityHub API and verifying system integrity.

---

## 🔑 1. Security Initialization (Login)

**Endpoint:** `POST /api/v1/auth/login`

> [!IMPORTANT]
> The `totpCode` is mandatory for all accounts. Use `"000000"` if MFA is not yet configured for the identity.

**PowerShell Automation:**
```powershell
$payload = @{
    tenantSlug = "default"
    username = "admin"
    password = "Admin@123"
    totpCode = "000000"
}
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -Body ($payload | ConvertTo-Json) -ContentType "application/json"
$token = $response.accessToken
```

---

## 👤 2. Identity Operations

### Update Security Profile
**Endpoint:** `PUT /api/v1/users/{id}` (Auth Required)

```powershell
$headers = @{ Authorization = "Bearer $token" }
$update = @{
    firstName = "Security"
    lastName = "Officer"
    phone = "+1-555-0199"
}
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/users/me" -Method Put -Body ($update | ConvertTo-Json) -ContentType "application/json" -Headers $headers
```

### Identity Deactivation (Soft-Delete)
**Endpoint:** `DELETE /api/v1/users/{id}`

---

## 🛡️ 3. Access Control (RBAC)

### Role Management
**Endpoint:** `POST /api/v1/users/{id}/roles`
- Add a new authority link between an identity and a security role.

### Permission Synchronization
**Endpoint:** `POST /api/v1/roles/{id}/permissions`
- Update granular permission scopes for a specific role.

---

## 📊 4. Surveillance & Auditing

**Endpoint:** `GET /api/v1/audit`

**Advanced Filtering:**
```bash
# Filter by suspicious IP activity
curl -G http://localhost:8080/api/v1/audit \
  -H "Authorization: Bearer $token" \
  -d "ipAddress=192.168.1.1" \
  -d "action=LOGIN_FAILURE"
```

---

## 🗝️ 5. Programmatic Access (API Keys)

### Key Generation
**Endpoint:** `POST /api/v1/api-keys`
```powershell
$keyParams = @{ name = "Surveillance-Bot"; expiresInDays = 90 }
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/api-keys" -Method Post -Body ($keyParams | ConvertTo-Json) -ContentType "application/json" -Headers $headers
```

---

## 🛠️ Verification Protocols

1. **JPA Identity Stability**: Verify that role assignments are persistent across identity reloads (Fixed via stable `equals`/`hashCode`).
2. **2FA Enforcement**: Attempt login without `totpCode` and verify the system rejects the transaction.
3. **Notification Integrity**: In the dashboard, trigger an error (e.g., invalid password) and verify the `Security Breach` error card appears instead of a browser alert.
4. **Tenant Isolation**: Log in with a different `tenantSlug` and ensure data from the `default` tenant remains invisible.

---

## 🆘 Critical Support
- **Logs**: Monitor `target/auth-service.log` for real-time security events.
- **Troubleshooting**: If you encounter a `403 Forbidden`, ensure the identity has the `ADMIN` authority assigned in the dashboard or database.
