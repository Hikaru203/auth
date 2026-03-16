package com.auth.service;

import com.auth.config.SecurityProperties;
import com.auth.domain.Role;
import com.auth.domain.User;
import com.auth.domain.UserStatus;
import com.auth.dto.response.RoleDistributionResponse;
import com.auth.exception.ApiException;
import com.auth.repository.RoleRepository;
import com.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    public Page<User> listUsers(UUID tenantId, Pageable pageable) {
        return userRepository.findAllByTenantId(tenantId, pageable);
    }

    @Transactional
    public User createUser(UUID tenantId, String username, String email, String password,
                          String firstName, String lastName, String phone) {
        if (userRepository.existsByTenantIdAndUsername(tenantId, username)) {
            throw ApiException.badRequest("Username already exists in this tenant");
        }
        if (userRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw ApiException.badRequest("Email already exists in this tenant");
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .phone(phone)
                .status(UserStatus.ACTIVE)
                .failedAttempts(0)
                .emailVerified(false)
                .totpEnabled(false)
                .build();
        // Set tenant manually or via a helper
        return userRepository.save(user);
    }

    public User getUser(UUID id, UUID tenantId) {
        return userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    @Transactional
    public User updateUser(UUID id, UUID tenantId, String firstName, String lastName, String phone) {
        User user = getUser(id, tenantId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID id, UUID tenantId) {
        User user = getUser(id, tenantId);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public void lockUser(UUID id, UUID tenantId) {
        User user = getUser(id, tenantId);
        user.setStatus(UserStatus.LOCKED);
        user.setLockedUntil(Instant.now().plus(securityProperties.getAccountLockout().getLockoutDuration()));
        userRepository.save(user);
    }

    @Transactional
    public void unlockUser(UUID id, UUID tenantId) {
        User user = getUser(id, tenantId);
        user.setStatus(UserStatus.ACTIVE);
        user.setLockedUntil(null);
        user.setFailedAttempts(0);
        userRepository.save(user);
    }

    @Transactional
    public void assignRole(UUID userId, UUID tenantId, UUID roleId) {
        User user = getUser(userId, tenantId);
        Role role = roleRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> ApiException.notFound("Role not found"));
        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public RoleDistributionResponse getRoleDistribution(UUID tenantId) {
        List<Object[]> results = userRepository.countUsersByRole(tenantId);
        List<RoleDistributionResponse.RoleCount> distribution = results.stream()
                .map(r -> new RoleDistributionResponse.RoleCount((String) r[0], (Long) r[1]))
                .collect(Collectors.toList());
        return RoleDistributionResponse.builder().distribution(distribution).build();
    }

    @Transactional
    public void removeRole(UUID userId, UUID tenantId, UUID roleId) {
        User user = getUser(userId, tenantId);
        user.getRoles().removeIf(r -> r.getId().equals(roleId));
        userRepository.save(user);
    }
}
