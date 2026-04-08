package com.msig.claimsapi.service;

import com.msig.claimsapi.config.ProcessingConfig;
import com.msig.claimsapi.repository.ClaimRepository;
import com.msig.claimsapi.service.ai.AICircuitBreaker;
import com.msig.claimsapi.service.ai.AIOrchestrationService;
import com.msig.claimsapi.service.ai.TextSimilarityDuplicateDetector;
import com.msig.claimsapi.service.audit.ClaimAuditService;
import com.msig.claimsapi.service.workflow.ClaimWorkflowStateMachine;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Claim.WorkflowStatus;
import com.msig.claimsdomain.entities.Policy;
import com.msig.claimsdomain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimProcessingServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private TextSimilarityDuplicateDetector duplicateDetector;

    @Mock
    private AIOrchestrationService aiOrchestrationService;

    @Mock
    private AICircuitBreaker aiCircuitBreaker;

    @Mock
    private ClaimWorkflowStateMachine stateMachine;

    @Mock
    private ProcessingConfig processingConfig;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ClaimAuditService auditService;

    @InjectMocks
    private ClaimProcessingService service;

    private Claim claim;
    private Policy policy;

    @BeforeEach
    void setUp() {
        policy = Policy.builder()
                .id(1L)
                .policyNumber("POL-001")
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .expirationDate(LocalDate.of(2025, 1, 1))
                .status(Policy.PolicyStatus.ACTIVE)
                .build();

        claim = Claim.builder()
                .id(1L)
                .policy(policy)
                .dateOfLoss(LocalDate.of(2024, 3, 15))
                .incidentNarrative("Cargo damage during voyage")
                .lossLocation("Port of Singapore")
                .workflowStatus(WorkflowStatus.RECEIVED)
                .build();
    }

    @Test
    void processClaim_aiAvailable_routesToSTP() {
        claim.setAiConfidenceScore(90.0);
        when(claimRepository.findByIdWithPolicy(1L)).thenReturn(Optional.of(claim));
        when(aiCircuitBreaker.isOpen()).thenReturn(false);
        when(processingConfig.getDefaultMode()).thenReturn(ProcessingMode.AI_ASSISTED);

        // Mock extract returning high confidence
        ExtractedClaimData extractedData = ExtractedClaimData.builder()
                .confidenceScore(90.0).build();
        ProcessingResult<ExtractedClaimData> extractResult = ProcessingResult.<ExtractedClaimData>builder()
                .data(extractedData).mode(ProcessingMode.AI_ASSISTED).aiAvailable(true).build();
        when(aiOrchestrationService.extractClaimData(any())).thenReturn(extractResult);

        // Mock policy verification
        PolicyVerificationResult pvrResult = PolicyVerificationResult.builder()
                .policyActive(true).coverageValid(true).confidenceScore(0.9).build();
        ProcessingResult<PolicyVerificationResult> verifyResult = ProcessingResult.<PolicyVerificationResult>builder()
                .data(pvrResult).mode(ProcessingMode.AI_ASSISTED).aiAvailable(true).build();
        when(aiOrchestrationService.verifyPolicy(any(), any())).thenReturn(verifyResult);

        // Mock entity matching
        EntityMatchResult entityMatch = EntityMatchResult.builder().allEntitiesMatched(true).confidenceScore(0.9).build();
        ProcessingResult<EntityMatchResult> entityResult = ProcessingResult.<EntityMatchResult>builder()
                .data(entityMatch).mode(ProcessingMode.AI_ASSISTED).aiAvailable(true).build();
        when(aiOrchestrationService.matchEntities(any())).thenReturn(entityResult);

        // Mock forensics
        ForensicsResult forensicsData = ForensicsResult.builder().imagesAuthentic(true).overallScore(0.95).quarantineRecommended(false).build();
        ProcessingResult<ForensicsResult> forensicsResult = ProcessingResult.<ForensicsResult>builder()
                .data(forensicsData).mode(ProcessingMode.AI_ASSISTED).aiAvailable(true).build();
        when(aiOrchestrationService.runForensics(any())).thenReturn(forensicsResult);

        // Mock duplicate detection — no duplicates
        when(duplicateDetector.findDuplicates(any())).thenReturn(List.of());

        // Mock routing to STP
        when(stateMachine.requiresHumanReview(any(), any(), anyDouble())).thenReturn(false);
        when(processingConfig.getStpConfidenceThreshold()).thenReturn(85);
        when(claimRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProcessingResult<Claim> result = service.processClaim(1L, ProcessingMode.AI_ASSISTED);

        assertThat(result).isNotNull();
        verify(claimRepository, atLeastOnce()).save(any(Claim.class));
    }

    @Test
    void processClaim_lowConfidence_routesToHITL() {
        claim.setAiConfidenceScore(50.0);
        when(claimRepository.findByIdWithPolicy(1L)).thenReturn(Optional.of(claim));
        when(aiCircuitBreaker.isOpen()).thenReturn(false);
        when(processingConfig.getDefaultMode()).thenReturn(ProcessingMode.AI_ASSISTED);

        ExtractedClaimData extractedData = ExtractedClaimData.builder().confidenceScore(50.0).build();
        ProcessingResult<ExtractedClaimData> extractResult = ProcessingResult.<ExtractedClaimData>builder()
                .data(extractedData).mode(ProcessingMode.AI_ASSISTED).aiAvailable(true).build();
        when(aiOrchestrationService.extractClaimData(any())).thenReturn(extractResult);

        PolicyVerificationResult pvrResult = PolicyVerificationResult.builder().policyActive(true).coverageValid(true).build();
        when(aiOrchestrationService.verifyPolicy(any(), any())).thenReturn(
                ProcessingResult.<PolicyVerificationResult>builder().data(pvrResult).mode(ProcessingMode.AI_ASSISTED).aiAvailable(true).build());

        EntityMatchResult entityMatch = EntityMatchResult.builder().allEntitiesMatched(false).confidenceScore(0.5).build();
        when(aiOrchestrationService.matchEntities(any())).thenReturn(
                ProcessingResult.<EntityMatchResult>builder().data(entityMatch).mode(ProcessingMode.AI_ASSISTED).aiAvailable(true).build());

        ForensicsResult forensicsData = ForensicsResult.builder().imagesAuthentic(true).overallScore(0.5).quarantineRecommended(false).build();
        when(aiOrchestrationService.runForensics(any())).thenReturn(
                ProcessingResult.<ForensicsResult>builder().data(forensicsData).mode(ProcessingMode.AI_ASSISTED).aiAvailable(true).build());

        when(duplicateDetector.findDuplicates(any())).thenReturn(List.of());

        // Routing → HITL because confidence < threshold
        when(stateMachine.requiresHumanReview(any(), any(), anyDouble())).thenReturn(true);
        when(claimRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProcessingResult<Claim> result = service.processClaim(1L, ProcessingMode.AI_ASSISTED);

        assertThat(result).isNotNull();
        verify(claimRepository, atLeastOnce()).save(argThat(c -> c.getWorkflowStatus() == WorkflowStatus.HITL));
    }

    @Test
    void processClaim_fullManualMode_skipsAI() {
        when(claimRepository.findByIdWithPolicy(1L)).thenReturn(Optional.of(claim));
        when(aiCircuitBreaker.isOpen()).thenReturn(false);
        when(claimRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stateMachine.requiresHumanReview(any(), eq(ProcessingMode.FULL_MANUAL), anyDouble())).thenReturn(true);

        ProcessingResult<Claim> result = service.processClaim(1L, ProcessingMode.FULL_MANUAL);

        assertThat(result).isNotNull();
        // AI orchestration should not be called in FULL_MANUAL mode
        verifyNoInteractions(aiOrchestrationService);
    }

    @Test
    void processClaim_circuitBreakerOpen_routesToHITL() {
        when(claimRepository.findByIdWithPolicy(1L)).thenReturn(Optional.of(claim));
        when(aiCircuitBreaker.isOpen()).thenReturn(true);
        when(claimRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProcessingResult<Claim> result = service.processClaim(1L, ProcessingMode.AI_ASSISTED);

        assertThat(result.isAiAvailable()).isFalse();
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("circuit breaker"));
        verify(claimRepository).save(argThat(c -> c.getWorkflowStatus() == WorkflowStatus.HITL));
    }

    @Test
    void processClaim_aiThrowsException_routesToHITL() {
        when(claimRepository.findByIdWithPolicy(1L)).thenReturn(Optional.of(claim));
        when(aiCircuitBreaker.isOpen()).thenReturn(false);
        when(processingConfig.getDefaultMode()).thenReturn(ProcessingMode.AI_ASSISTED);
        when(aiOrchestrationService.extractClaimData(any())).thenThrow(new RuntimeException("AI timeout"));
        when(claimRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ProcessingResult<Claim> result = service.processClaim(1L, ProcessingMode.AI_ASSISTED);

        assertThat(result.getWarnings()).anyMatch(w -> w.contains("Processing failed"));
        verify(claimRepository).save(argThat(c -> c.getWorkflowStatus() == WorkflowStatus.HITL));
    }

    @Test
    void extract_aiAvailable_setsVerifyingStatus() {
        when(aiCircuitBreaker.isOpen()).thenReturn(false);

        ExtractedClaimData extractedData = ExtractedClaimData.builder()
                .confidenceScore(88.0).incidentNarrative("Cargo damaged").build();
        ProcessingResult<ExtractedClaimData> aiResult = ProcessingResult.<ExtractedClaimData>builder()
                .data(extractedData).mode(ProcessingMode.AI_ASSISTED).aiAvailable(true).build();
        when(aiOrchestrationService.extractClaimData(any())).thenReturn(aiResult);
        when(claimRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String traceId = UUID.randomUUID().toString();
        ProcessingResult<ExtractedClaimData> result = service.extract(claim, ProcessingMode.AI_ASSISTED, traceId);

        assertThat(result.isAiAvailable()).isTrue();
        assertThat(result.getData().getConfidenceScore()).isEqualTo(88.0);
        verify(claimRepository).save(argThat(c -> c.getWorkflowStatus() == WorkflowStatus.VERIFYING));
    }

    @Test
    void route_aboveThreshold_setsSTPStatus() {
        claim.setAiConfidenceScore(90.0);
        when(stateMachine.requiresHumanReview(any(), any(), anyDouble())).thenReturn(false);
        when(processingConfig.getStpConfidenceThreshold()).thenReturn(85);
        when(claimRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(aiOrchestrationService.isAIAvailable()).thenReturn(true);

        String traceId = UUID.randomUUID().toString();
        ProcessingResult<Claim> result = service.route(claim, ProcessingMode.AI_ASSISTED, traceId);

        assertThat(result.getData().getWorkflowStatus()).isEqualTo(WorkflowStatus.STP);
    }
}
