package com.msig.claimsapi.controller;

import com.msig.claimsapi.service.PolicyService;
import com.msig.claimsdomain.entities.Policy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
@Slf4j
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    public ResponseEntity<List<Policy>> getAllPolicies() {
        log.debug("GET /api/policies - retrieving all policies");
        return ResponseEntity.ok(policyService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Policy> getPolicyById(@PathVariable("id") Long id) {
        log.debug("GET /api/policies/{} - retrieving policy", id);
        return policyService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{policyNumber}")
    public ResponseEntity<Policy> getPolicyByNumber(@PathVariable String policyNumber) {
        log.debug("GET /api/policies/number/{} - retrieving policy by number", policyNumber);
        return policyService.findByPolicyNumber(policyNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{policyNumber}/exists")
    public ResponseEntity<Map<String, Boolean>> checkPolicyExists(@PathVariable String policyNumber) {
        log.debug("GET /api/policies/number/{}/exists - checking policy existence", policyNumber);
        return ResponseEntity.ok(Map.of("exists", policyService.existsByPolicyNumber(policyNumber)));
    }

    @PostMapping
    public ResponseEntity<Policy> createPolicy(@RequestBody Policy policy) {
        log.info("POST /api/policies - creating new policy");
        Policy saved = policyService.save(policy);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Policy> updatePolicy(@PathVariable("id") Long id, @RequestBody Policy policy) {
        log.info("PUT /api/policies/{} - updating policy", id);
        if (!id.equals(policy.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(policyService.save(policy));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Policy> updatePolicyStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        log.info("PATCH /api/policies/{}/status - updating policy status", id);
        Policy.PolicyStatus newStatus = Policy.PolicyStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(policyService.updateStatus(id, newStatus));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable("id") Long id) {
        log.info("DELETE /api/policies/{} - deleting policy", id);
        policyService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}