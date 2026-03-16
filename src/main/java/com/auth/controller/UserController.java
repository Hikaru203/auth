package com.auth.controller;

import com.auth.domain.User;
import com.auth.dto.request.CreateUserRequest;
import com.auth.dto.request.UpdateUserRequest;
import com.auth.dto.response.PageResponse;
import com.auth.dto.response.UserResponse;
import com.auth.security.CustomUserDetails;
import com.auth.service.UserService;
import com.auth.service.TenantService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "CRUD operations for users within a tenant")
@SecurityRequirement(name = "Bearer")
public class UserController {

    private final UserService userService;
    private final TenantService tenantService;

    @GetMapping
    @Operation(summary = "List users (paginated)")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<PageResponse<UserResponse>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Page<User> users = userService.listUsers(currentUser.getTenantId(),
                PageRequest.of(page, size, Sort.by(sort).descending()));
        return ResponseEntity.ok(PageResponse.from(users.map(this::toResponse)));
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        UUID tenantId = tenantService.getBySlug(request.getTenantSlug()).getId();
        User user = userService.createUser(tenantId, request.getUsername(), request.getEmail(),
                request.getPassword(), request.getFirstName(), request.getLastName(), request.getPhone());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        User user = userService.getUser(id, currentUser.getTenantId());
        return ResponseEntity.ok(toResponse(user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        User user = userService.updateUser(id, currentUser.getTenantId(),
                request.getFirstName(), request.getLastName(), request.getPhone());
        return ResponseEntity.ok(toResponse(user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate user (soft delete)")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        userService.deleteUser(id, currentUser.getTenantId());
        return ResponseEntity.ok(Map.of("message", "User deactivated"));
    }

    @PostMapping("/{id}/lock")
    @Operation(summary = "Lock user account")
    @PreAuthorize("hasAuthority('USER_LOCK')")
    public ResponseEntity<Map<String, String>> lockUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        userService.lockUser(id, currentUser.getTenantId());
        return ResponseEntity.ok(Map.of("message", "User locked"));
    }

    @PostMapping("/{id}/unlock")
    @Operation(summary = "Unlock user account")
    @PreAuthorize("hasAuthority('USER_LOCK')")
    public ResponseEntity<Map<String, String>> unlockUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        userService.unlockUser(id, currentUser.getTenantId());
        return ResponseEntity.ok(Map.of("message", "User unlocked"));
    }

    @PostMapping("/{id}/roles")
    @Operation(summary = "Assign role to user")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public ResponseEntity<Map<String, String>> assignRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        UUID roleId = UUID.fromString(body.get("roleId"));
        userService.assignRole(id, currentUser.getTenantId(), roleId);
        return ResponseEntity.ok(Map.of("message", "Role assigned"));
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @Operation(summary = "Remove role from user")
    @PreAuthorize("hasAuthority('ROLE_WRITE')")
    public ResponseEntity<Map<String, String>> removeRole(
            @PathVariable UUID id,
            @PathVariable UUID roleId,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        userService.removeRole(id, currentUser.getTenantId(), roleId);
        return ResponseEntity.ok(Map.of("message", "Role removed"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal CustomUserDetails currentUser) {
        User user = userService.getUser(currentUser.getId(), currentUser.getTenantId());
        return ResponseEntity.ok(toResponse(user));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .tenantId(user.getTenant().getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .totpEnabled(user.isTotpEnabled())
                .emailVerified(user.isEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .roles(user.getRoles().stream().map(r -> r.getName()).toList())
                .build();
    }
}
