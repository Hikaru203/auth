package com.auth.repository;

import com.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameAndTenantSlug(String username, @Param("slug") String tenantSlug);

    @Query("SELECT u FROM User u WHERE u.tenant.slug = :tenantSlug AND u.username = :username")
    Optional<User> findByTenantSlugAndUsername(@Param("tenantSlug") String tenantSlug,
                                               @Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.tenant.slug = :tenantSlug AND u.email = :email")
    Optional<User> findByTenantSlugAndEmail(@Param("tenantSlug") String tenantSlug,
                                            @Param("email") String email);

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId")
    Page<User> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    boolean existsByTenantIdAndUsername(UUID tenantId, String username);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    @Query("SELECT u FROM User u JOIN FETCH u.roles r JOIN FETCH r.permissions WHERE u.id = :id")
    Optional<User> findByIdWithRolesAndPermissions(@Param("id") UUID id);

    @Query("SELECT r.name, COUNT(u) FROM User u JOIN u.roles r WHERE u.tenant.id = :tenantId GROUP BY r.name")
    java.util.List<Object[]> countUsersByRole(@Param("tenantId") UUID tenantId);
}
