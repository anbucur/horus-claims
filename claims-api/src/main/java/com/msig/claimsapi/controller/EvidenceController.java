package com.msig.claimsapi.controller;

import com.msig.claimsapi.repository.EvidenceRepository;
import com.msig.claimsdomain.entities.Evidence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evidence")
@RequiredArgsConstructor
@Slf4j
public class EvidenceController {

    private final EvidenceRepository evidenceRepository;

    @GetMapping
    public ResponseEntity<List<Evidence>> getAllEvidence() {
        log.debug("GET /api/evidence - retrieving all evidence");
        return ResponseEntity.ok(evidenceRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Evidence> getEvidenceById(@PathVariable("id") Long id) {
        log.debug("GET /api/evidence/{} - retrieving evidence", id);
        return evidenceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/claim/{claimId}")
    public ResponseEntity<List<Evidence>> getEvidenceByClaimId(@PathVariable Long claimId) {
        log.debug("GET /api/evidence/claim/{} - retrieving evidence for claim", claimId);
        return ResponseEntity.ok(evidenceRepository.findByClaimId(claimId));
    }

    @GetMapping("/claim/{claimId}/quarantined")
    public ResponseEntity<List<Evidence>> getQuarantinedEvidenceByClaimId(@PathVariable Long claimId) {
        log.debug("GET /api/evidence/claim/{}/quarantined - retrieving quarantined evidence", claimId);
        return ResponseEntity.ok(evidenceRepository.findByClaimIdAndIsQuarantined(claimId, true));
    }

    @PostMapping
    public ResponseEntity<Evidence> createEvidence(@RequestBody Evidence evidence) {
        log.info("POST /api/evidence - creating new evidence");
        Evidence saved = evidenceRepository.save(evidence);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Evidence> updateEvidence(@PathVariable("id") Long id, @RequestBody Evidence evidence) {
        log.info("PUT /api/evidence/{} - updating evidence", id);
        if (!id.equals(evidence.getId())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(evidenceRepository.save(evidence));
    }

    @PatchMapping("/{id}/quarantine")
    public ResponseEntity<Evidence> quarantineEvidence(@PathVariable("id") Long id) {
        log.info("PATCH /api/evidence/{}/quarantine - quarantining evidence", id);
        Evidence evidence = evidenceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + id));
        evidence.setIsQuarantined(true);
        return ResponseEntity.ok(evidenceRepository.save(evidence));
    }

    @PatchMapping("/{id}/release")
    public ResponseEntity<Evidence> releaseEvidence(@PathVariable("id") Long id) {
        log.info("PATCH /api/evidence/{}/release - releasing evidence from quarantine", id);
        Evidence evidence = evidenceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Evidence not found: " + id));
        evidence.setIsQuarantined(false);
        return ResponseEntity.ok(evidenceRepository.save(evidence));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvidence(@PathVariable("id") Long id) {
        log.info("DELETE /api/evidence/{} - deleting evidence", id);
        evidenceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}