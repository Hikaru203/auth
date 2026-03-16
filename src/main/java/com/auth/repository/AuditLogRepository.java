package com.auth.repository;

import com.auth.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>,
        JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findAllByUserId(UUID userId, Pageable pageable);

    Page<AuditLog> findAllByTenantId(UUID tenantId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT CAST(a.createdAt AS LocalDate) as date, " +
           "SUM(CASE WHEN a.statusCode < 400 THEN 1 ELSE 0 END) as success, " +
           "SUM(CASE WHEN a.statusCode >= 400 THEN 1 ELSE 0 END) as failure " +
           "FROM AuditLog a " +
           "WHERE a.tenantId = :tenantId AND a.createdAt >= :startDate " +
           "GROUP BY CAST(a.createdAt AS LocalDate) " +
           "ORDER BY CAST(a.createdAt AS LocalDate) ASC")
    java.util.List<Object[]> findTrafficStats(@Param("tenantId") UUID tenantId, 
                                            @Param("startDate") java.time.Instant startDate);
}
