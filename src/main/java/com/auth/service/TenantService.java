package com.auth.service;

import com.auth.domain.Tenant;
import com.auth.exception.ApiException;
import com.auth.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public List<Tenant> listAll() {
        return tenantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Tenant getById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Tenant not found"));
    }

    @Transactional(readOnly = true)
    public Tenant getBySlug(String slug) {
        return tenantRepository.findBySlug(slug)
                .orElseThrow(() -> ApiException.notFound("Tenant not found: " + slug));
    }

    @Transactional
    public Tenant create(String name, String slug) {
        if (tenantRepository.existsBySlug(slug)) {
            throw ApiException.conflict("Tenant with slug '" + slug + "' already exists");
        }
        Tenant tenant = Tenant.builder()
                .name(name)
                .slug(slug.toLowerCase().trim())
                .active(true)
                .build();
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant update(UUID id, String name, boolean active) {
        Tenant tenant = getById(id);
        tenant.setName(name);
        tenant.setActive(active);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public void delete(UUID id) {
        Tenant tenant = getById(id);
        tenantRepository.delete(tenant);
    }
}
