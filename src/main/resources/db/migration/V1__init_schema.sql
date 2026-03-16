-- ============================================================
-- V1: Initial Schema
-- Auth & Account Management Service
-- ============================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- TENANTS
-- ============================================================
CREATE TABLE tenants (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    metadata    JSONB,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenants_slug ON tenants(slug);

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id           UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    username            VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    first_name          VARCHAR(100),
    last_name           VARCHAR(100),
    phone               VARCHAR(20),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE','INACTIVE','LOCKED','PENDING_VERIFICATION')),
    failed_attempts     INT NOT NULL DEFAULT 0,
    locked_until        TIMESTAMP WITH TIME ZONE,
    totp_secret         VARCHAR(255),
    totp_enabled        BOOLEAN NOT NULL DEFAULT FALSE,
    email_verified      BOOLEAN NOT NULL DEFAULT FALSE,
    password_changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login_at       TIMESTAMP WITH TIME ZONE,
    last_login_ip       VARCHAR(45),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, username),
    UNIQUE(tenant_id, email)
);

CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_email ON users(tenant_id, email);
CREATE INDEX idx_users_username ON users(tenant_id, username);
CREATE INDEX idx_users_status ON users(status);

-- ============================================================
-- ROLES
-- ============================================================
CREATE TABLE roles (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, name)
);

CREATE INDEX idx_roles_tenant_id ON roles(tenant_id);

-- ============================================================
-- PERMISSIONS
-- ============================================================
CREATE TABLE permissions (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    module      VARCHAR(100) NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_permissions_module ON permissions(module);

-- ============================================================
-- ROLE_PERMISSIONS (join)
-- ============================================================
CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (role_id, permission_id)
);

-- ============================================================
-- USER_ROLES (join)
-- ============================================================
CREATE TABLE user_roles (
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id    UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

-- ============================================================
-- API KEYS
-- ============================================================
CREATE TABLE api_keys (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    key_hash        VARCHAR(255) NOT NULL UNIQUE,
    key_prefix      VARCHAR(10)  NOT NULL,
    scopes          TEXT[]       NOT NULL DEFAULT '{}',
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at      TIMESTAMP WITH TIME ZONE,
    revoked_reason  VARCHAR(500),
    expires_at      TIMESTAMP WITH TIME ZONE,
    last_used_at    TIMESTAMP WITH TIME ZONE,
    last_used_ip    VARCHAR(45),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_api_keys_key_hash   ON api_keys(key_hash);
CREATE INDEX idx_api_keys_user_id    ON api_keys(user_id);
CREATE INDEX idx_api_keys_tenant_id  ON api_keys(tenant_id);
CREATE INDEX idx_api_keys_expires_at ON api_keys(expires_at);

-- ============================================================
-- REFRESH TOKENS
-- ============================================================
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    device_info VARCHAR(500),
    ip_address  VARCHAR(45),
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id    ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- ============================================================
-- PASSWORD RESET TOKENS
-- ============================================================
CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    used_at     TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prt_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_prt_token   ON password_reset_tokens(token_hash);

-- ============================================================
-- AUDIT LOGS
-- ============================================================
CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID REFERENCES tenants(id) ON DELETE SET NULL,
    user_id     UUID,
    username    VARCHAR(100),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   VARCHAR(255),
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    status_code INT,
    request_method VARCHAR(10),
    request_path   VARCHAR(1000),
    metadata    JSONB,
    error_message TEXT,
    duration_ms BIGINT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_tenant_id  ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_user_id    ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action     ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_entity     ON audit_logs(entity_type, entity_id);

-- ============================================================
-- IP RULES (Whitelist / Blacklist)
-- ============================================================
CREATE TABLE ip_rules (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID REFERENCES tenants(id) ON DELETE CASCADE,
    ip_address  VARCHAR(45) NOT NULL,
    rule_type   VARCHAR(10) NOT NULL CHECK (rule_type IN ('WHITELIST', 'BLACKLIST')),
    description VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, ip_address, rule_type)
);

CREATE INDEX idx_ip_rules_tenant ON ip_rules(tenant_id, rule_type, active);
