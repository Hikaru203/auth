package com.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private UUID tenantId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String status;
    private boolean totpEnabled;
    private boolean emailVerified;
    private Instant lastLoginAt;
    private Instant createdAt;
    private java.util.List<String> roles;
}
