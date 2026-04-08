package com.msig.claimsapi.service;

import com.msig.claimsapi.repository.ClaimRepository;
import com.msig.claimsapi.repository.FinancialsRepository;
import com.msig.claimsapi.repository.SettlementActionRepository;
import com.msig.claimsapi.service.audit.ClaimAuditService;
import com.msig.claimsapi.service.settlement.ClaimSettlementService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Claim.WorkflowStatus;
import com.msig.claimsdomain.entities.Financials;
import com.msig.claimsdomain.entities.Policy;
import com.msig.claimsdomain.entities.SettlementAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimSettlementServiceTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private FinancialsRepository financialsRepository;
    @Mock
    private SettlementActionRepository settlementActionRepository;
    @Mock
    private ClaimAuditService auditService;

    private ClaimSettlementService settlementService;
    private Claim sampleClaim;
    private Financials sampleReserve;

    @BeforeEach
    void setUp() {
        settlementService = new ClaimSettlementService(
                claimRepository, financialsRepository, settlementActionRepository, auditService);

        Policy policy = Policy.builder().id(1L).policyNumber("POL-001").build();
        sampleClaim = Claim.builder()
                .id(1L)
                .policy(policy)
                .dateOfLoss(LocalDate.of(2024, 1, 15))
                .workflowStatus(WorkflowStatus.STP)
                .build();

        sampleReserve = Financials.builder()
                .id(10L)
                .claim(sampleClaim)
                .category(Financials.Category.INDEMNITY)
                .amount(new BigDecimal("50000.00"))
                .currency("USD")
                .status(Financials.TransactionStatus.RESERVE)
                .transactionDate(LocalDate.now())
                .build();
    }

    @Test
    void createReserve_whenClaimExists_createsReserve() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(sampleClaim));
        when(financialsRepository.save(any(Financials.class))).thenAnswer(i -> {
            Financials f = i.getArgument(0);
            f.setId(10L);
            return f;
        });
        when(settlementActionRepository.save(any(SettlementAction.class)))
                .thenAnswer(i -> i.getArgument(0));

        Financials result = settlementService.createReserve(
                1L,
                new BigDecimal("50000.00"),
                "USD",
                Financials.Category.INDEMNITY,
                "adjuster1"
        );

        assertNotNull(result);
        assertEquals(new BigDecimal("50000.00"), result.getAmount());
        assertEquals("USD", result.getCurrency());
        assertEquals(Financials.TransactionStatus.RESERVE, result.getStatus());
        verify(financialsRepository).save(any(Financials.class));
        verify(settlementActionRepository).save(any(SettlementAction.class));
    }

    @Test
    void createReserve_whenClaimNotFound_throwsException() {
        when(claimRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> settlementService.createReserve(999L, new BigDecimal("1000"), "USD",
                        Financials.Category.INDEMNITY, "adjuster1"));
    }

    @Test
    void getSettlementSummary_calculatesCorrectTotals() {
        Financials reserve = Financials.builder()
                .id(1L).claim(sampleClaim)
                .amount(new BigDecimal("50000.00"))
                .status(Financials.TransactionStatus.RESERVE)
                .build();

        Financials payment = Financials.builder()
                .id(2L).claim(sampleClaim)
                .amount(new BigDecimal("45000.00"))
                .status(Financials.TransactionStatus.PAYMENT)
                .approvedAt(LocalDateTime.now())
                .paidAt(null)
                .build();

        when(financialsRepository.findByClaimId(1L))
                .thenReturn(List.of(reserve, payment));

        ClaimSettlementService.SettlementSummary summary = settlementService.getSettlementSummary(1L);

        assertEquals(new BigDecimal("50000.00"), summary.totalReserveAmount());
        assertEquals(new BigDecimal("45000.00"), summary.totalApprovedAmount());
        assertEquals(BigDecimal.ZERO, summary.totalPaidAmount());
        assertEquals(new BigDecimal("45000.00"), summary.netAmountOutstanding());
    }

    @Test
    void approveReserve_whenReserveExists_createsPayment() {
        when(financialsRepository.findById(10L)).thenReturn(Optional.of(sampleReserve));
        when(financialsRepository.save(any(Financials.class))).thenAnswer(i -> {
            Financials f = i.getArgument(0);
            f.setId(20L);
            return f;
        });
        when(settlementActionRepository.save(any(SettlementAction.class)))
                .thenAnswer(i -> i.getArgument(0));

        Financials result = settlementService.approveReserve(10L, new BigDecimal("48000.00"), "manager1");

        assertNotNull(result);
        assertEquals(new BigDecimal("48000.00"), result.getAmount());
        assertEquals(Financials.TransactionStatus.PAYMENT, result.getStatus());
        assertEquals("manager1", result.getApprovedBy());
        verify(settlementActionRepository).save(any(SettlementAction.class));
    }

    @Test
    void approveReserve_whenReserveNotFound_throwsException() {
        when(financialsRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> settlementService.approveReserve(999L, new BigDecimal("1000"), "manager1"));
    }

    @Test
    void getReserves_returnsReserveList() {
        when(financialsRepository.findByClaimIdAndStatus(1L, Financials.TransactionStatus.RESERVE))
                .thenReturn(List.of(sampleReserve));

        List<Financials> result = settlementService.getReserves(1L);

        assertEquals(1, result.size());
        assertEquals(Financials.TransactionStatus.RESERVE, result.get(0).getStatus());
    }

    @Test
    void processPayment_whenClaimExists_marksAsCompleted() {
        Financials payment = Financials.builder()
                .id(2L).claim(sampleClaim)
                .amount(new BigDecimal("50000.00"))
                .currency("USD")
                .status(Financials.TransactionStatus.PAYMENT)
                .paidAt(null)
                .build();

        when(claimRepository.findById(1L)).thenReturn(Optional.of(sampleClaim));
        when(financialsRepository.findByClaimIdAndStatus(1L, Financials.TransactionStatus.PAYMENT))
                .thenReturn(List.of(payment));
        when(financialsRepository.save(any(Financials.class))).thenAnswer(i -> i.getArgument(0));
        when(financialsRepository.findByClaimId(1L)).thenReturn(List.of(payment));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));
        when(settlementActionRepository.save(any(SettlementAction.class)))
                .thenAnswer(i -> i.getArgument(0));

        Claim result = settlementService.processPayment(1L, "settlement-clerk");

        assertEquals(WorkflowStatus.COMPLETED, result.getWorkflowStatus());
        assertNotNull(result.getSettledAt());
        verify(auditService).logStatusChange(any(Claim.class), eq(WorkflowStatus.STP),
                eq(WorkflowStatus.COMPLETED), eq("settlement-clerk"), anyString());
    }

    @Test
    void rejectSettlement_whenClaimExists_marksAsRejected() {
        when(claimRepository.findById(1L)).thenReturn(Optional.of(sampleClaim));
        when(claimRepository.save(any(Claim.class))).thenAnswer(i -> i.getArgument(0));
        when(settlementActionRepository.save(any(SettlementAction.class)))
                .thenAnswer(i -> i.getArgument(0));

        Claim result = settlementService.rejectSettlement(1L, "Insufficient documentation", "manager1");

        assertEquals(WorkflowStatus.REJECTED, result.getWorkflowStatus());
        assertEquals("Insufficient documentation", result.getRejectionReason());
        verify(auditService).logStatusChange(any(Claim.class), eq(WorkflowStatus.STP),
                eq(WorkflowStatus.REJECTED), eq("manager1"), eq("Insufficient documentation"));
    }
}
