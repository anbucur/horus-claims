package com.msig.claimsapi.service;

import com.msig.claimsapi.repository.ClaimRepository;
import com.msig.claimsapi.repository.ClaimReviewRepository;
import com.msig.claimsapi.service.audit.ClaimAuditService;
import com.msig.claimsapi.service.settlement.ClaimSettlementService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Claim.WorkflowStatus;
import com.msig.claimsdomain.entities.ClaimReview;
import com.msig.claimsdomain.model.ClaimReviewAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimReviewService {

    private final ClaimReviewRepository reviewRepository;
    private final ClaimRepository claimRepository;
    private final ClaimAuditService auditService;
    private final ClaimSettlementService settlementService;

    @Transactional
    public ClaimReview submitReview(Long claimId, ClaimReviewAction action, String notes, String reviewer) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));

        ClaimReview review = ClaimReview.builder()
                .claim(claim)
                .action(action)
                .reviewerNotes(notes)
                .reviewer(reviewer)
                .build();
        ClaimReview saved = reviewRepository.save(review);

        WorkflowStatus fromStatus = claim.getWorkflowStatus();

        switch (action) {
            case APPROVE -> {
                claim.setWorkflowStatus(WorkflowStatus.APPROVED);
                claimRepository.save(claim);
                auditService.logStatusChange(claim, fromStatus, WorkflowStatus.APPROVED, reviewer,
                        "Approved via HITL review: " + notes);
                log.info("Claim {} approved via HITL review by {}", claimId, reviewer);
            }
            case REJECT -> {
                claim.setWorkflowStatus(WorkflowStatus.REJECTED);
                claim.setRejectionReason(notes);
                claimRepository.save(claim);
                auditService.logStatusChange(claim, fromStatus, WorkflowStatus.REJECTED, reviewer, notes);
                log.info("Claim {} rejected via HITL review by {}", claimId, reviewer);
            }
            case ESCALATE, REQUEST_INFO -> {
                auditService.logStatusChange(claim, fromStatus, WorkflowStatus.HITL, reviewer,
                        action.name() + ": " + notes);
                log.info("Claim {} {} by {}", claimId, action.name().toLowerCase(), reviewer);
            }
        }

        return saved;
    }
}
