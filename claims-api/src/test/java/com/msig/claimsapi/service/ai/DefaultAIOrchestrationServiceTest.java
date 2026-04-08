package com.msig.claimsapi.service.ai;

import com.msig.claimsapi.service.SemanticSearchService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Evidence;
import com.msig.claimsdomain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultAIOrchestrationServiceTest {

    @Mock
    private AICircuitBreaker circuitBreaker;

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private ReActClaimAgent reactAgent;

    @Mock
    private AIClient aiClient;

    @InjectMocks
    private DefaultAIOrchestrationService service;

    private Claim claim;

    @BeforeEach
    void setUp() {
        claim = Claim.builder()
                .id(1L)
                .incidentNarrative("Cargo damage during voyage")
                .lossLocation("Port of Singapore")
                .build();
    }

    @Test
    void extractClaimData_delegatesToClient() {
        when(circuitBreaker.isOpen()).thenReturn(false);

        FNOLDocument doc = FNOLDocument.builder()
                .claimId(1L)
                .incidentDescription("Cargo damage during voyage")
                .lossLocation("Port of Singapore")
                .build();

        AIClient.ExtractedClaimData aiResult = new AIClient.ExtractedClaimData(
                "2024-03-15", "Cargo damage during voyage", "Port of Singapore",
                "USD 50000", "USD", 0.88, true, "gpt-4o", "trace-001");
        when(aiClient.extractClaimData(any())).thenReturn(aiResult);

        ProcessingResult<ExtractedClaimData> result = service.extractClaimData(doc);

        assertThat(result.isAiAvailable()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData().getConfidenceScore()).isEqualTo(0.88);
        verify(circuitBreaker).recordSuccess();
    }

    @Test
    void isAIAvailable_returnsTrueWhenCircuitClosed() {
        when(circuitBreaker.isOpen()).thenReturn(false);
        when(aiClient.isAvailable()).thenReturn(true);

        assertThat(service.isAIAvailable()).isTrue();
    }

    @Test
    void verifyPolicy_delegatesToClient() {
        when(circuitBreaker.isOpen()).thenReturn(false);

        com.msig.claimsdomain.entities.Policy policy = com.msig.claimsdomain.entities.Policy.builder()
                .id(1L)
                .policyNumber("POL-001")
                .build();
        claim.setPolicy(policy);

        AIClient.PolicyVerificationResult aiResult = new AIClient.PolicyVerificationResult(
                1L, "POL-001", "ACTIVE", "Marine Hull", "Full coverage", "5000", true, true, "gpt-4o");
        when(aiClient.verifyPolicy(anyString(), any())).thenReturn(aiResult);

        ProcessingResult<PolicyVerificationResult> result = service.verifyPolicy(claim, policy);

        assertThat(result.isAiAvailable()).isTrue();
        verify(circuitBreaker).recordSuccess();
    }

    @Test
    void runForensics_delegatesToClient() {
        when(circuitBreaker.isOpen()).thenReturn(false);

        AIClient.ForensicsResult aiResult = new AIClient.ForensicsResult(
                List.of(), 0.05, false, false, "No anomalies detected", true, true, "gpt-4o");
        when(aiClient.runForensics(any())).thenReturn(aiResult);

        ProcessingResult<ForensicsResult> result = service.runForensics(List.of());

        assertThat(result.isAiAvailable()).isTrue();
        assertThat(result.getData().isImagesAuthentic()).isTrue();
        verify(circuitBreaker).recordSuccess();
    }
}
