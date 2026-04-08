package com.msig.claimsapi.service.ai;

import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Evidence;
import com.msig.claimsdomain.entities.Policy;
import com.msig.claimsdomain.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "claims.processing.ai-enabled", havingValue = "false", matchIfMissing = false)
@Slf4j
public class StubAIOrchestrationService implements AIOrchestrationService {
    
    @Override
    public ProcessingResult<ExtractedClaimData> extractClaimData(FNOLDocument fnolDocument) {
        log.warn("AI extraction is disabled. Returning empty result for claim: {}", fnolDocument.getClaimId());
        return ProcessingResult.empty(ProcessingMode.FULL_MANUAL);
    }
    
    @Override
    public ProcessingResult<PolicyVerificationResult> verifyPolicy(Claim claim, Policy policy) {
        log.warn("AI policy verification is disabled. Returning empty result for claim: {}", claim.getId());
        return ProcessingResult.empty(ProcessingMode.FULL_MANUAL);
    }
    
    @Override
    public ProcessingResult<EntityMatchResult> matchEntities(ExtractedEntities extractedEntities) {
        log.warn("AI entity matching is disabled. Returning empty result.");
        return ProcessingResult.empty(ProcessingMode.FULL_MANUAL);
    }
    
    @Override
    public ProcessingResult<ForensicsResult> runForensics(List<Evidence> evidence) {
        log.warn("AI forensics is disabled. Returning empty result for {} evidence items.", 
                evidence != null ? evidence.size() : 0);
        return ProcessingResult.empty(ProcessingMode.FULL_MANUAL);
    }
    
    @Override
    public ProcessingResult<List<DuplicateMatch>> detectDuplicates(Claim claim) {
        log.warn("AI duplicate detection is disabled. Returning empty result for claim: {}", claim.getId());
        return ProcessingResult.<List<DuplicateMatch>>builder()
                .data(Collections.emptyList())
                .mode(ProcessingMode.FULL_MANUAL)
                .aiAvailable(false)
                .warnings(List.of("AI duplicate detection disabled"))
                .build();
    }
    
    @Override
    public ProcessingResult<List<ClaimSimilarityResult>> findSimilarClaims(Claim claim) {
        log.warn("AI semantic search is disabled. Returning empty result for claim: {}", claim.getId());
        return ProcessingResult.<List<ClaimSimilarityResult>>builder()
                .data(Collections.emptyList())
                .mode(ProcessingMode.FULL_MANUAL)
                .aiAvailable(false)
                .build();
    }

    @Override
    public ClaimRecommendation recommendRouting(Claim claim, ClaimContext context) {
        log.warn("ReAct agent is disabled. Returning HITL for claim: {}", claim.getId());
        return new ClaimRecommendation("HITL", "AI disabled, defaulting to human review", 0.0, List.of(), "n/a");
    }

    @Override
    public boolean isAIAvailable() {
        return false;
    }
    
    @Override
    public void recordSuccess() {
        log.debug("Recording AI success (stub - no-op)");
    }
    
    @Override
    public void recordFailure() {
        log.debug("Recording AI failure (stub - no-op)");
    }
}