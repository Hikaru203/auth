package com.auth.security;

import com.auth.service.AuditLogService;
import com.auth.util.RequestUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class AuditLoggingFilter extends OncePerRequestFilter {

    private final AuditLogService auditLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || !request.getRequestURI().startsWith("/api/v1")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip login/refresh/audit to avoid noise or recursion if needed
        // but user specifically said "all logs"
        
        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequest(request, response, duration);
        }
    }

    private void logRequest(HttpServletRequest request, HttpServletResponse response, long duration) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        int status = response.getStatus();

        // Skip audit endpoints themselves and static assets
        if (path.contains("/api/v1/audit")) return;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = null;
        UUID tenantId = null;
        String username = "anonymous";

        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails user) {
            userId = user.getId();
            tenantId = user.getTenantId();
            username = user.getUsername();
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("method", method);
        metadata.put("durationMs", duration);
        metadata.put("queryString", request.getQueryString());

        String action = method + " " + path;
        
        auditLogService.log(
                action,
                "API",
                null,
                tenantId,
                userId,
                username,
                RequestUtils.getClientIp(request),
                RequestUtils.getUserAgent(request),
                status,
                path,
                metadata
        );
    }
}
