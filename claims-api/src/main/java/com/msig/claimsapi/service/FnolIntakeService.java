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
import com.msig.claimsdomain.entities.Evidence;
import com.msig.claimsdomain.entities.Policy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FnolIntakeService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final EvidenceRepository evidenceRepository;
    private final ClaimAuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public FnolIntakeResponse submitIntake(FnolIntakeRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.info("FNOL intake submitted for policy {} (traceId: {})",
                request.getPolicyNumber(), traceId);

        Policy policy = policyRepository.findByPolicyNumber(request.getPolicyNumber())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Policy not found: " + request.getPolicyNumber()));

        Claim claim = Claim.builder()
                .policy(policy)
                .dateOfLoss(request.getDateOfLoss())
                .incidentNarrative(request.getIncidentNarrative())
                .lossLocation(request.getLossLocation())
                .workflowStatus(WorkflowStatus.RECEIVED)
                .build();
        Claim saved = claimRepository.save(claim);

        if (request.getEvidenceUrls() != null && !request.getEvidenceUrls().isEmpty()) {
            for (String url : request.getEvidenceUrls()) {
                Evidence evidence = Evidence.builder()
                        .claim(saved)
                        .documentType(Evidence.DocumentType.EMAIL)
                        .fileUrl(url)
                        .isQuarantined(false)
                        .build();
                evidenceRepository.save(evidence);
            }
        }

        auditService.logStatusChange(saved, null, WorkflowStatus.RECEIVED,
                request.getSubmittedBy(), "FNOL intake submitted");

        ClaimSubmittedEvent event = ClaimSubmittedEvent.builder()
                .claimId(saved.getId())
                .policyNumber(request.getPolicyNumber())
                .submittedAt(java.time.LocalDateTime.now())
                .traceId(traceId)
                .build();
        eventPublisher.publishEvent(event);

        log.info("FNOL intake completed: claim {} created for policy {}",
                saved.getId(), request.getPolicyNumber());

        return FnolIntakeResponse.builder()
                .claimId(saved.getId())
                .status(WorkflowStatus.RECEIVED.name())
                .policyNumber(request.getPolicyNumber())
                .message("Claim received successfully")
                .traceId(traceId)
                .build();
    }
}
