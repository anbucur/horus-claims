package com.msig.claimsapi.controller;

import com.msig.claimsapi.repository.ClaimRepository;
import com.msig.claimsapi.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "claims-api",
            "timestamp", java.time.Instant.now().toString()
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(Map.of(
            "policies", policyRepository.count(),
            "claims", claimRepository.count()
        ));
    }
}
