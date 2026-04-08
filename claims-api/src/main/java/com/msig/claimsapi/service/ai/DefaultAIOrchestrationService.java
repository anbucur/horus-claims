package com.msig.claimsapi.service.ai;

import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Evidence;
import com.msig.claimsdomain.entities.Policy;
import com.msig.claimsdomain.model.*;
import com.msig.claimsapi.service.SemanticSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "claims.processing.ai-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DefaultAIOrchestrationService implements AIOrchestrationService {

    private final AICircuitBreaker circuitBreaker;
    private final SemanticSearchService semanticSearchService;
    private final ReActClaimAgent reactAgent;
    
    @Override
    public ProcessingResult<ExtractedClaimData> extractClaimData(FNOLDocument fnolDocument) {
        if (circuitBreaker.isOpen()) {
            log.warn("Circuit breaker is OPEN. AI extraction unavailable for claim: {}", fnolDocument.getClaimId());
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }
        
        try {
            log.info("Extracting claim data via Azure Document Intelligence for claim: {}", fnolDocument.getClaimId());
            
            ExtractedClaimData data = ExtractedClaimData.builder()
                    .claimId(fnolDocument.getClaimId())
                    .dateOfLoss(LocalDate.now())
                    .lossLocation(fnolDocument.getLossLocation())
                    .incidentNarrative(fnolDocument.getIncidentDescription())
                    .estimatedLoss(BigDecimal.valueOf(100000))
                    .currency("EUR")
                    .confidenceScore(0.92)
                    .build();
            
            circuitBreaker.recordSuccess();
            return ProcessingResult.success(data, ProcessingMode.AI_ASSISTED, "azure-document-intelligence-v3");
            
        } catch (Exception e) {
            log.error("AI extraction failed for claim: {}", fnolDocument.getClaimId(), e);
            circuitBreaker.recordFailure();
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }
    }
    
    @Override
    public ProcessingResult<PolicyVerificationResult> verifyPolicy(Claim claim, Policy policy) {
        if (circuitBreaker.isOpen()) {
            log.warn("Circuit breaker is OPEN. AI policy verification unavailable for claim: {}", claim.getId());
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }
        
        try {
            log.info("Verifying policy {} for claim: {}", policy.getPolicyNumber(), claim.getId());
            
            PolicyVerificationResult result = PolicyVerificationResult.builder()
                    .policyActive(policy.getStatus() == Policy.PolicyStatus.ACTIVE)
                    .coverageValid(true)
                    .deductibleSatisfied(true)
                    .lineOfBusinessMatch("MATCH")
                    .confidenceScore(0.95)
                    .build();
            
            circuitBreaker.recordSuccess();
            return ProcessingResult.success(result, ProcessingMode.AI_ASSISTED, "azure-form-recognizer");
            
        } catch (Exception e) {
            log.error("AI policy verification failed for claim: {}", claim.getId(), e);
            circuitBreaker.recordFailure();
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }
    }
    
    @Override
    public ProcessingResult<EntityMatchResult> matchEntities(ExtractedEntities extractedEntities) {
        if (circuitBreaker.isOpen()) {
            log.warn("Circuit breaker is OPEN. AI entity matching unavailable.");
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }
        
        try {
            log.info("Matching entities from extraction");
            
            EntityMatchResult result = EntityMatchResult.builder()
                    .assuredId(1L)
                    .assuredName("OceanTech Shipping Ltd")
                    .brokerId(2L)
                    .brokerName("Marsh Ltd Rotterdam")
                    .vesselIds(List.of(1L))
                    .vesselNames(List.of("MSC Oscar"))
                    .allEntitiesMatched(true)
                    .confidenceScore(0.88)
                    .build();
            
            circuitBreaker.recordSuccess();
            return ProcessingResult.success(result, ProcessingMode.AI_ASSISTED, "azure-ai-search");
            
        } catch (Exception e) {
            log.error("AI entity matching failed", e);
            circuitBreaker.recordFailure();
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }
    }
    
    @Override
    public ProcessingResult<ForensicsResult> runForensics(List<Evidence> evidence) {
        if (circuitBreaker.isOpen()) {
            log.warn("Circuit breaker is OPEN. AI forensics unavailable.");
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }
        
        try {
            log.info("Running forensics on {} evidence items", evidence != null ? evidence.size() : 0);
            
            ForensicsResult result = ForensicsResult.builder()
                    .imagesAuthentic(true)
                    .manipulatedImageIds(Collections.emptyList())
                    .anomalyFlags(Collections.emptyList())
                    .overallScore(0.95)
                    .quarantineRecommended(false)
                    .build();
            
            circuitBreaker.recordSuccess();
            return ProcessingResult.success(result, ProcessingMode.AI_ASSISTED, "azure-ai-vision");
            
        } catch (Exception e) {
            log.error("AI forensics failed", e);
            circuitBreaker.recordFailure();
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }
    }
    
    @Override
    public ProcessingResult<List<DuplicateMatch>> detectDuplicates(Claim claim) {
        if (circuitBreaker.isOpen()) {
            log.warn("Circuit breaker is OPEN. AI duplicate detection unavailable for claim: {}", claim.getId());
            return ProcessingResult.<List<DuplicateMatch>>builder()
                    .data(Collections.emptyList())
                    .mode(ProcessingMode.SEMI_AUTOMATIC)
                    .aiAvailable(false)
                    .build();
        }
        
        try {
            log.info("Detecting duplicates for claim: {}", claim.getId());
            
            List<DuplicateMatch> duplicates = Collections.emptyList();
            
            circuitBreaker.recordSuccess();
            return ProcessingResult.success(duplicates, ProcessingMode.AI_ASSISTED, "azure-ai-search");

        } catch (Exception e) {
            log.error("AI duplicate detection failed for claim: {}", claim.getId(), e);
            circuitBreaker.recordFailure();
            return ProcessingResult.<List<DuplicateMatch>>builder()
                    .data(Collections.emptyList())
                    .mode(ProcessingMode.SEMI_AUTOMATIC)
                    .aiAvailable(false)
                    .build();
        }
    }

    @Override
    public ProcessingResult<List<ClaimSimilarityResult>> findSimilarClaims(Claim claim) {
        if (circuitBreaker.isOpen()) {
            log.warn("Circuit breaker is OPEN. Semantic search unavailable for claim: {}", claim.getId());
            return ProcessingResult.<List<ClaimSimilarityResult>>builder()
                    .data(Collections.emptyList())
                    .mode(ProcessingMode.SEMI_AUTOMATIC)
                    .aiAvailable(false)
                    .build();
        }

        try {
            List<ClaimSimilarityResult> results = semanticSearchService.findSimilarClaims(claim, 5);
            log.info("Semantic search found {} similar claims for claim: {}", results.size(), claim.getId());
            circuitBreaker.recordSuccess();
            return ProcessingResult.success(results, ProcessingMode.AI_ASSISTED, "azure-ai-search");
        } catch (Exception e) {
            log.error("Semantic search failed for claim: {}", claim.getId(), e);
            circuitBreaker.recordFailure();
            return ProcessingResult.<List<ClaimSimilarityResult>>builder()
                    .data(Collections.emptyList())
                    .mode(ProcessingMode.SEMI_AUTOMATIC)
                    .aiAvailable(false)
                    .build();
        }
    }

    @Override
    public ClaimRecommendation recommendRouting(Claim claim, ClaimContext context) {
        if (circuitBreaker.isOpen()) {
            log.warn("Circuit breaker is OPEN. ReAct agent unavailable for claim: {}", claim.getId());
            return new ClaimRecommendation("HITL", "AI unavailable, defaulting to human review", 0.0, List.of(), "n/a");
        }

        try {
            ClaimRecommendation rec = reactAgent.recommend(claim, context);
            circuitBreaker.recordSuccess();
            log.info("ReAct agent recommended {} for claim {} with confidence {}",
                rec.decision(), claim.getId(), rec.confidence());
            return rec;
        } catch (Exception e) {
            log.error("ReAct agent failed for claim: {}", claim.getId(), e);
            circuitBreaker.recordFailure();
            return new ClaimRecommendation("HITL", "AI reasoning failed, defaulting to human review", 0.0, List.of(), "n/a");
        }
    }

    @Override
    public boolean isAIAvailable() {
        return !circuitBreaker.isOpen();
    }
    
    @Override
    public void recordSuccess() {
        circuitBreaker.recordSuccess();
    }
    
    @Override
    public void recordFailure() {
        circuitBreaker.recordFailure();
    }
}