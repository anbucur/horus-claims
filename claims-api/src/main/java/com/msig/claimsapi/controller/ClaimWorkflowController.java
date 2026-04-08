package com.msig.claimsapi.controller;

import com.msig.claimsapi.service.ClaimService;
import com.msig.claimsdomain.entities.Claim;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/claims/{id}")
@RequiredArgsConstructor
@Slf4j
public class ClaimWorkflowController {

    private final ClaimService claimService;

    @PostMapping("/extract")
    public ResponseEntity<Map<String, Object>> extractClaim(@PathVariable Long id) {
        log.info("POST /api/claims/{}/extract - starting AI extraction", id);
        Claim claim = claimService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + id));
        
        claim.setWorkflowStatus(Claim.WorkflowStatus.EXTRACTING);
        claim.setAiConfidenceScore(0.0);
        claimService.save(claim);
        
        log.info("Claim {} extraction started, status updated to EXTRACTING", id);
        return ResponseEntity.ok(Map.of(
                "claim_id", id,
                "status", "EXTRACTING",
                "action", "extract",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/verify-policy")
    public ResponseEntity<Map<String, Object>> verifyPolicy(@PathVariable Long id) {
        log.info("POST /api/claims/{}/verify-policy - verifying policy coverage", id);
        Claim claim = claimService.findByIdWithPolicy(id)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + id));
        
        boolean policyValid = claim.getPolicy() != null && 
                claim.getPolicy().getStatus() == com.msig.claimsdomain.entities.Policy.PolicyStatus.ACTIVE;
        
        claim.setWorkflowStatus(Claim.WorkflowStatus.VERIFYING);
        claimService.save(claim);
        
        log.info("Claim {} policy verification completed, valid: {}", id, policyValid);
        return ResponseEntity.ok(Map.of(
                "claim_id", id,
                "status", "VERIFYING",
                "policy_valid", policyValid,
                "action", "verify-policy",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/route-hitl")
    public ResponseEntity<Map<String, Object>> routeToHitl(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        log.info("POST /api/claims/{}/route-hitl - routing to human review", id);
        Claim claim = claimService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + id));
        
        String reason = body != null ? body.getOrDefault("reason", "Manual review required") : "Manual review required";
        
        claim.setWorkflowStatus(Claim.WorkflowStatus.HITL);
        claimService.save(claim);
        
        log.info("Claim {} routed to HITL, reason: {}", id, reason);
        return ResponseEntity.ok(Map.of(
                "claim_id", id,
                "status", "HITL",
                "reason", reason,
                "action", "route-hitl",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/approve-stp")
    public ResponseEntity<Map<String, Object>> approveStp(@PathVariable Long id) {
        log.info("POST /api/claims/{}/approve-stp - approving straight-through processing", id);
        Claim claim = claimService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + id));
        
        if (claim.getAiConfidenceScore() == null || claim.getAiConfidenceScore() < 0.85) {
            log.warn("Claim {} STP approval denied - confidence score too low: {}", 
                    id, claim.getAiConfidenceScore());
            return ResponseEntity.badRequest().body(Map.of(
                    "claim_id", id,
                    "status", "REJECTED",
                    "reason", "AI confidence score below threshold (0.85)",
                    "current_score", claim.getAiConfidenceScore(),
                    "action", "approve-stp",
                    "timestamp", LocalDateTime.now().toString()
            ));
        }
        
        claim.setWorkflowStatus(Claim.WorkflowStatus.STP);
        claimService.save(claim);
        
        log.info("Claim {} approved for STP", id);
        return ResponseEntity.ok(Map.of(
                "claim_id", id,
                "status", "STP",
                "action", "approve-stp",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}