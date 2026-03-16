package com.auth.service;

import com.auth.domain.Role;
import com.auth.domain.*;
import com.auth.exception.ApiException;
import com.auth.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public List<Role> listRoles(UUID tenantId) {
        return roleRepository.findAllByTenantIdWithPermissions(tenantId);
    }

    @Transactional(readOnly = true)
    public Role getRole(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> ApiException.notFound("Role not found"));
    }

    @Transactional
    public Role createRole(UUID tenantId, String name, String description) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("Tenant not found"));

        if (roleRepository.existsByNameAndTenantId(name, tenantId)) {
            throw ApiException.conflict("Role '" + name + "' already exists");
        }

        Role role = Role.builder()
                .tenant(tenant)
                .name(name.toUpperCase())
                .description(description)
                .systemRole(false)
                .build();
        return roleRepository.save(role);
    }

    @Transactional
    public Role updateRole(UUID roleId, UUID tenantId, String name, String description) {
        Role role = getRole(roleId);
        if (!role.getTenant().getId().equals(tenantId)) {
            throw ApiException.forbidden("Access denied");
        }
        if (role.isSystemRole()) {
            throw ApiException.badRequest("Cannot modify system roles");
        }
        if (name != null) role.setName(name.toUpperCase());
        if (description != null) role.setDescription(description);
        return roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(UUID roleId, UUID tenantId) {
        Role role = getRole(roleId);
        if (!role.getTenant().getId().equals(tenantId)) throw ApiException.forbidden("Access denied");
        if (role.isSystemRole()) throw ApiException.badRequest("Cannot delete system roles");
        roleRepository.delete(role);
    }

    @Transactional
    public Role addPermission(UUID roleId, UUID permissionId, UUID tenantId) {
        Role role = getRole(roleId);
        if (!role.getTenant().getId().equals(tenantId)) throw ApiException.forbidden("Access denied");

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> ApiException.notFound("Permission not found"));

        role.getPermissions().add(permission);
        return roleRepository.save(role);
    }

    @Transactional
    public Role removePermission(UUID roleId, UUID permissionId, UUID tenantId) {
        Role role = getRole(roleId);
        if (!role.getTenant().getId().equals(tenantId)) throw ApiException.forbidden("Access denied");
        role.getPermissions().removeIf(p -> p.getId().equals(permissionId));
        return roleRepository.save(role);
    }
}
