package com.msig.claimsapi.temporal;

import com.msig.claimsapi.repository.ClaimRepository;
import com.msig.claimsapi.service.ClaimProcessingService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.model.*;
import io.temporal.activity.Activity;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Temporal activity implementations for the claim processing pipeline.
 *
 * <p>Each method delegates to the corresponding step in
 * {@link ClaimProcessingService}, keeping all business logic in the existing
 * service layer. Activities are stateless and load the claim from the database
 * on each invocation, which is safe because Temporal guarantees at-least-once
 * delivery with idempotent retries.
 *
 * <p>Registered on task queue {@code claims-processing} via the
 * {@link ActivityImpl} annotation.
 */
@Component("claimProcessingActivitiesImpl")
@ActivityImpl(taskQueues = "claims-processing")
@RequiredArgsConstructor
@Slf4j
public class ClaimProcessingActivitiesImpl implements ClaimProcessingActivities {

    private final ClaimProcessingService claimProcessingService;
    private final ClaimRepository claimRepository;

    @Override
    public double extractClaimData(Long claimId, ProcessingMode mode, String traceId) {
        log.info("[Activity] EXTRACT claimId={} mode={} traceId={}", claimId, mode, traceId);
        Claim claim = loadClaim(claimId);
        ProcessingResult<ExtractedClaimData> result =
            claimProcessingService.extract(claim, mode, traceId);
        double confidence = 0.0;
        if (result.getData() != null) {
            confidence = result.getData().getConfidenceScore();
        }
        log.info("[Activity] EXTRACT complete claimId={} confidence={}", claimId, confidence);
        return confidence;
    }

    @Override
    public boolean verifyPolicy(Long claimId, ProcessingMode mode, String traceId) {
        log.info("[Activity] VERIFY_POLICY claimId={} mode={} traceId={}", claimId, mode, traceId);
        Claim claim = loadClaim(claimId);
        ProcessingResult<PolicyVerificationResult> result =
            claimProcessingService.verifyPolicy(claim, mode, traceId);
        boolean passed = result.getData() == null || result.getData().isCoverageValid();
        log.info("[Activity] VERIFY_POLICY complete claimId={} passed={}", claimId, passed);
        return passed;
    }

    @Override
    public int matchEntities(Long claimId, ProcessingMode mode, String traceId) {
        log.info("[Activity] MATCH_ENTITIES claimId={} mode={} traceId={}", claimId, mode, traceId);
        Claim claim = loadClaim(claimId);
        ProcessingResult<EntityMatchResult> result =
            claimProcessingService.matchEntities(claim, mode, traceId);
        int matched = (result.getData() != null && result.getData().isAllEntitiesMatched()) ? 1 : 0;
        log.info("[Activity] MATCH_ENTITIES complete claimId={} matched={}", claimId, matched);
        return matched;
    }

    @Override
    public boolean runForensics(Long claimId, ProcessingMode mode, String traceId) {
        log.info("[Activity] FORENSICS claimId={} mode={} traceId={}", claimId, mode, traceId);
        Claim claim = loadClaim(claimId);
        ProcessingResult<ForensicsResult> result =
            claimProcessingService.runForensics(claim, mode, traceId);
        boolean clean = result.getData() == null || !result.getData().isQuarantineRecommended();
        log.info("[Activity] FORENSICS complete claimId={} clean={}", claimId, clean);
        return clean;
    }

    @Override
    public int checkDuplicates(Long claimId, ProcessingMode mode, String traceId) {
        log.info("[Activity] DUPLICATE_CHECK claimId={} mode={} traceId={}", claimId, mode, traceId);
        Claim claim = loadClaim(claimId);
        ProcessingResult<List<DuplicateMatch>> result =
            claimProcessingService.checkDuplicates(claim, mode, traceId);
        int count = result.getData() != null ? result.getData().size() : 0;
        log.info("[Activity] DUPLICATE_CHECK complete claimId={} duplicates={}", claimId, count);
        return count;
    }

    @Override
    public String routeClaim(Long claimId, ProcessingMode mode, double confidenceScore, String traceId) {
        log.info("[Activity] ROUTE claimId={} mode={} confidence={} traceId={}",
            claimId, mode, confidenceScore, traceId);
        Claim claim = loadClaim(claimId);
        claim.setAiConfidenceScore(confidenceScore);
        ProcessingResult<Claim> result = claimProcessingService.route(claim, mode, traceId);
        String status = result.getData() != null
            ? result.getData().getWorkflowStatus().name()
            : "HITL";
        log.info("[Activity] ROUTE complete claimId={} status={}", claimId, status);
        return status;
    }

    private Claim loadClaim(Long claimId) {
        return claimRepository.findByIdWithPolicy(claimId)
            .orElseThrow(() -> new IllegalArgumentException("Claim not found: " + claimId));
    }
}
