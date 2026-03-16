package com.auth.controller;

import com.auth.dto.response.RoleDistributionResponse;
import com.auth.dto.response.TrafficStatsResponse;
import com.auth.security.CustomUserDetails;
import com.auth.service.AuditLogService;
import com.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Tag(name = "Statistics", description = "System metrics and analytics")
@SecurityRequirement(name = "Bearer")
public class StatisticsController {

    private final AuditLogService auditLogService;
    private final UserService userService;

    @GetMapping("/traffic")
    @Operation(summary = "Get authentication traffic stats")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public ResponseEntity<TrafficStatsResponse> getTrafficStats(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(auditLogService.getTrafficStats(currentUser.getTenantId(), days));
    }

    @GetMapping("/roles")
    @Operation(summary = "Get user role distribution")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<RoleDistributionResponse> getRoleDistribution(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(userService.getRoleDistribution(currentUser.getTenantId()));
    }
}
