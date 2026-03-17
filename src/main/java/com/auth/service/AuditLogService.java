package com.auth.service;

import com.auth.domain.AuditLog;
import com.auth.dto.response.TrafficStatsResponse;
import com.auth.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, String entityId,
                    UUID tenantId, UUID userId, String username,
                    String ip, String userAgent, Integer statusCode,
                    String requestPath, Map<String, Object> metadata) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .tenantId(tenantId)
                    .userId(userId)
                    .username(username)
                    .ipAddress(ip)
                    .userAgent(userAgent)
                    .statusCode(statusCode)
                    .requestPath(requestPath)
                    .metadata(metadata)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save audit log for action {}: {}", action, e.getMessage());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String action, String entityType, UUID entityId, UUID userId, UUID tenantId, String description, Map<String, Object> metadata) {
        log(action, entityType, entityId != null ? entityId.toString() : null, tenantId, userId, null, null, null, null, null, metadata);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogin(UUID userId, String username, String tenantSlug, String ip, String userAgent) {
        log("LOGIN_SUCCESS", "USER", userId.toString(), null, userId, username, ip, userAgent, 200, "/api/v1/auth/login", null);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailedLogin(UUID userId, String username, String tenantSlug,
                               String ip, String userAgent, String reason) {
        log("LOGIN_FAILED", "USER", userId != null ? userId.toString() : "unknown",
                null, userId, username, ip, userAgent, 401,
                "/api/v1/auth/login", Map.of("reason", reason, "tenantSlug", tenantSlug));
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getLogs(UUID tenantId, Pageable pageable) {
        return auditLogRepository.findAllByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public TrafficStatsResponse getTrafficStats(UUID tenantId, int lastDays) {
        Instant startDate = Instant.now().minus(lastDays, ChronoUnit.DAYS);
        List<Object[]> results = auditLogRepository.findTrafficStats(tenantId, startDate);
        
        List<TrafficStatsResponse.TrafficDataPoint> points = results.stream()
                .map(r -> new TrafficStatsResponse.TrafficDataPoint(
                        (LocalDate) r[0],
                        (Long) r[1],
                        (Long) r[2]
                ))
                .collect(Collectors.toList());
                
        return TrafficStatsResponse.builder().points(points).build();
    }
    @Transactional(readOnly = true)
    public Page<AuditLog> searchLogs(UUID tenantId, String username, String action,
                                     Integer statusCode, Instant startDate, Instant endDate,
                                     Pageable pageable) {
        return auditLogRepository.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (username != null && !username.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (statusCode != null) {
                predicates.add(cb.equal(root.get("statusCode"), statusCode));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getMyLogs(UUID userId, Pageable pageable) {
        return auditLogRepository.findAllByUserId(userId, pageable);
    }
}
