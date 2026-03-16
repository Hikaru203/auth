package com.auth.service;

import com.auth.domain.Permission;
import com.auth.exception.ApiException;
import com.auth.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<Permission> listAll() {
        return permissionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Permission> listByModule(String module) {
        return permissionRepository.findByModule(module);
    }

    @Transactional(readOnly = true)
    public Permission getById(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Permission not found"));
    }

    @Transactional
    public Permission create(String name, String module, String action, String description) {
        if (permissionRepository.existsByName(name)) {
            throw ApiException.conflict("Permission '" + name + "' already exists");
        }
        Permission p = Permission.builder()
                .name(name.toUpperCase())
                .module(module.toUpperCase())
                .action(action.toUpperCase())
                .description(description)
                .build();
        return permissionRepository.save(p);
    }

    @Transactional
    public void delete(UUID id) {
        Permission p = getById(id);
        permissionRepository.delete(p);
    }
}
