package com.msig.claimsapi.controller;

import com.msig.claimsapi.config.ProcessingConfig;
import com.msig.claimsapi.service.ClaimProcessingService;
import com.msig.claimsapi.service.ClaimService;
import com.msig.claimsapi.service.ai.AIOrchestrationService;
import com.msig.claimsapi.service.ai.AICircuitBreaker;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.model.ProcessingMode;
import com.msig.claimsdomain.model.ProcessingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ClaimWorkflowController {

    private final ClaimService claimService;
    private final ClaimProcessingService claimProcessingService;
    private final ProcessingConfig processingConfig;
    private final AIOrchestrationService aiOrchestrationService;
    private final AICircuitBreaker circuitBreaker;

    @PostMapping("/api/claims/{id}/process")
    public ResponseEntity<Map<String, Object>> processClaim(
            @PathVariable Long id,
            @RequestParam(required = false) ProcessingMode mode) {
        log.info("POST /api/claims/{}/process - mode: {}", id, mode);
        
        Claim claim = claimService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + id));
        
        ProcessingMode effectiveMode = mode != null ? mode : processingConfig.getDefaultMode();
        
        ProcessingResult<Claim> result = claimProcessingService.processClaim(id, effectiveMode);
        
        return ResponseEntity.ok(Map.of(
                "claim_id", id,
                "status", result.getData().getWorkflowStatus(),
                "mode", result.getMode(),
                "ai_available", result.isAiAvailable(),
                "confidence_score", result.getData().getAiConfidenceScore() != null 
                        ? result.getData().getAiConfidenceScore() : 0.0,
                "warnings", result.getWarnings(),
                "trace_id", result.getTraceId(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/api/claims/{id}/extract")
    public ResponseEntity<Map<String, Object>> extractClaim(
            @PathVariable Long id,
            @RequestParam(required = false) ProcessingMode mode) {
        log.info("POST /api/claims/{}/extract - mode: {}", id, mode);
        
        Claim claim = claimService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + id));
        
        ProcessingMode effectiveMode = mode != null ? mode : processingConfig.getDefaultMode();
        
        var result = claimProcessingService.extract(claim, effectiveMode, java.util.UUID.randomUUID().toString());
        
        return ResponseEntity.ok(Map.of(
                "claim_id", id,
                "status", claim.getWorkflowStatus(),
                "mode", result.getMode(),
                "ai_available", result.isAiAvailable(),
                "data", result.getData() != null ? result.getData() : "manual_required",
                "warnings", result.getWarnings(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/api/claims/{id}/set-mode")
    public ResponseEntity<Map<String, Object>> setProcessingMode(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        log.info("POST /api/claims/{}/set-mode", id);
        
        String modeStr = body.get("mode");
        ProcessingMode mode = ProcessingMode.valueOf(modeStr);
        
        Claim claim = claimService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + id));
        
        log.info("Claim {} processing mode set to {}", id, mode);
        
        return ResponseEntity.ok(Map.of(
                "claim_id", id,
                "mode", mode,
                "message", "Processing mode updated successfully",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/api/claims/{id}/processing-status")
    public ResponseEntity<Map<String, Object>> getProcessingStatus(@PathVariable Long id) {
        log.info("GET /api/claims/{}/processing-status", id);
        
        Claim claim = claimService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + id));
        
        return ResponseEntity.ok(Map.of(
                "claim_id", id,
                "status", claim.getWorkflowStatus(),
                "current_mode", processingConfig.getDefaultMode(),
                "ai_available", aiOrchestrationService.isAIAvailable(),
                "ai_circuit_breaker_state", circuitBreaker.getState().name(),
                "confidence_score", claim.getAiConfidenceScore() != null 
                        ? claim.getAiConfidenceScore() : 0.0,
                "stp_threshold", processingConfig.getStpConfidenceThreshold(),
                "hitl_threshold", processingConfig.getHitlConfidenceThreshold(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/api/claims/{id}/override")
    public ResponseEntity<Map<String, Object>> overrideClaim(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        log.info("POST /api/claims/{}/override", id);
        
        Claim claim = claimService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + id));
        
        String targetStatus = body.get("status");
        String reason = body.getOrDefault("reason", "Manual override");
        
        Claim.WorkflowStatus newStatus = Claim.WorkflowStatus.valueOf(targetStatus);
        claim.setWorkflowStatus(newStatus);
        claimService.save(claim);
        
        log.info("Claim {} manually overridden to {} with reason: {}", id, newStatus, reason);
        
        return ResponseEntity.ok(Map.of(
                "claim_id", id,
                "previous_status", claim.getWorkflowStatus(),
                "new_status", newStatus,
                "reason", reason,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/api/system/processing-mode")
    public ResponseEntity<Map<String, Object>> getGlobalProcessingMode() {
        log.info("GET /api/system/processing-mode");
        
        return ResponseEntity.ok(Map.of(
                "default_mode", processingConfig.getDefaultMode(),
                "ai_enabled", processingConfig.isAiEnabled(),
                "ai_available", aiOrchestrationService.isAIAvailable(),
                "ai_circuit_breaker_state", circuitBreaker.getState().name(),
                "stp_confidence_threshold", processingConfig.getStpConfidenceThreshold(),
                "hitl_confidence_threshold", processingConfig.getHitlConfidenceThreshold(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @PutMapping("/api/system/processing-mode")
    public ResponseEntity<Map<String, Object>> updateGlobalProcessingMode(@RequestBody Map<String, Object> body) {
        log.info("PUT /api/system/processing-mode");
        
        String modeStr = (String) body.get("default-mode");
        Boolean aiEnabled = (Boolean) body.get("ai-enabled");
        
        if (modeStr != null) {
            ProcessingMode newMode = ProcessingMode.valueOf(modeStr);
            processingConfig.setDefaultMode(newMode);
            log.info("Global processing mode updated to {}", newMode);
        }
        
        if (aiEnabled != null) {
            processingConfig.setAiEnabled(aiEnabled);
            log.info("AI enabled set to {}", aiEnabled);
        }
        
        return ResponseEntity.ok(Map.of(
                "default_mode", processingConfig.getDefaultMode(),
                "ai_enabled", processingConfig.isAiEnabled(),
                "message", "Processing mode updated successfully",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}