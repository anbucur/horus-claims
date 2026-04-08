package com.msig.claimsapi.service;

import com.msig.claimsapi.repository.PolicyRepository;
import com.msig.claimsdomain.entities.Policy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final PolicyRepository policyRepository;

    @Transactional(readOnly = true)
    public List<Policy> findAll() {
        return policyRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Policy> findById(Long id) {
        return policyRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Policy> findByPolicyNumber(String policyNumber) {
        return policyRepository.findByPolicyNumber(policyNumber);
    }

    @Transactional(readOnly = true)
    public boolean existsByPolicyNumber(String policyNumber) {
        return policyRepository.existsByPolicyNumber(policyNumber);
    }

    @Transactional
    public Policy save(Policy policy) {
        log.debug("Saving policy: {}", policy.getPolicyNumber());
        return policyRepository.save(policy);
    }

    @Transactional
    public void deleteById(Long id) {
        log.debug("Deleting policy: {}", id);
        policyRepository.deleteById(id);
    }

    @Transactional
    public Policy updateStatus(Long policyId, Policy.PolicyStatus newStatus) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));
        policy.setStatus(newStatus);
        return policyRepository.save(policy);
    }
}