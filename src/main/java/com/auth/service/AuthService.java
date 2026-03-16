package com.auth.service;

import com.auth.config.SecurityProperties;
import com.auth.domain.*;
import com.auth.exception.ApiException;
import com.auth.repository.*;
import com.auth.security.JwtUtils;
import com.auth.util.HashUtils;
import com.auth.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SecurityProperties securityProperties;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final TwoFactorService twoFactorService;

    @Transactional
    public LoginResult login(String tenantSlug, String username, String password,
                             String totpCode, HttpServletRequest request) {

        String ip = RequestUtils.getClientIp(request);
        String userAgent = RequestUtils.getUserAgent(request);

        // Check IP blacklist (basic implementation)
        Tenant tenant = tenantRepository.findBySlug(tenantSlug)
                .orElseThrow(() -> ApiException.notFound("Tenant not found: " + tenantSlug));

        User user = userRepository.findByTenantSlugAndUsername(tenantSlug, username)
                .orElseGet(() -> userRepository.findByTenantSlugAndEmail(tenantSlug, username)
                        .orElse(null));

        if (user == null) {
            auditLogService.logFailedLogin(null, username, tenantSlug, ip, userAgent, "USER_NOT_FOUND");
            throw ApiException.unauthorized("Invalid credentials");
        }

        // Check lockout
        if (user.isLocked()) {
            auditLogService.logFailedLogin(user.getId(), username, tenantSlug, ip, userAgent, "ACCOUNT_LOCKED");
            throw ApiException.unauthorized("Account is locked. Try again later.");
        }

        if (!user.isActive()) {
            throw ApiException.unauthorized("Account is inactive or suspended.");
        }

        // Validate password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            handleFailedLogin(user, ip, userAgent, tenantSlug);
            throw ApiException.unauthorized("Invalid credentials");
        }

        // Validate 2FA if enabled
        if (user.isTotpEnabled()) {
            if (totpCode == null || totpCode.isBlank()) {
                throw ApiException.badRequest("2FA code required");
            }
            if (!twoFactorService.verifyCode(user.getTotpSecret(), totpCode)) {
                auditLogService.logFailedLogin(user.getId(), username, tenantSlug, ip, userAgent, "INVALID_2FA");
                throw ApiException.unauthorized("Invalid 2FA code");
            }
        }

        // Success — reset failed attempts
        user.resetFailedAttempts();
        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(ip);
        userRepository.save(user);

        // Build token data
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        List<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .toList();

        String accessToken = jwtUtils.generateAccessToken(
                user.getId(), user.getUsername(), tenant.getId().toString(), roles, permissions);
        String rawRefreshToken = jwtUtils.generateRefreshToken(user.getId());

        // Save refresh token hash
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(HashUtils.sha256(rawRefreshToken))
                .ipAddress(ip)
                .deviceInfo(userAgent)
                .expiresAt(Instant.now().plusMillis(
                        securityProperties.getApiKey().getDefaultExpiryDays() * 24L * 60 * 60 * 1000))
                .build();
        refreshTokenRepository.save(refreshToken);

        auditLogService.logLogin(user.getId(), username, tenantSlug, ip, userAgent);

        return new LoginResult(accessToken, rawRefreshToken, user);
    }

    @Transactional
    public TokenPair refreshToken(String rawRefreshToken) {
        String hash = HashUtils.sha256(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));

        if (!token.isValid()) {
            throw ApiException.unauthorized("Refresh token expired or revoked");
        }

        User user = token.getUser();

        // Rotate: revoke old token
        token.setRevoked(true);
        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);

        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        List<String> permissions = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getName)
                .distinct()
                .toList();

        String newAccessToken = jwtUtils.generateAccessToken(
                user.getId(), user.getUsername(),
                user.getTenant().getId().toString(), roles, permissions);
        String newRawRefreshToken = jwtUtils.generateRefreshToken(user.getId());

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(HashUtils.sha256(newRawRefreshToken))
                .ipAddress(token.getIpAddress())
                .deviceInfo(token.getDeviceInfo())
                .expiresAt(Instant.now().plusMillis(
                        securityProperties.getApiKey().getDefaultExpiryDays() * 24L * 60 * 60 * 1000))
                .build();
        refreshTokenRepository.save(newRefreshToken);

        return new TokenPair(newAccessToken, newRawRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        String hash = HashUtils.sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void initiatePasswordReset(String tenantSlug, String email) {
        userRepository.findByTenantSlugAndEmail(tenantSlug, email).ifPresent(user -> {
            // Invalidate old tokens
            passwordResetTokenRepository.invalidateAllByUserId(user.getId());

            String rawToken = HashUtils.generateSecureToken(32);
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(HashUtils.sha256(rawToken))
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();
            passwordResetTokenRepository.save(resetToken);

            notificationService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), rawToken);
        });
        // Always return OK to prevent user enumeration
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String hash = HashUtils.sha256(rawToken);
        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset token"));

        if (!token.isValid()) {
            throw ApiException.badRequest("Reset token has expired or already been used");
        }

        User user = token.getUser();
        user.setPasswordHash(hashPassword(newPassword));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        token.setUsed(true);
        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());

        auditLogService.log("PASSWORD_RESET", "USER", user.getId().toString(),
                user.getTenant().getId(), user.getId(), user.getUsername(),
                null, null, 200, null, null);
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }

        user.setPasswordHash(hashPassword(newPassword));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        // Revoke all refresh tokens after password change
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());

        auditLogService.log("PASSWORD_CHANGE", "USER", userId.toString(),
                user.getTenant().getId(), userId, user.getUsername(),
                null, null, 200, null, null);
    }

    private void handleFailedLogin(User user, String ip, String userAgent, String tenantSlug) {
        user.incrementFailedAttempts();
        int maxAttempts = securityProperties.getAccountLockout().getMaxFailedAttempts();
        if (user.getFailedAttempts() >= maxAttempts) {
            int lockoutMinutes = securityProperties.getAccountLockout().getLockoutDurationMinutes();
            user.setLockedUntil(Instant.now().plus(lockoutMinutes, ChronoUnit.MINUTES));
            user.setStatus(UserStatus.LOCKED);
            userRepository.save(user);
            notificationService.sendAccountLockedEmail(user.getEmail(), user.getFullName());
            auditLogService.logFailedLogin(user.getId(), user.getUsername(), tenantSlug,
                    ip, userAgent, "MAX_ATTEMPTS_REACHED");
        } else {
            userRepository.save(user);
            auditLogService.logFailedLogin(user.getId(), user.getUsername(), tenantSlug,
                    ip, userAgent, "INVALID_PASSWORD");
        }
    }

    private String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    // DTO records
    public record LoginResult(String accessToken, String refreshToken, User user) {}
    public record TokenPair(String accessToken, String refreshToken) {}
}
