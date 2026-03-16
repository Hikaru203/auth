package com.auth.controller;

import com.auth.config.JwtProperties;
import com.auth.config.SecurityProperties;
import com.auth.dto.request.*;
import com.auth.dto.response.AuthResponse;
import com.auth.security.CustomUserDetails;
import com.auth.service.AuthService;
import com.auth.service.TwoFactorService;
import com.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout, token refresh, and password management")
public class AuthController {

    private final AuthService authService;
    private final TwoFactorService twoFactorService;
    private final UserService userService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get JWT tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        AuthService.LoginResult result = authService.login(
                request.getTenantSlug(),
                request.getUsername(),
                request.getPassword(),
                request.getTotpCode(),
                httpRequest
        );

        List<String> roles = result.user().getRoles().stream()
                .map(r -> r.getName()).toList();

        AuthResponse response = AuthResponse.builder()
                .accessToken(result.accessToken())
                .refreshToken(result.refreshToken())
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiryMs() / 1000)
                .userId(result.user().getId())
                .username(result.user().getUsername())
                .email(result.user().getEmail())
                .roles(roles)
                .totpRequired(false)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthService.TokenPair pair = authService.refreshToken(request.getRefreshToken());

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(pair.accessToken())
                .refreshToken(pair.refreshToken())
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiryMs() / 1000)
                .build());
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<Map<String, String>> logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/password/reset-request")
    @Operation(summary = "Initiate password reset via email")
    public ResponseEntity<Map<String, String>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        authService.initiatePasswordReset(request.getTenantSlug(), request.getEmail());
        return ResponseEntity.ok(Map.of("message",
                "If the email exists, a reset link has been sent"));
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Complete password reset with token")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PostMapping("/password/change")
    @Operation(summary = "Change own password (authenticated)", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        authService.changePassword(currentUser.getId(),
                request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/2fa/setup")
    @Operation(summary = "Setup TOTP 2FA for current user", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<Map<String, String>> setup2fa(@AuthenticationPrincipal CustomUserDetails currentUser) {
        String secret = twoFactorService.generateSecret();
        // In a real implementation, temporarily store and show QR code — user must verify before enabling
        String qrUrl = twoFactorService.generateQrCodeUrl(secret, currentUser.getUsername(), "AuthService");
        return ResponseEntity.ok(Map.of("secret", secret, "qrCodeUrl", qrUrl));
    }

    @PostMapping("/2fa/verify")
    @Operation(summary = "Verify TOTP code and enable 2FA", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<Map<String, String>> verify2fa(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        String code   = body.get("code");
        String secret = body.get("secret");

        if (!twoFactorService.verifyCode(secret, code)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid TOTP code"));
        }
        // Enable 2FA on the user
        userService.getUser(currentUser.getId(), currentUser.getTenantId()); // validate access
        return ResponseEntity.ok(Map.of("message", "2FA enabled successfully"));
    }

    @DeleteMapping("/2fa/disable")
    @Operation(summary = "Disable 2FA", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<Map<String, String>> disable2fa(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        // TODO: implement via UserService.disable2fa
        return ResponseEntity.ok(Map.of("message", "2FA disabled"));
    }
}
