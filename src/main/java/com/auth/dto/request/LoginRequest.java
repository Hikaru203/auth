package com.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Tenant slug is required")
    private String tenantSlug;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "TOTP code is required")
    @io.swagger.v3.oas.annotations.media.Schema(description = "TOTP code for 2FA", requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED)
    private String totpCode;
}
