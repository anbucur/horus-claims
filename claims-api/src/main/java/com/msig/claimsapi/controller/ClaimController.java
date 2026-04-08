package com.msig.claimsapi.controller;

import com.msig.claimsapi.service.ClaimService;
import com.msig.claimsapi.service.audit.ClaimAuditService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Claim.WorkflowStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.msig.claimsdomain.entities.ClaimStatusHistory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Claims", description = "Claims lifecycle management")
public class ClaimController {

    private final ClaimService claimService;
    private final ClaimAuditService auditService;

    @GetMapping
    @Operation(summary = "Get all claims")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "List of all claims"))
    public ResponseEntity<List<Claim>> getAllClaims() {
        log.debug("GET /api/claims - retrieving all claims");
        return ResponseEntity.ok(claimService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get claim by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim found"),
            @ApiResponse(responseCode = "404", description = "Claim not found")
    })
    public ResponseEntity<Claim> getClaimById(
            @Parameter(description = "Claim ID") @PathVariable("id") Long id) {
        log.debug("GET /api/claims/{} - retrieving claim", id);
        return claimService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/with-policy")
    public ResponseEntity<Claim> getClaimByIdWithPolicy(@PathVariable("id") Long id) {
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
    @Operation(summary = "Create a new claim")
    @ApiResponse(responseCode = "201", description = "Claim created successfully")
    public ResponseEntity<Claim> createClaim(
            @Parameter(description = "Claim payload") @RequestBody Claim claim) {
        log.info("POST /api/claims - creating new claim");
        Claim saved = claimService.save(claim);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing claim")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Claim updated"),
            @ApiResponse(responseCode = "400", description = "Claim ID mismatch")
    })
    public ResponseEntity<Claim> updateClaim(
            @Parameter(description = "Claim ID") @PathVariable("id") Long id,
            @Parameter(description = "Updated claim payload") @RequestBody Claim claim) {
        log.info("PUT /api/claims/{} - updating claim", id);
        if (!id.equals(claim.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(claimService.save(claim));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Claim> updateWorkflowStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        log.info("PATCH /api/claims/{}/status - updating workflow status", id);
        WorkflowStatus newStatus = WorkflowStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(claimService.updateWorkflowStatus(id, newStatus));
    }

    @PatchMapping("/{id}/confidence")
    public ResponseEntity<Claim> updateAiConfidenceScore(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Double> body) {
        log.info("PATCH /api/claims/{}/confidence - updating AI confidence score", id);
        return ResponseEntity.ok(claimService.updateAiConfidenceScore(id, body.get("score")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClaim(@PathVariable("id") Long id) {
        log.info("DELETE /api/claims/{} - deleting claim", id);
        claimService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<ClaimStatusHistory>> getClaimHistory(@PathVariable("id") Long id) {
        log.debug("GET /api/claims/{}/history - retrieving claim history", id);
        return ResponseEntity.ok(auditService.getClaimHistory(id));
    }
}