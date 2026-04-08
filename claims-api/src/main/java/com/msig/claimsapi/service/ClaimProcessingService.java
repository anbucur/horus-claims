package com.msig.claimsapi.service;

import com.msig.claimsapi.config.ProcessingConfig;
import com.msig.claimsapi.event.ClaimApprovedSTPCEvent;
import com.msig.claimsapi.event.ClaimRoutedToHITLEvent;
import com.msig.claimsapi.event.ClaimStepCompletedEvent;
import com.msig.claimsapi.repository.ClaimRepository;
import com.msig.claimsapi.service.ai.AIOrchestrationService;
import com.msig.claimsapi.service.workflow.ClaimWorkflowStateMachine;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Evidence;
import com.msig.claimsdomain.entities.Policy;
import com.msig.claimsdomain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimProcessingService {
    
    private final ClaimRepository claimRepository;
    private final AIOrchestrationService aiOrchestrationService;
    private final ClaimWorkflowStateMachine stateMachine;
    private final ProcessingConfig processingConfig;
    private final ApplicationEventPublisher eventPublisher;
    
    @Transactional
    public ProcessingResult<Claim> processClaim(Long claimId, ProcessingMode mode) {
        String traceId = UUID.randomUUID().toString();
        log.info("Starting claim processing for claim {} with mode {} (traceId: {})", claimId, mode, traceId);
        
        Claim claim = claimRepository.findByIdWithPolicy(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
        
        ProcessingMode effectiveMode = determineEffectiveMode(mode);
        
        try {
            ProcessingResult<ExtractedClaimData> extractionResult = extract(claim, effectiveMode, traceId);
            if (extractionResult.isAiAvailable() && extractionResult.getData() != null) {
                claim.setAiConfidenceScore(extractionResult.getData().getConfidenceScore());
            }
            
            ProcessingResult<PolicyVerificationResult> verifyResult = verifyPolicy(claim, effectiveMode, traceId);
            
            ProcessingResult<EntityMatchResult> entityResult = matchEntities(claim, effectiveMode, traceId);
            
            ProcessingResult<ForensicsResult> forensicsResult = runForensics(claim, effectiveMode, traceId);
            
            ProcessingResult<List<DuplicateMatch>> duplicateResult = checkDuplicates(claim, effectiveMode, traceId);
            
            return route(claim, effectiveMode, traceId);
            
        } catch (Exception e) {
            log.error("Error processing claim {}: {}", claimId, e.getMessage(), e);
            claim.setWorkflowStatus(Claim.WorkflowStatus.HITL);
            claimRepository.save(claim);
            return ProcessingResult.<Claim>builder()
                    .data(claim)
                    .mode(effectiveMode)
                    .aiAvailable(aiOrchestrationService.isAIAvailable())
                    .warnings(List.of("Processing failed: " + e.getMessage()))
                    .traceId(traceId)
                    .build();
        }
    }
    
    @Transactional
    public ProcessingResult<ExtractedClaimData> extract(Claim claim, ProcessingMode mode, String traceId) {
        log.info("EXTRACT step for claim {} in mode {}", claim.getId(), mode);
        
        if (mode == ProcessingMode.FULL_MANUAL) {
            log.info("FULL_MANUAL mode: skipping AI extraction, routing to manual extraction");
            claim.setWorkflowStatus(Claim.WorkflowStatus.EXTRACTING);
            claimRepository.save(claim);
            return ProcessingResult.empty(mode);
        }
        
        FNOLDocument fnolDocument = FNOLDocument.builder()
                .claimId(claim.getId())
                .lossLocation(claim.getLossLocation())
                .incidentDescription(claim.getIncidentNarrative())
                .build();
        
        ProcessingResult<ExtractedClaimData> result = aiOrchestrationService.extractClaimData(fnolDocument);
        
        if (result.isAiAvailable() && result.getData() != null) {
            claim.setWorkflowStatus(Claim.WorkflowStatus.VERIFYING);
            claim.setAiConfidenceScore(result.getData().getConfidenceScore());
            claimRepository.save(claim);
            
            emitStepCompletedEvent(claim, "EXTRACTING", result, traceId);
        } else {
            log.warn("AI extraction returned no data for claim {}, routing to HITL", claim.getId());
            claim.setWorkflowStatus(Claim.WorkflowStatus.HITL);
            claimRepository.save(claim);
            emitRoutedToHITLEvent(claim, "AI extraction unavailable", traceId);
        }
        
        return result;
    }
    
    @Transactional
    public ProcessingResult<PolicyVerificationResult> verifyPolicy(Claim claim, ProcessingMode mode, String traceId) {
        log.info("VERIFY step for claim {} in mode {}", claim.getId(), mode);
        
        if (mode == ProcessingMode.FULL_MANUAL) {
            log.info("FULL_MANUAL mode: skipping AI policy verification");
            return ProcessingResult.empty(mode);
        }
        
        Policy policy = claim.getPolicy();
        if (policy == null) {
            return ProcessingResult.<PolicyVerificationResult>builder()
                    .data(PolicyVerificationResult.builder()
                            .policyActive(false)
                            .coverageValid(false)
                            .mismatchReason("Policy not found")
                            .confidenceScore(0.0)
                            .build())
                    .mode(mode)
                    .aiAvailable(false)
                    .traceId(traceId)
                    .build();
        }
        
        ProcessingResult<PolicyVerificationResult> result = aiOrchestrationService.verifyPolicy(claim, policy);
        
        if (result.isAiAvailable() && result.getData() != null) {
            claim.setWorkflowStatus(Claim.WorkflowStatus.VERIFYING);
            claimRepository.save(claim);
            emitStepCompletedEvent(claim, "VERIFYING", result, traceId);
        }
        
        return result;
    }
    
    @Transactional
    public ProcessingResult<EntityMatchResult> matchEntities(Claim claim, ProcessingMode mode, String traceId) {
        log.info("ENTITY_MATCHING step for claim {} in mode {}", claim.getId(), mode);
        
        if (mode == ProcessingMode.FULL_MANUAL) {
            log.info("FULL_MANUAL mode: skipping AI entity matching");
            return ProcessingResult.empty(mode);
        }
        
        ExtractedEntities entities = ExtractedEntities.builder()
                .assuredNames(Collections.emptyList())
                .brokerNames(Collections.emptyList())
                .vesselNames(Collections.emptyList())
                .build();
        
        ProcessingResult<EntityMatchResult> result = aiOrchestrationService.matchEntities(entities);
        
        if (result.isAiAvailable() && result.getData() != null) {
            emitStepCompletedEvent(claim, "ENTITY_MATCHING", result, traceId);
        }
        
        return result;
    }
    
    @Transactional
    public ProcessingResult<ForensicsResult> runForensics(Claim claim, ProcessingMode mode, String traceId) {
        log.info("FORENSICS step for claim {} in mode {}", claim.getId(), mode);
        
        if (mode == ProcessingMode.FULL_MANUAL) {
            log.info("FULL_MANUAL mode: skipping AI forensics");
            return ProcessingResult.empty(mode);
        }
        
        List<Evidence> evidence = claim.getEvidences();
        ProcessingResult<ForensicsResult> result = aiOrchestrationService.runForensics(evidence);
        
        if (result.isAiAvailable() && result.getData() != null) {
            emitStepCompletedEvent(claim, "FORENSICS", result, traceId);
            
            if (result.getData().isQuarantineRecommended()) {
                log.warn("Forensics recommends quarantine for evidence on claim {}", claim.getId());
            }
        }
        
        return result;
    }
    
    @Transactional
    public ProcessingResult<List<DuplicateMatch>> checkDuplicates(Claim claim, ProcessingMode mode, String traceId) {
        log.info("DUPLICATE_CHECK step for claim {} in mode {}", claim.getId(), mode);
        
        if (mode == ProcessingMode.FULL_MANUAL) {
            log.info("FULL_MANUAL mode: skipping AI duplicate detection");
            return ProcessingResult.<List<DuplicateMatch>>builder()
                    .data(Collections.emptyList())
                    .mode(mode)
                    .aiAvailable(false)
                    .traceId(traceId)
                    .build();
        }
        
        ProcessingResult<List<DuplicateMatch>> result = aiOrchestrationService.detectDuplicates(claim);
        
        if (result.isAiAvailable() && result.getData() != null && !result.getData().isEmpty()) {
            log.warn("Potential duplicates found for claim {}: {}", claim.getId(), result.getData().size());
            emitStepCompletedEvent(claim, "DUPLICATE_CHECK", result, traceId);
        }
        
        return result;
    }
    
    @Transactional
    public ProcessingResult<Claim> route(Claim claim, ProcessingMode mode, String traceId) {
        log.info("ROUTE step for claim {} in mode {}", claim.getId(), mode);
        
        Double confidenceScore = claim.getAiConfidenceScore();
        
        if (confidenceScore == null) {
            confidenceScore = 0.0;
        }
        
        if (stateMachine.requiresHumanReview(claim, mode, confidenceScore)) {
            claim.setWorkflowStatus(Claim.WorkflowStatus.HITL);
            claimRepository.save(claim);
            emitRoutedToHITLEvent(claim, "Below confidence threshold", traceId);
            return ProcessingResult.<Claim>builder()
                    .data(claim)
                    .mode(mode)
                    .aiAvailable(aiOrchestrationService.isAIAvailable())
                    .warnings(List.of("Routed to HITL: confidence below threshold"))
                    .traceId(traceId)
                    .build();
        }
        
        if (confidenceScore >= processingConfig.getStpConfidenceThreshold()) {
            claim.setWorkflowStatus(Claim.WorkflowStatus.STP);
            claimRepository.save(claim);
            emitApprovedSTPCEvent(claim, traceId);
            return ProcessingResult.<Claim>builder()
                    .data(claim)
                    .mode(mode)
                    .aiAvailable(aiOrchestrationService.isAIAvailable())
                    .traceId(traceId)
                    .build();
        }
        
        claim.setWorkflowStatus(Claim.WorkflowStatus.HITL);
        claimRepository.save(claim);
        emitRoutedToHITLEvent(claim, "Confidence between thresholds", traceId);
        return ProcessingResult.<Claim>builder()
                .data(claim)
                .mode(mode)
                .aiAvailable(aiOrchestrationService.isAIAvailable())
                .warnings(List.of("Routed to HITL: confidence in intermediate range"))
                .traceId(traceId)
                .build();
    }
    
    private ProcessingMode determineEffectiveMode(ProcessingMode requested) {
        if (requested != null) {
            return requested;
        }
        return processingConfig.getDefaultMode();
    }
    
    private void emitStepCompletedEvent(Claim claim, String stepName, ProcessingResult<?> result, String traceId) {
        ClaimStepCompletedEvent event = ClaimStepCompletedEvent.builder()
                .claimId(claim.getId())
                .stepName(stepName)
                .mode(result.getMode())
                .aiAvailable(result.isAiAvailable())
                .confidenceScore(claim.getAiConfidenceScore())
                .timestamp(LocalDateTime.now())
                .traceId(traceId)
                .build();
        eventPublisher.publishEvent(event);
        log.debug("Published ClaimStepCompletedEvent for claim {} step {}", claim.getId(), stepName);
    }
    
    private void emitRoutedToHITLEvent(Claim claim, String reason, String traceId) {
        ClaimRoutedToHITLEvent event = ClaimRoutedToHITLEvent.builder()
                .claimId(claim.getId())
                .reason(reason)
                .confidenceScore(claim.getAiConfidenceScore())
                .timestamp(LocalDateTime.now())
                .traceId(traceId)
                .build();
        eventPublisher.publishEvent(event);
        log.info("Published ClaimRoutedToHITLEvent for claim {}: {}", claim.getId(), reason);
    }
    
    private void emitApprovedSTPCEvent(Claim claim, String traceId) {
        ClaimApprovedSTPCEvent event = ClaimApprovedSTPCEvent.builder()
                .claimId(claim.getId())
                .confidenceScore(claim.getAiConfidenceScore())
                .timestamp(LocalDateTime.now())
                .traceId(traceId)
                .build();
        eventPublisher.publishEvent(event);
        log.info("Published ClaimApprovedSTPCEvent for claim {} with confidence {}", 
                claim.getId(), claim.getAiConfidenceScore());
    }
}