package com.auth.repository;

import com.auth.domain.ApiKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    Page<ApiKey> findAllByUserId(UUID userId, Pageable pageable);

    Page<ApiKey> findAllByUserIdAndRevokedFalse(UUID userId, Pageable pageable);

    int countByUserIdAndRevokedFalse(UUID userId);

    @Query("SELECT k FROM ApiKey k WHERE k.revoked = false AND k.expiresAt < :now")
    List<ApiKey> findExpiredKeys(@Param("now") Instant now);
}
