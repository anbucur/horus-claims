package com.msig.claimsapi.service;

import com.msig.claimsapi.repository.ClaimRepository;
import com.msig.claimsapi.service.audit.ClaimAuditService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Claim.WorkflowStatus;
import com.msig.claimsdomain.entities.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;

    @Mock
    private ClaimAuditService auditService;

    @InjectMocks
    private ClaimService claimService;

    private Claim sampleClaim;

    @BeforeEach
    void setUp() {
        Policy policy = Policy.builder()
                .id(1L)
                .policyNumber("POL-001")
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .expirationDate(LocalDate.of(2025, 1, 1))
                .lineOfBusiness("Marine Hull")
                .status(Policy.PolicyStatus.ACTIVE)
                .build();

        sampleClaim = Claim.builder()
                .id(1L)
                .policy(policy)
                .dateOfLoss(LocalDate.of(2024, 1, 15))
                .incidentNarrative("Test incident")
                .lossLocation("Test location")
                .workflowStatus(WorkflowStatus.RECEIVED)
                .build();
    }

    @Test
    void findById_whenExists_returnsClaim() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(sampleClaim));

        Optional<Claim> result = claimService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(claimRepository).findById(1L);
    }

    @Test
    void findById_whenNotExists_returnsEmpty() {
        when(claimRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Claim> result = claimService.findById(999L);

        assertTrue(result.isEmpty());
        verify(claimRepository).findById(999L);
    }

    @Test
    void save_persistsClaim() {
        when(claimRepository.save(any(Claim.class))).thenReturn(sampleClaim);

        Claim saved = claimService.save(sampleClaim);

        assertNotNull(saved);
        assertEquals(1L, saved.getId());
        verify(claimRepository).save(sampleClaim);
    }

    @Test
    void updateWorkflowStatus_whenClaimExists_updatesStatus() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(sampleClaim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));

        Claim updated = claimService.updateWorkflowStatus(1L, WorkflowStatus.EXTRACTING);

        assertEquals(WorkflowStatus.EXTRACTING, updated.getWorkflowStatus());
        verify(auditService).logStatusChange(any(Claim.class), eq(WorkflowStatus.RECEIVED), eq(WorkflowStatus.EXTRACTING), isNull(), isNull());
    }

    @Test
    void updateWorkflowStatus_whenClaimNotFound_throwsException() {
        when(claimRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> claimService.updateWorkflowStatus(999L, WorkflowStatus.EXTRACTING));
    }

    @Test
    void updateAiConfidenceScore_whenClaimExists_updatesScore() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(sampleClaim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));

        Claim updated = claimService.updateAiConfidenceScore(1L, 92.5);

        assertEquals(92.5, updated.getAiConfidenceScore());
        verify(claimRepository).save(sampleClaim);
    }

    @Test
    void updateAiConfidenceScore_whenClaimNotFound_throwsException() {
        when(claimRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> claimService.updateAiConfidenceScore(999L, 92.5));
    }

    @Test
    void findAll_returnsAllClaims() {
        Claim claim2 = Claim.builder().id(2L).workflowStatus(WorkflowStatus.RECEIVED).build();
        when(claimRepository.findAll()).thenReturn(List.of(sampleClaim, claim2));

        List<Claim> result = claimService.findAll();

        assertEquals(2, result.size());
        verify(claimRepository).findAll();
    }

    @Test
    void findByWorkflowStatus_filtersCorrectly() {
        when(claimRepository.findByWorkflowStatus(WorkflowStatus.RECEIVED))
                .thenReturn(List.of(sampleClaim));

        List<Claim> result = claimService.findByWorkflowStatus(WorkflowStatus.RECEIVED);

        assertEquals(1, result.size());
        assertEquals(WorkflowStatus.RECEIVED, result.get(0).getWorkflowStatus());
    }

    @Test
    void deleteById_callsRepository() {
        doNothing().when(claimRepository).deleteById(1L);

        claimService.deleteById(1L);

        verify(claimRepository).deleteById(1L);
    }
}
