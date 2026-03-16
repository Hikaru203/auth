package com.auth.controller;

import com.auth.domain.Tenant;
import com.auth.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant Management", description = "Super-admin tenant operations")
@SecurityRequirement(name = "Bearer")
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @Operation(summary = "List all tenants (super-admin only)")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Tenant>> listTenants() {
        return ResponseEntity.ok(tenantService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Tenant> createTenant(@RequestBody Map<String, String> body) {
        Tenant tenant = tenantService.create(body.get("name"), body.get("slug"));
        return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Tenant> getTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Tenant> updateTenant(@PathVariable UUID id,
                                                @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return ResponseEntity.ok(tenantService.update(id, name, active));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteTenant(@PathVariable UUID id) {
        tenantService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Tenant deleted"));
    }
}
