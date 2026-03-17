package com.auth.repository;

import com.auth.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Role> findByNameAndTenantId(String name, UUID tenantId);

    @Query("SELECT r FROM Role r WHERE r.tenant.id = :tenantId")
    org.springframework.data.domain.Page<Role> findAllByTenantId(@Param("tenantId") UUID tenantId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT r FROM Role r JOIN FETCH r.permissions WHERE r.tenant.id = :tenantId")
    List<Role> findAllByTenantIdWithPermissions(@Param("tenantId") UUID tenantId);

    boolean existsByNameAndTenantId(String name, UUID tenantId);
}
