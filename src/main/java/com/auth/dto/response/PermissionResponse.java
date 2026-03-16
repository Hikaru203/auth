package com.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PermissionResponse {
    private UUID id;
    private String name;
    private String module;
    private String action;
    private String description;
}
