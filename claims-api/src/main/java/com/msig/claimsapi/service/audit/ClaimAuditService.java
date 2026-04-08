package com.msig.claimsapi.service.audit;

import com.msig.claimsapi.repository.ClaimStatusHistoryRepository;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.ClaimStatusHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimAuditService {

    private final ClaimStatusHistoryRepository statusHistoryRepository;

    @Transactional
    public ClaimStatusHistory logStatusChange(Claim claim, Claim.WorkflowStatus fromStatus,
                                              Claim.WorkflowStatus toStatus, String changedBy, String reason) {
        ClaimStatusHistory history = ClaimStatusHistory.builder()
                .claim(claim)
                .fromStatus(fromStatus != null ? fromStatus.name() : null)
                .toStatus(toStatus.name())
                .changedBy(changedBy)
                .reason(reason)
                .build();
        ClaimStatusHistory saved = statusHistoryRepository.save(history);
        log.info("Audit log: claim {} transitioned from {} to {} by {}",
                claim.getId(), fromStatus, toStatus, changedBy);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ClaimStatusHistory> getClaimHistory(Long claimId) {
        return statusHistoryRepository.findByClaimIdOrderByCreatedAtDesc(claimId);
    }
}
