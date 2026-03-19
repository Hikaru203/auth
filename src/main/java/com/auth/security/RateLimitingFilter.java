package com.auth.security;

import com.auth.config.SecurityProperties;
import com.auth.repository.IpRuleRepository;
import com.auth.domain.IpRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final SecurityProperties securityProperties;
    private final IpRuleRepository ipRuleRepository;
    private final ObjectMapper objectMapper;

    // Per-IP bucket cache
    private final ConcurrentHashMap<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    // Per-API-key bucket cache
    private final ConcurrentHashMap<String, Bucket> apiKeyBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = getClientIp(request);

        // Check IP blacklist
        List<IpRule> blacklist = ipRuleRepository.findAllByTenantIdAndRuleTypeAndActiveTrue(
                null, IpRule.IpRuleType.BLACKLIST);
        boolean isBlacklisted = blacklist.stream().anyMatch(r -> r.getIpAddress().equals(ip));
        if (isBlacklisted) {
            sendError(response, HttpStatus.FORBIDDEN.value(), "Your IP address has been blocked");
            return;
        }

        // Apply rate limiting to login endpoint
        boolean isLoginPath = request.getRequestURI().endsWith("/auth/login");
        if (isLoginPath) {
            Bucket bucket = ipBuckets.computeIfAbsent(ip, this::createLoginBucket);
            if (!bucket.tryConsume(1)) {
                sendError(response, HttpStatus.TOO_MANY_REQUESTS.value(),
                        "Too many login attempts. Please try again later.");
                return;
            }
        }

        // Apply rate limiting to API key calls
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null) {
            Bucket bucket = apiKeyBuckets.computeIfAbsent(apiKey, this::createApiKeyBucket);
            if (!bucket.tryConsume(1)) {
                sendError(response, HttpStatus.TOO_MANY_REQUESTS.value(),
                        "API rate limit exceeded");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket createLoginBucket(String ip) {
        int perMinute = securityProperties.getRateLimit().getLoginRequestsPerMinute();
        return Bucket.builder()
                .addLimit(Bandwidth.classic(perMinute, Refill.greedy(perMinute, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket createApiKeyBucket(String key) {
        int perMinute = securityProperties.getRateLimit().getApiRequestsPerMinute();
        return Bucket.builder()
                .addLimit(Bandwidth.classic(perMinute, Refill.greedy(perMinute, Duration.ofMinutes(1))))
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        else ip = ip.split(",")[0].trim();
        return ip;
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                Map.of("status", status, "error", HttpStatus.valueOf(status).getReasonPhrase(),
                        "message", message));
    }
}
