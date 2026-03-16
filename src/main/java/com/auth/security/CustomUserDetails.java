package com.auth.security;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

@Getter
@Builder
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final UUID tenantId;
    private final String tenantSlug;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final boolean accountNonExpired;
    private final boolean credentialsNonExpired;
}
