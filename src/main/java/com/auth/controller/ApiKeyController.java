package com.auth.controller;

import com.auth.domain.ApiKey;
import com.auth.dto.request.CreateApiKeyRequest;
import com.auth.dto.response.ApiKeyResponse;
import com.auth.dto.response.PageResponse;
import com.auth.security.CustomUserDetails;
import com.auth.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Key Management", description = "Manage API keys for programmatic access")
@SecurityRequirement(name = "Bearer")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    @Operation(summary = "List my API keys")
    public ResponseEntity<PageResponse<ApiKeyResponse>> listKeys(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Page<ApiKey> keys = apiKeyService.listKeys(currentUser.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PageResponse.from(keys.map(k -> toResponse(k, null))));
    }

    @PostMapping
    @Operation(summary = "Generate a new API key",
               description = "The raw key is returned ONLY once. Store it securely.")
    public ResponseEntity<ApiKeyResponse> generateKey(
            @Valid @RequestBody CreateApiKeyRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ApiKeyService.ApiKeyCreationResult result = apiKeyService.generateApiKey(
                currentUser.getId(), currentUser.getTenantId(),
                request.getName(), request.getScopes(), request.getExpiryDays());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(result.apiKey(), result.rawKey()));
    }

    @PostMapping("/{id}/revoke")
    @Operation(summary = "Revoke an API key")
    public ResponseEntity<Map<String, String>> revokeKey(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        String reason = body != null ? body.getOrDefault("reason", "Manually revoked") : "Manually revoked";
        apiKeyService.revokeKey(id, currentUser.getId(), currentUser.getTenantId(), reason);
        return ResponseEntity.ok(Map.of("message", "API key revoked"));
    }

    @PostMapping("/{id}/refresh")
    @Operation(summary = "Refresh an API key (revoke current, generate new)")
    public ResponseEntity<ApiKeyResponse> refreshKey(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        ApiKeyService.ApiKeyCreationResult result = apiKeyService.refreshKey(
                id, currentUser.getId(), currentUser.getTenantId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(result.apiKey(), result.rawKey()));
    }

    private ApiKeyResponse toResponse(ApiKey key, String rawKey) {
        return ApiKeyResponse.builder()
                .id(key.getId())
                .name(key.getName())
                .keyPrefix(key.getKeyPrefix())
                .rawKey(rawKey)
                .scopes(key.getScopes() != null ? Arrays.asList(key.getScopes()) : java.util.List.of())
                .revoked(key.isRevoked())
                .expiresAt(key.getExpiresAt())
                .lastUsedAt(key.getLastUsedAt())
                .createdAt(key.getCreatedAt())
                .build();
    }
}
