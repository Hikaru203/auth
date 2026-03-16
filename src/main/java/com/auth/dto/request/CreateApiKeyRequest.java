package com.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateApiKeyRequest {

    @NotBlank(message = "API key name is required")
    private String name;

    private String[] scopes;
    private Integer expiryDays;
}
