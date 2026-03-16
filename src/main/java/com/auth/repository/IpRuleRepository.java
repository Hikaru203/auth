package com.auth.repository;

import com.auth.domain.IpRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IpRuleRepository extends JpaRepository<IpRule, UUID> {

    List<IpRule> findAllByTenantIdAndRuleTypeAndActiveTrue(UUID tenantId, IpRule.IpRuleType ruleType);

    Optional<IpRule> findByIpAddressAndRuleType(String ipAddress, IpRule.IpRuleType ruleType);
}
