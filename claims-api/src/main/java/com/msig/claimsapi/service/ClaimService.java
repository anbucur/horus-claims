package com.msig.claimsapi.service;

import com.msig.claimsapi.repository.ClaimRepository;
import com.msig.claimsapi.service.audit.ClaimAuditService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Claim.WorkflowStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ClaimAuditService auditService;

    @Transactional(readOnly = true)
    public List<Claim> findAll() {
        return claimRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Claim> findById(Long id) {
        return claimRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Claim> findByIdWithPolicy(Long id) {
        return claimRepository.findByIdWithPolicy(id);
    }

    @Transactional(readOnly = true)
    public List<Claim> findByPolicyId(Long policyId) {
        return claimRepository.findByPolicyId(policyId);
    }

    @Transactional(readOnly = true)
    public List<Claim> findByWorkflowStatus(WorkflowStatus status) {
        return claimRepository.findByWorkflowStatus(status);
    }

    @Transactional
    public Claim save(Claim claim) {
        log.debug("Saving claim: {}", claim.getId());
        return claimRepository.save(claim);
    }

    @Transactional
    public void deleteById(Long id) {
        log.debug("Deleting claim: {}", id);
        claimRepository.deleteById(id);
    }

    @Transactional
    public Claim updateWorkflowStatus(Long claimId, WorkflowStatus newStatus) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
        WorkflowStatus fromStatus = claim.getWorkflowStatus();
        claim.setWorkflowStatus(newStatus);
        Claim saved = claimRepository.save(claim);
        auditService.logStatusChange(saved, fromStatus, newStatus, null, null);
        return saved;
    }

    @Transactional
    public Claim updateAiConfidenceScore(Long claimId, Double score) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
        claim.setAiConfidenceScore(score);
        return claimRepository.save(claim);
    }
}