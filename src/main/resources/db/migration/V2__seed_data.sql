-- ============================================================
-- V2: Seed Data — Default Tenant, Roles, Permissions, Admin User
-- ============================================================

-- Default Tenant
INSERT INTO tenants (id, name, slug, active) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Default Organization', 'default', TRUE);

-- ============================================================
-- Permissions
-- ============================================================
INSERT INTO permissions (id, name, module, action, description) VALUES
    -- User Management
    ('10000000-0000-0000-0000-000000000001', 'USER_READ',   'USER', 'READ',   'View user details'),
    ('10000000-0000-0000-0000-000000000002', 'USER_WRITE',  'USER', 'WRITE',  'Create and update users'),
    ('10000000-0000-0000-0000-000000000003', 'USER_DELETE', 'USER', 'DELETE', 'Delete users'),
    ('10000000-0000-0000-0000-000000000004', 'USER_LOCK',   'USER', 'LOCK',   'Lock/unlock user accounts'),
    -- Role Management
    ('10000000-0000-0000-0000-000000000010', 'ROLE_READ',   'ROLE', 'READ',   'View roles'),
    ('10000000-0000-0000-0000-000000000011', 'ROLE_WRITE',  'ROLE', 'WRITE',  'Create and update roles'),
    ('10000000-0000-0000-0000-000000000012', 'ROLE_DELETE', 'ROLE', 'DELETE', 'Delete roles'),
    -- Permission Management
    ('10000000-0000-0000-0000-000000000020', 'PERMISSION_READ',  'PERMISSION', 'READ',  'View permissions'),
    ('10000000-0000-0000-0000-000000000021', 'PERMISSION_WRITE', 'PERMISSION', 'WRITE', 'Manage permissions'),
    -- API Key Management
    ('10000000-0000-0000-0000-000000000030', 'API_KEY_READ',   'API_KEY', 'READ',   'View API keys'),
    ('10000000-0000-0000-0000-000000000031', 'API_KEY_WRITE',  'API_KEY', 'WRITE',  'Create API keys'),
    ('10000000-0000-0000-0000-000000000032', 'API_KEY_DELETE', 'API_KEY', 'DELETE', 'Revoke API keys'),
    -- Audit
    ('10000000-0000-0000-0000-000000000040', 'AUDIT_READ', 'AUDIT', 'READ', 'View audit logs'),
    -- Tenant Management
    ('10000000-0000-0000-0000-000000000050', 'TENANT_READ',   'TENANT', 'READ',   'View tenants'),
    ('10000000-0000-0000-0000-000000000051', 'TENANT_WRITE',  'TENANT', 'WRITE',  'Create and update tenants'),
    ('10000000-0000-0000-0000-000000000052', 'TENANT_DELETE', 'TENANT', 'DELETE', 'Delete tenants');

-- ============================================================
-- Roles
-- ============================================================
INSERT INTO roles (id, tenant_id, name, description, system_role) VALUES
    ('20000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001', 'SUPER_ADMIN', 'Super administrator with all permissions', TRUE),
    ('20000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', 'ADMIN',       'Tenant administrator', TRUE),
    ('20000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', 'USER',        'Regular user', TRUE),
    ('20000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'GUEST',       'Read-only guest', TRUE);

-- SUPER_ADMIN → all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000001', id FROM permissions;

-- ADMIN → all except tenant management
INSERT INTO role_permissions (role_id, permission_id)
SELECT '20000000-0000-0000-0000-000000000002', id FROM permissions
WHERE module NOT IN ('TENANT');

-- USER → read own + api key management
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001'), -- USER_READ
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000030'), -- API_KEY_READ
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000031'), -- API_KEY_WRITE
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000032'); -- API_KEY_DELETE

-- GUEST → user read only
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001'); -- USER_READ

-- ============================================================
-- Default Admin User
-- Password: Admin@123 (BCrypt hash)
-- ============================================================
INSERT INTO users (id, tenant_id, username, email, password_hash, first_name, last_name, status, email_verified) VALUES
    ('30000000-0000-0000-0000-000000000001',
     '00000000-0000-0000-0000-000000000001',
     'admin',
     'admin@auth-service.local',
     '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj3rb4.4Kg2G',
     'System',
     'Admin',
     'ACTIVE',
     TRUE);

-- Assign SUPER_ADMIN role to admin
INSERT INTO user_roles (user_id, role_id) VALUES
    ('30000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001');
