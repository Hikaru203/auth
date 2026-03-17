package com.auth.controller;

import com.auth.domain.AuditLog;
import com.auth.dto.response.PageResponse;
import com.auth.security.CustomUserDetails;
import com.auth.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Query audit log entries")
@SecurityRequirement(name = "Bearer")
public class AuditController {

    private final AuditLogService auditLogService;

    @GetMapping
    @Operation(summary = "Query audit logs for tenant with filtering (admin only)")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public ResponseEntity<PageResponse<AuditLog>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) java.time.Instant startDate,
            @RequestParam(required = false) java.time.Instant endDate,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Page<AuditLog> logs = auditLogService.searchLogs(
                currentUser.getTenantId(), username, action, statusCode, startDate, endDate,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PageResponse.from(logs));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my audit log entries")
    public ResponseEntity<PageResponse<AuditLog>> getMyLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Page<AuditLog> logs = auditLogService.getMyLogs(currentUser.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(PageResponse.from(logs));
    }
}
