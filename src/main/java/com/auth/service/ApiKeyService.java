package com.auth.service;

import com.auth.config.SecurityProperties;
import com.auth.domain.ApiKey;
import com.auth.domain.User;
import com.auth.domain.Tenant;
import com.auth.exception.ApiException;
import com.auth.repository.ApiKeyRepository;
import com.auth.repository.UserRepository;
import com.auth.repository.TenantRepository;
import com.auth.util.HashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final SecurityProperties securityProperties;
    private final AuditLogService auditLogService;

    public record ApiKeyCreationResult(ApiKey apiKey, String rawKey) {}

    @Transactional
    public ApiKeyCreationResult generateApiKey(UUID userId, UUID tenantId, String name, String[] scopes, Integer expiryDays) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("Tenant not found"));

        if (apiKeyRepository.countByUserIdAndRevokedFalse(userId) >= securityProperties.getApiKey().getMaxKeysPerUser()) {
            throw ApiException.badRequest("Maximum active API keys reached for this user");
        }

        String rawKey = HashUtils.generateApiKey();
        String hash = HashUtils.sha256(rawKey);
        String prefix = HashUtils.extractPrefix(rawKey);

        Instant expiresAt = null;
        int days = expiryDays != null ? expiryDays : securityProperties.getApiKey().getDefaultExpiryDays();
        if (days > 0) {
            expiresAt = Instant.now().plus(days, ChronoUnit.DAYS);
        }

        ApiKey apiKey = ApiKey.builder()
                .tenant(tenant)
                .user(user)
                .name(name)
                .keyHash(hash)
                .keyPrefix(prefix)
                .scopes(scopes != null ? scopes : new String[0])
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        apiKey = apiKeyRepository.save(apiKey);
        auditLogService.logAction("CREATE_API_KEY", "ApiKey", apiKey.getId(), userId, tenantId, "Created API key: " + name, null);
        
        return new ApiKeyCreationResult(apiKey, rawKey);
    }

    public Page<ApiKey> listKeys(UUID userId, Pageable pageable) {
        return apiKeyRepository.findAllByUserIdAndRevokedFalse(userId, pageable);
    }

    @Transactional
    public void revokeKey(UUID keyId, UUID userId, UUID tenantId, String reason) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> ApiException.notFound("API key not found"));

        if (!apiKey.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("You do not own this API key");
        }

        apiKey.setRevoked(true);
        apiKey.setRevokedReason(reason);
        apiKeyRepository.save(apiKey);
        
        auditLogService.logAction("REVOKE_API_KEY", "ApiKey", keyId, userId, tenantId, "Revoked API key: " + apiKey.getName(), null);
    }

    @Transactional
    public ApiKeyCreationResult refreshKey(UUID keyId, UUID userId, UUID tenantId) {
        ApiKey oldKey = apiKeyRepository.findById(keyId)
                .orElseThrow(() -> ApiException.notFound("API key not found"));

        if (!oldKey.getUser().getId().equals(userId)) {
            throw ApiException.forbidden("You do not own this API key");
        }

        revokeKey(keyId, userId, tenantId, "Refreshed");
        
        return generateApiKey(userId, tenantId, oldKey.getName(), oldKey.getScopes(), null);
    }

    public Optional<ApiKey> validateKey(String rawKey) {
        String hash = HashUtils.sha256(rawKey);
        return apiKeyRepository.findByKeyHash(hash)
                .filter(ApiKey::isValid);
    }
}
