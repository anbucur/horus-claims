package com.msig.claimsapi.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
@Slf4j
public class IngestController {

    @PostMapping("/fnohl")
    public ResponseEntity<Map<String, Object>> ingestFnohl(@RequestBody Map<String, Object> payload) {
        log.info("POST /api/ingest/fnohl - received FNOHL payload");
        String claimId = (String) payload.getOrDefault("claim_id", "unknown");
        log.debug("FNOHL payload for claim: {}", claimId);
        return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "source", "fnohl",
                "claim_id", claimId,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/email")
    public ResponseEntity<Map<String, Object>> ingestEmail(@RequestBody Map<String, Object> payload) {
        log.info("POST /api/ingest/email - received email payload");
        String claimId = (String) payload.getOrDefault("claim_id", "unknown");
        log.debug("Email payload for claim: {}", claimId);
        return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "source", "email",
                "claim_id", claimId,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> ingestBatch(@RequestBody Map<String, Object> payload) {
        log.info("POST /api/ingest/batch - received batch payload");
        int count = payload.containsKey("claims") ? ((java.util.List<?>) payload.get("claims")).size() : 0;
        log.debug("Batch payload with {} claims", count);
        return ResponseEntity.ok(Map.of(
                "status", "accepted",
                "source", "batch",
                "count", count,
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}