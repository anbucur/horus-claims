package com.msig.claimsapi.service;

import com.msig.claimsapi.dto.FnolIntakeRequest;
import com.msig.claimsapi.dto.FnolIntakeResponse;
import com.msig.claimsapi.event.ClaimSubmittedEvent;
import com.msig.claimsapi.repository.ClaimRepository;
import com.msig.claimsapi.repository.EvidenceRepository;
import com.msig.claimsapi.repository.PolicyRepository;
import com.msig.claimsapi.service.audit.ClaimAuditService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Claim.WorkflowStatus;
import com.msig.claimsdomain.entities.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FnolIntakeServiceTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private PolicyRepository policyRepository;
    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private ClaimAuditService auditService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private FnolIntakeService fnolIntakeService;

    private Policy samplePolicy;
    private FnolIntakeRequest validRequest;

    @BeforeEach
    void setUp() {
        fnolIntakeService = new FnolIntakeService(
                claimRepository, policyRepository, evidenceRepository, auditService, eventPublisher);

        samplePolicy = Policy.builder()
                .id(1L)
                .policyNumber("POL-001")
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .expirationDate(LocalDate.of(2025, 1, 1))
                .lineOfBusiness("Marine Hull")
                .status(Policy.PolicyStatus.ACTIVE)
                .build();

        validRequest = FnolIntakeRequest.builder()
                .policyNumber("POL-001")
                .dateOfLoss(LocalDate.of(2024, 1, 15))
                .incidentNarrative("Cargo damage during transit")
                .lossLocation("Port of Singapore")
                .submittedBy("intake-agent")
                .build();
    }

    @Test
    void submitIntake_happyPath_createsClaim() {
        when(policyRepository.findByPolicyNumber("POL-001")).thenReturn(Optional.of(samplePolicy));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> {
            Claim c = i.getArgument(0);
            c.setId(100L);
            return c;
        });
        when(auditService.logStatusChange(any(), any(), any(), any(), any())).thenReturn(null);
        doNothing().when(eventPublisher).publishEvent(any(ClaimSubmittedEvent.class));

        FnolIntakeResponse response = fnolIntakeService.submitIntake(validRequest);

        assertNotNull(response);
        assertEquals(100L, response.getClaimId());
        assertEquals(WorkflowStatus.RECEIVED.name(), response.getStatus());
        assertEquals("POL-001", response.getPolicyNumber());
        assertNotNull(response.getTraceId());

        verify(claimRepository).save(any(Claim.class));
        verify(auditService).logStatusChange(any(Claim.class), isNull(), eq(WorkflowStatus.RECEIVED),
                eq("intake-agent"), eq("FNOL intake submitted"));
        verify(eventPublisher).publishEvent(any(ClaimSubmittedEvent.class));
    }

    @Test
    void submitIntake_withEvidenceUrls_savesEvidence() {
        FnolIntakeRequest requestWithEvidence = FnolIntakeRequest.builder()
                .policyNumber("POL-001")
                .dateOfLoss(LocalDate.of(2024, 1, 15))
                .incidentNarrative("Cargo damage")
                .lossLocation("Port of Singapore")
                .submittedBy("intake-agent")
                .evidenceUrls(List.of("https://storage.example.com/doc1.pdf",
                        "https://storage.example.com/doc2.pdf"))
                .build();

        when(policyRepository.findByPolicyNumber("POL-001")).thenReturn(Optional.of(samplePolicy));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> {
            Claim c = i.getArgument(0);
            c.setId(100L);
            return c;
        });
        when(evidenceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        fnolIntakeService.submitIntake(requestWithEvidence);

        verify(evidenceRepository, times(2)).save(any());
    }

    @Test
    void submitIntake_policyNotFound_throwsException() {
        when(policyRepository.findByPolicyNumber("INVALID-POL")).thenReturn(Optional.empty());

        FnolIntakeRequest invalidRequest = FnolIntakeRequest.builder()
                .policyNumber("INVALID-POL")
                .dateOfLoss(LocalDate.of(2024, 1, 15))
                .submittedBy("intake-agent")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fnolIntakeService.submitIntake(invalidRequest));

        assertTrue(ex.getMessage().contains("Policy not found"));
        verify(claimRepository, never()).save(any());
    }

    @Test
    void submitIntake_publishesCorrectEvent() {
        when(policyRepository.findByPolicyNumber("POL-001")).thenReturn(Optional.of(samplePolicy));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> {
            Claim c = i.getArgument(0);
            c.setId(100L);
            return c;
        });

        ArgumentCaptor<ClaimSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(ClaimSubmittedEvent.class);

        fnolIntakeService.submitIntake(validRequest);

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        ClaimSubmittedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(100L, publishedEvent.getClaimId());
        assertEquals("POL-001", publishedEvent.getPolicyNumber());
        assertNotNull(publishedEvent.getTraceId());
    }

    @Test
    void submitIntake_auditTrailRecordsCorrectStatus() {
        when(policyRepository.findByPolicyNumber("POL-001")).thenReturn(Optional.of(samplePolicy));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> {
            Claim c = i.getArgument(0);
            c.setId(100L);
            return c;
        });

        fnolIntakeService.submitIntake(validRequest);

        verify(auditService).logStatusChange(
                any(Claim.class),
                isNull(),
                eq(WorkflowStatus.RECEIVED),
                eq("intake-agent"),
                eq("FNOL intake submitted")
        );
    }
}
