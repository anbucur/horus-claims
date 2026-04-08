package com.msig.claimsapi.service.settlement;

import com.msig.claimsapi.event.ClaimSettledEvent;
import com.msig.claimsapi.repository.ClaimRepository;
import com.msig.claimsapi.service.audit.ClaimAuditService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Claim.WorkflowStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimCompletionService {

    private final ClaimRepository claimRepository;
    private final ClaimAuditService auditService;
    private final ClaimSettlementService settlementService;
    private final ApplicationEventPublisher eventPublisher;

    public SettlementCalculation calculateSettlement(Long claimId) {
        ClaimSettlementService.SettlementSummary summary = settlementService.getSettlementSummary(claimId);
        return new SettlementCalculation(
                summary.totalReserveAmount(),
                summary.totalApprovedAmount(),
                summary.totalPaidAmount(),
                summary.netAmountOutstanding(),
                null
        );
    }

    @Transactional
    public void finaliseClaim(Long claimId, String performedBy) {
        String traceId = UUID.randomUUID().toString();
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));

        ClaimSettlementService.SettlementSummary summary = settlementService.getSettlementSummary(claimId);

        List<com.msig.claimsdomain.entities.Financials> financials =
                claim.getFinancials();

        String currency = financials.isEmpty() ? "USD" :
                financials.stream()
                        .filter(f -> f.getCurrency() != null)
                        .findFirst()
                        .map(com.msig.claimsdomain.entities.Financials::getCurrency)
                        .orElse("USD");

        claim.setSettlementAmount(summary.netAmountOutstanding());
        claim.setSettlementCurrency(currency);
        claim.setSettledAt(LocalDateTime.now());

        WorkflowStatus fromStatus = claim.getWorkflowStatus();
        claim.setWorkflowStatus(WorkflowStatus.COMPLETED);
        claimRepository.save(claim);

        auditService.logStatusChange(claim, fromStatus, WorkflowStatus.COMPLETED,
                performedBy, "Claim finalised via settlement");

        ClaimSettledEvent event = ClaimSettledEvent.builder()
                .claimId(claimId)
                .settlementAmount(summary.netAmountOutstanding())
                .currency(currency)
                .settledAt(LocalDateTime.now())
                .traceId(traceId)
                .build();
        eventPublisher.publishEvent(event);

        log.info("Claim {} finalised with settlement amount {} {}",
                claimId, summary.netAmountOutstanding(), currency);
    }

    public record SettlementCalculation(
            BigDecimal totalReserves,
            BigDecimal totalApproved,
            BigDecimal totalPaid,
            BigDecimal netAmount,
            String currency
    ) {}
}
