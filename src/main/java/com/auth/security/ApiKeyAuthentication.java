package com.auth.security;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

@Getter
public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final UUID apiKeyId;
    private final UUID userId;
    private final UUID tenantId;

    public ApiKeyAuthentication(UUID apiKeyId, UUID userId, UUID tenantId,
                                Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKeyId = apiKeyId;
        this.userId = userId;
        this.tenantId = tenantId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userId.toString();
    }
}
