package com.auth.service;

import com.auth.config.SecurityProperties;
import com.auth.domain.*;
import com.auth.exception.ApiException;
import com.auth.repository.*;
import com.auth.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock TenantRepository tenantRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtils jwtUtils;
    private SecurityProperties securityProperties = new SecurityProperties();
    @Mock AuditLogService auditLogService;
    @Mock NotificationService notificationService;
    @Mock TwoFactorService twoFactorService;
    @Mock HttpServletRequest httpRequest;

    AuthService authService;

    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, refreshTokenRepository, passwordResetTokenRepository,
                tenantRepository, passwordEncoder, jwtUtils, securityProperties,
                auditLogService, notificationService, twoFactorService
        );
        tenant = Tenant.builder().name("Default").slug("default").active(true).build();
        tenant.setId(UUID.randomUUID());
        user = User.builder()
                .tenant(tenant)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashed")
                .status(UserStatus.ACTIVE)
                .failedAttempts(0)
                .totpEnabled(false)
                .roles(new HashSet<>())
                .build();
        user.setId(UUID.randomUUID());

        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");
    }

    @Test
    void login_success() {
        when(tenantRepository.findBySlug("default")).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantSlugAndUsername("default", "testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(jwtUtils.generateAccessToken(any(), any(), any(), any(), any())).thenReturn("access-token");
        when(jwtUtils.generateRefreshToken(any())).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenReturn(user);

        AuthService.LoginResult result = authService.login("default", "testuser", "password", "000000", httpRequest);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(auditLogService).logLogin(any(), any(), any(), any(), any());
    }

    @Test
    void login_2faEnabled_success() {
        user.setTotpEnabled(true);
        user.setTotpSecret("secret");
        when(tenantRepository.findBySlug("default")).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantSlugAndUsername("default", "testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(twoFactorService.verifyCode("secret", "123456")).thenReturn(true);
        when(jwtUtils.generateAccessToken(any(), any(), any(), any(), any())).thenReturn("access-token");
        when(jwtUtils.generateRefreshToken(any())).thenReturn("refresh-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthService.LoginResult result = authService.login("default", "testuser", "password", "123456", httpRequest);

        assertThat(result.accessToken()).isEqualTo("access-token");
    }

    @Test
    void login_2faEnabled_wrongCode_throwsUnauthorized() {
        user.setTotpEnabled(true);
        user.setTotpSecret("secret");
        when(tenantRepository.findBySlug("default")).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantSlugAndUsername("default", "testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(twoFactorService.verifyCode("secret", "wrong")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("default", "testuser", "password", "wrong", httpRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid 2FA code");
    }

    @Test
    void login_wrongPassword_incrementsFailedAttempts() {
        when(tenantRepository.findBySlug("default")).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantSlugAndUsername("default", "testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);

        assertThatThrownBy(() -> authService.login("default", "testuser", "wrong", null, httpRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid credentials");

        assertThat(user.getFailedAttempts()).isEqualTo(1);
    }

    @Test
    void login_lockedUser_throwsException() {
        user.setLockedUntil(java.time.Instant.now().plusSeconds(600));
        user.setStatus(UserStatus.LOCKED);
        when(tenantRepository.findBySlug("default")).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantSlugAndUsername("default", "testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("default", "testuser", "p", null, httpRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void login_userNotFound_throwsUnauthorized() {
        when(tenantRepository.findBySlug("default")).thenReturn(Optional.of(tenant));
        when(userRepository.findByTenantSlugAndUsername("default", "ghost")).thenReturn(Optional.empty());
        when(userRepository.findByTenantSlugAndEmail("default", "ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("default", "ghost", "p", null, httpRequest))
                .isInstanceOf(ApiException.class);
    }
}
