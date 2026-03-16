package com.auth.security;

import com.auth.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtils {

    private final JwtProperties jwtProperties;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    private PrivateKey getPrivateKey() {
        if (privateKey == null) {
            privateKey = jwtProperties.loadPrivateKey();
        }
        return privateKey;
    }

    private PublicKey getPublicKey() {
        if (publicKey == null) {
            publicKey = jwtProperties.loadPublicKey();
        }
        return publicKey;
    }

    public String generateAccessToken(UUID userId, String username, String tenantId,
                                      List<String> roles, List<String> permissions) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("tenantId", tenantId);
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("type", "access");

        return Jwts.builder()
                .claims(claims)
                .subject(userId.toString())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiryMs()))
                .signWith(getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
                .claims(Map.of("type", "refresh"))
                .subject(userId.toString())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshTokenExpiryMs()))
                .signWith(getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractAllClaims(token).getSubject());
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).get("username", String.class);
    }

    public String extractTenantId(String token) {
        return extractAllClaims(token).get("tenantId", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return (List<String>) extractAllClaims(token).get("roles");
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        return (List<String>) extractAllClaims(token).get("permissions");
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            return "access".equals(extractAllClaims(token).get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            return "refresh".equals(extractAllClaims(token).get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }
}
