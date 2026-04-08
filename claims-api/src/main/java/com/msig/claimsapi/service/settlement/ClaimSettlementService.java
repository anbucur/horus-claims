package com.msig.claimsapi.service.settlement;

import com.msig.claimsapi.repository.ClaimRepository;
import com.msig.claimsapi.repository.FinancialsRepository;
import com.msig.claimsapi.repository.SettlementActionRepository;
import com.msig.claimsapi.service.audit.ClaimAuditService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Claim.WorkflowStatus;
import com.msig.claimsdomain.entities.Financials;
import com.msig.claimsdomain.entities.SettlementAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimSettlementService {

    private final ClaimRepository claimRepository;
    private final FinancialsRepository financialsRepository;
    private final SettlementActionRepository settlementActionRepository;
    private final ClaimAuditService auditService;

    @Transactional
    public Financials createReserve(Long claimId, BigDecimal amount, String currency,
                                   Financials.Category category, String performedBy) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));

        Financials reserve = Financials.builder()
                .claim(claim)
                .category(category)
                .amount(amount)
                .currency(currency)
                .status(Financials.TransactionStatus.RESERVE)
                .transactionDate(LocalDate.now())
                .build();
        Financials saved = financialsRepository.save(reserve);

        SettlementAction action = SettlementAction.builder()
                .claim(claim)
                .action("RESERVE_CREATED")
                .amount(amount)
                .currency(currency)
                .performedBy(performedBy)
                .notes("Reserve created for category: " + category)
                .build();
        settlementActionRepository.save(action);

        log.info("Created reserve {} {} for claim {} by {}",
                amount, currency, claimId, performedBy);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Financials> getReserves(Long claimId) {
        return financialsRepository.findByClaimIdAndStatus(claimId, Financials.TransactionStatus.RESERVE);
    }

    @Transactional
    public Financials approveReserve(Long financialsId, BigDecimal approvedAmount, String performedBy) {
        Financials reserve = financialsRepository.findById(financialsId)
                .orElseThrow(() -> new IllegalArgumentException("Reserve not found: " + financialsId));

        Claim claim = reserve.getClaim();

        Financials payment = Financials.builder()
                .claim(claim)
                .category(reserve.getCategory())
                .amount(approvedAmount)
                .currency(reserve.getCurrency())
                .status(Financials.TransactionStatus.PAYMENT)
                .transactionDate(LocalDate.now())
                .approvedBy(performedBy)
                .approvedAt(LocalDateTime.now())
                .build();
        Financials saved = financialsRepository.save(payment);

        SettlementAction action = SettlementAction.builder()
                .claim(claim)
                .action("RESERVE_APPROVED")
                .amount(approvedAmount)
                .currency(reserve.getCurrency())
                .performedBy(performedBy)
                .notes("Approved from reserve " + reserve.getId())
                .build();
        settlementActionRepository.save(action);

        log.info("Approved reserve {} as payment {} by {}",
                financialsId, approvedAmount, performedBy);
        return saved;
    }

    @Transactional(readOnly = true)
    public SettlementSummary getSettlementSummary(Long claimId) {
        List<Financials> all = financialsRepository.findByClaimId(claimId);

        BigDecimal totalReserve = all.stream()
                .filter(f -> f.getStatus() == Financials.TransactionStatus.RESERVE)
                .map(Financials::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalApproved = all.stream()
                .filter(f -> f.getStatus() == Financials.TransactionStatus.PAYMENT && f.getApprovedAt() != null)
                .map(Financials::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = all.stream()
                .filter(f -> f.getStatus() == Financials.TransactionStatus.PAYMENT && f.getPaidAt() != null)
                .map(Financials::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netOutstanding = totalApproved.subtract(totalPaid);

        return new SettlementSummary(totalReserve, totalApproved, totalPaid, netOutstanding);
    }

    @Transactional
    public Claim processPayment(Long claimId, String performedBy) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));

        List<Financials> payments = financialsRepository
                .findByClaimIdAndStatus(claimId, Financials.TransactionStatus.PAYMENT);

        for (Financials p : payments) {
            if (p.getPaidAt() == null) {
                p.setPaidAt(LocalDateTime.now());
                financialsRepository.save(p);
            }
        }

        SettlementSummary summary = getSettlementSummary(claimId);

        claim.setSettlementAmount(summary.netAmountOutstanding());
        claim.setSettlementCurrency(payments.isEmpty() ? null : payments.get(0).getCurrency());
        claim.setSettledAt(LocalDateTime.now());

        WorkflowStatus fromStatus = claim.getWorkflowStatus();
        claim.setWorkflowStatus(WorkflowStatus.COMPLETED);
        claimRepository.save(claim);

        auditService.logStatusChange(claim, fromStatus, WorkflowStatus.COMPLETED, performedBy,
                "Settlement processed");

        SettlementAction action = SettlementAction.builder()
                .claim(claim)
                .action("PAYMENT_PROCESSED")
                .amount(summary.netAmountOutstanding())
                .currency(payments.isEmpty() ? null : payments.get(0).getCurrency())
                .performedBy(performedBy)
                .notes("Final settlement payment processed")
                .build();
        settlementActionRepository.save(action);

        log.info("Processed settlement payment for claim {} by {}", claimId, performedBy);
        return claim;
    }

    @Transactional
    public Claim rejectSettlement(Long claimId, String reason, String performedBy) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));

        WorkflowStatus fromStatus = claim.getWorkflowStatus();
        claim.setRejectionReason(reason);
        claim.setWorkflowStatus(WorkflowStatus.REJECTED);
        claimRepository.save(claim);

        auditService.logStatusChange(claim, fromStatus, WorkflowStatus.REJECTED, performedBy, reason);

        SettlementAction action = SettlementAction.builder()
                .claim(claim)
                .action("SETTLEMENT_REJECTED")
                .performedBy(performedBy)
                .notes(reason)
                .build();
        settlementActionRepository.save(action);

        log.info("Rejected settlement for claim {} by {}: {}", claimId, performedBy, reason);
        return claim;
    }

    public record SettlementSummary(
            BigDecimal totalReserveAmount,
            BigDecimal totalApprovedAmount,
            BigDecimal totalPaidAmount,
            BigDecimal netAmountOutstanding
    ) {}
}
