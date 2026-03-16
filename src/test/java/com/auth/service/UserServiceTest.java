package com.auth.service;

import com.auth.config.SecurityProperties;
import com.auth.domain.User;
import com.auth.domain.UserStatus;
import com.auth.repository.RoleRepository;
import com.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private SecurityProperties securityProperties = new SecurityProperties();

    private UserService userService;

    private UUID tenantId;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, passwordEncoder, securityProperties);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(userId);
    }

    @Test
    void createUser_success() {
        when(userRepository.existsByTenantIdAndUsername(any(), any())).thenReturn(false);
        when(userRepository.existsByTenantIdAndEmail(any(), any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(user);

        User result = userService.createUser(tenantId, "testuser", "test@example.com", "pass", "First", "Last", "123");

        assertThat(result).isNotNull();
        verify(userRepository).save(any());
    }

    @Test
    void lockUser_updatesStatusAndLockTime() {
        securityProperties.getAccountLockout().setLockoutDuration(Duration.ofMinutes(30));
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));

        userService.lockUser(userId, tenantId);

        assertThat(user.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(user.getLockedUntil()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void unlockUser_resetsStats() {
        user.setStatus(UserStatus.LOCKED);
        user.setFailedAttempts(5);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(user));

        userService.unlockUser(userId, tenantId);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getFailedAttempts()).isEqualTo(0);
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepository).save(user);
    }
}
