package com.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ApiKeyResponse {
    private UUID id;
    private String name;
    private String keyPrefix;   // Only prefix shown — never the full key
    private String rawKey;      // Only present at creation time
    private List<String> scopes;
    private boolean revoked;
    private Instant expiresAt;
    private Instant lastUsedAt;
    private Instant createdAt;
}
