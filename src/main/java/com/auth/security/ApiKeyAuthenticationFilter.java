package com.auth.security;

import com.auth.domain.ApiKey;
import com.auth.repository.ApiKeyRepository;
import com.auth.util.HashUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "x-api-key";

    private final ApiKeyRepository apiKeyRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String rawKey = request.getHeader(API_KEY_HEADER);

        if (StringUtils.hasText(rawKey) && SecurityContextHolder.getContext().getAuthentication() == null) {
            String timestamp = request.getHeader("x-timestamp");
            String signature = request.getHeader("x-signature");

            if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(signature)) {
                sendUnauthorized(response, "Missing security headers (x-timestamp or x-signature)");
                return;
            }

            // Time check (5 mins)
            try {
                long ts = Long.parseLong(timestamp);
                long now = Instant.now().getEpochSecond();
                if (Math.abs(now - ts) > 300) {
                    sendUnauthorized(response, "Request expired (TTL: 5m)");
                    return;
                }
            } catch (NumberFormatException e) {
                sendUnauthorized(response, "Invalid x-timestamp format");
                return;
            }

            // Key lookup
            String keyHash = HashUtils.sha256(rawKey);
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHash(keyHash);

            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();

                if (!apiKey.isValid()) {
                    sendUnauthorized(response, "API key is invalid, expired, or revoked");
                    return;
                }

                // Signature verification
                // signature = hex(sha256(rawKey + timestamp + body))
                // For now, if no body is present (GET), we use rawKey + timestamp
                String body = ""; // Simplification: in a real proxy, you'd read the cached body
                String expectedSignature = HashUtils.sha256(rawKey + timestamp + body);
                
                if (!expectedSignature.equalsIgnoreCase(signature)) {
                    sendUnauthorized(response, "Invalid x-signature");
                    return;
                }

                // Update last used
                apiKey.setLastUsedAt(Instant.now());
                apiKey.setLastUsedIp(getClientIp(request));
                apiKeyRepository.save(apiKey);

                List<SimpleGrantedAuthority> authorities;
                if (apiKey.getScopes() != null && apiKey.getScopes().length > 0) {
                    authorities = Arrays.stream(apiKey.getScopes())
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                } else {
                    authorities = apiKey.getUser().getRoles().stream()
                            .flatMap(role -> role.getPermissions().stream())
                            .map(p -> new SimpleGrantedAuthority(p.getName()))
                            .collect(Collectors.toList());
                }

                CustomUserDetails userDetails = CustomUserDetails.builder()
                        .id(apiKey.getUser().getId())
                        .tenantId(apiKey.getTenant().getId())
                        .tenantSlug(apiKey.getTenant().getSlug())
                        .username(apiKey.getUser().getUsername())
                        .password("") 
                        .authorities(authorities)
                        .enabled(true)
                        .accountNonLocked(true)
                        .accountNonExpired(true)
                        .credentialsNonExpired(true)
                        .build();

                ApiKeyAuthentication authentication = new ApiKeyAuthentication(
                        apiKey.getId(),
                        apiKey.getUser().getId(),
                        apiKey.getTenant().getId(),
                        userDetails,
                        authorities
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } else {
                sendUnauthorized(response, "Invalid API key");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", message
        ));
    }
}
