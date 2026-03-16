package com.auth.controller;

import com.auth.domain.Role;
import com.auth.dto.request.CreateRoleRequest;
import com.auth.dto.response.PermissionResponse;
import com.auth.dto.response.RoleResponse;
import com.auth.security.CustomUserDetails;
import com.auth.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "Manage roles and their permissions")
@SecurityRequirement(name = "Bearer")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "List all roles in tenant")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<List<RoleResponse>> listRoles(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(roleService.listRoles(user.getTenantId()).stream()
                .map(this::toResponse).toList());
    }

    @PostMapping
    @Operation(summary = "Create a new role")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public ResponseEntity<RoleResponse> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        Role role = roleService.createRole(user.getTenantId(), request.getName(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(role));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<RoleResponse> getRole(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(roleService.getRole(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable UUID id,
                                                    @RequestBody CreateRoleRequest request,
                                                    @AuthenticationPrincipal CustomUserDetails user) {
        Role role = roleService.updateRole(id, user.getTenantId(), request.getName(), request.getDescription());
        return ResponseEntity.ok(toResponse(role));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable UUID id,
                                                           @AuthenticationPrincipal CustomUserDetails user) {
        roleService.deleteRole(id, user.getTenantId());
        return ResponseEntity.ok(Map.of("message", "Role deleted"));
    }

    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public ResponseEntity<RoleResponse> addPermission(@PathVariable UUID id,
                                                       @RequestBody Map<String, String> body,
                                                       @AuthenticationPrincipal CustomUserDetails user) {
        UUID permId = UUID.fromString(body.get("permissionId"));
        Role role = roleService.addPermission(id, permId, user.getTenantId());
        return ResponseEntity.ok(toResponse(role));
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public ResponseEntity<RoleResponse> removePermission(@PathVariable UUID id,
                                                          @PathVariable UUID permissionId,
                                                          @AuthenticationPrincipal CustomUserDetails user) {
        Role role = roleService.removePermission(id, permissionId, user.getTenantId());
        return ResponseEntity.ok(toResponse(role));
    }

    private RoleResponse toResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .systemRole(role.isSystemRole())
                .permissions(role.getPermissions().stream().map(p ->
                        PermissionResponse.builder()
                                .id(p.getId())
                                .name(p.getName())
                                .module(p.getModule())
                                .action(p.getAction())
                                .description(p.getDescription())
                                .build()
                ).toList())
                .build();
    }
}
