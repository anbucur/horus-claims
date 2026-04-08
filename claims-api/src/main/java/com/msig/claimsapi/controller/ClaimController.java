package com.msig.claimsapi.controller;

import com.msig.claimsapi.service.ClaimService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Claim.WorkflowStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Slf4j
public class ClaimController {

    private final ClaimService claimService;

    @GetMapping
    public ResponseEntity<List<Claim>> getAllClaims() {
        log.debug("GET /api/claims - retrieving all claims");
        return ResponseEntity.ok(claimService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Claim> getClaimById(@PathVariable Long id) {
        log.debug("GET /api/claims/{} - retrieving claim", id);
        return claimService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/with-policy")
    public ResponseEntity<Claim> getClaimByIdWithPolicy(@PathVariable Long id) {
        log.debug("GET /api/claims/{}/with-policy - retrieving claim with policy", id);
        return claimService.findByIdWithPolicy(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/policy/{policyId}")
    public ResponseEntity<List<Claim>> getClaimsByPolicyId(@PathVariable Long policyId) {
        log.debug("GET /api/claims/policy/{} - retrieving claims for policy", policyId);
        return ResponseEntity.ok(claimService.findByPolicyId(policyId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Claim>> getClaimsByStatus(@PathVariable WorkflowStatus status) {
        log.debug("GET /api/claims/status/{} - retrieving claims by workflow status", status);
        return ResponseEntity.ok(claimService.findByWorkflowStatus(status));
    }

    @PostMapping
    public ResponseEntity<Claim> createClaim(@RequestBody Claim claim) {
        log.info("POST /api/claims - creating new claim");
        Claim saved = claimService.save(claim);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Claim> updateClaim(@PathVariable Long id, @RequestBody Claim claim) {
        log.info("PUT /api/claims/{} - updating claim", id);
        if (!id.equals(claim.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(claimService.save(claim));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Claim> updateWorkflowStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        log.info("PATCH /api/claims/{}/status - updating workflow status", id);
        WorkflowStatus newStatus = WorkflowStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(claimService.updateWorkflowStatus(id, newStatus));
    }

    @PatchMapping("/{id}/confidence")
    public ResponseEntity<Claim> updateAiConfidenceScore(
            @PathVariable Long id,
            @RequestBody Map<String, Double> body) {
        log.info("PATCH /api/claims/{}/confidence - updating AI confidence score", id);
        return ResponseEntity.ok(claimService.updateAiConfidenceScore(id, body.get("score")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClaim(@PathVariable Long id) {
        log.info("DELETE /api/claims/{} - deleting claim", id);
        claimService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}