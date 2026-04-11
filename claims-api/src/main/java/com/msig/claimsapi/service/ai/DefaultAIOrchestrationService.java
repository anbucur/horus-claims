package com.msig.claimsapi.service.ai;

import com.msig.claimsapi.service.SemanticSearchService;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.Evidence;
import com.msig.claimsdomain.entities.Policy;
import com.msig.claimsdomain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * AI orchestration service that delegates to the injected AIClient
 * (RealAzureAIClient or MockAIClient), with circuit-breaker protection
 * and structured metrics.
 *
 * All AI calls are wrapped via AICircuitBreaker — if the circuit is open,
 * returns an empty result immediately.
 *
 * For extractClaimData: raw text is extracted from the FNOLDocument and
 * passed to AIClient.extractClaimData (backed by GPT-4o via RealAzureAIClient).
 * The result is then mapped to the domain ExtractedClaimData model.
 */
@Service
@ConditionalOnProperty(name = "claims.azure.ai.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DefaultAIOrchestrationService implements AIOrchestrationService {

    private final AICircuitBreaker circuitBreaker;
    private final SemanticSearchService semanticSearchService;
    private final ReActClaimAgent reactAgent;
    private final AIClient aiClient;  // injected: RealAzureAIClient or MockAIClient

    @Override
    public ProcessingResult<ExtractedClaimData> extractClaimData(FNOLDocument fnolDocument) {
        if (circuitBreaker.isOpen()) {
            log.warn("[AIOrch] Circuit breaker OPEN — AI extraction unavailable for claim: {}",
                fnolDocument.getClaimId());
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }

        try {
            // Build raw text from FNOLDocument for the AI
            String rawText = buildRawText(fnolDocument);
            log.info("[AIOrch] Extracting claim data via AI for claim: {}", fnolDocument.getClaimId());

            AIClient.ExtractedClaimData aiResult = aiClient.extractClaimData(rawText);

            if (!aiResult.aiAvailable()) {
                circuitBreaker.recordFailure();
                log.warn("[AIOrch] AI extraction returned unavailable for claim: {}",
                    fnolDocument.getClaimId());
                return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
            }

            // Map AIClient.ExtractedClaimData → domain ExtractedClaimData
            ExtractedClaimData domainResult = ExtractedClaimData.builder()
                .claimId(fnolDocument.getClaimId())
                .dateOfLoss(parseDate(aiResult.dateOfLoss()))
                .lossLocation(aiResult.lossLocation())
                .incidentNarrative(aiResult.incidentNarrative())
                .estimatedLoss(parseBigDecimal(aiResult.estimatedValue()))
                .currency(aiResult.currency())
                .confidenceScore(aiResult.confidenceScore())
                .build();

            circuitBreaker.recordSuccess();
            return ProcessingResult.success(domainResult, ProcessingMode.AI_ASSISTED,
                aiResult.aiModelUsed() != null ? aiResult.aiModelUsed() : "gpt-4o");

        } catch (Exception e) {
            log.error("[AIOrch] AI extraction failed for claim: {}: {}",
                fnolDocument.getClaimId(), e.getMessage());
            circuitBreaker.recordFailure();
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }
    }

    @Override
    public ProcessingResult<PolicyVerificationResult> verifyPolicy(Claim claim, Policy policy) {
        if (circuitBreaker.isOpen()) {
            log.warn("[AIOrch] Circuit breaker OPEN — AI policy verification unavailable for claim: {}",
                claim.getId());
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }

        try {
            String policyNumber = policy.getPolicyNumber();
            String dateOfLoss = claim.getDateOfLoss() != null
                ? claim.getDateOfLoss().toString()
                : fnolDocumentDateOfLoss(claim);
            log.info("[AIOrch] Verifying policy {} for claim: {}", policyNumber, claim.getId());

            AIClient.PolicyVerificationResult aiResult =
                aiClient.verifyPolicy(policyNumber, dateOfLoss);

            if (!aiResult.aiAvailable()) {
                circuitBreaker.recordFailure();
                return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
            }

            PolicyVerificationResult domainResult = PolicyVerificationResult.builder()
                .policyActive(aiResult.inPeriod()) // aiResult.inPeriod means policy active + in period
                .coverageValid(aiResult.inPeriod())
                .deductibleSatisfied(true)
                .lineOfBusinessMatch(aiResult.lineOfBusiness())
                .mismatchReason(aiResult.coverageDetails())
                .confidenceScore(0.85)
                .build();

            circuitBreaker.recordSuccess();
            return ProcessingResult.success(domainResult, ProcessingMode.AI_ASSISTED,
                aiResult.aiModelUsed() != null ? aiResult.aiModelUsed() : "gpt-4o");

        } catch (Exception e) {
            log.error("[AIOrch] AI policy verification failed for claim: {}: {}",
                claim.getId(), e.getMessage());
            circuitBreaker.recordFailure();
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }
    }

    @Override
    public ProcessingResult<EntityMatchResult> matchEntities(ExtractedEntities extractedEntities) {
        if (circuitBreaker.isOpen()) {
            log.warn("[AIOrch] Circuit breaker OPEN — AI entity matching unavailable.");
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }

        try {
            List<String> allEntities = buildEntityList(extractedEntities);
            log.info("[AIOrch] Matching {} entities", allEntities.size());

            List<AIClient.EntityMatch> aiMatches = aiClient.matchEntities(allEntities);

            if (aiMatches.isEmpty()) {
                return ProcessingResult.<EntityMatchResult>builder()
                    .data(EntityMatchResult.builder()
                        .allEntitiesMatched(false)
                        .confidenceScore(0.0)
                        .build())
                    .mode(ProcessingMode.SEMI_AUTOMATIC)
                    .aiAvailable(true)
                    .warnings(List.of("No confident entity matches found"))
                    .build();
            }

            // Build domain result from AI matches
            EntityMatchResult domainResult = buildEntityMatchResult(aiMatches);

            circuitBreaker.recordSuccess();
            return ProcessingResult.success(domainResult, ProcessingMode.AI_ASSISTED,
                aiClient.getModelName());

        } catch (Exception e) {
            log.error("[AIOrch] AI entity matching failed: {}", e.getMessage());
            circuitBreaker.recordFailure();
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }
    }

    @Override
    public ProcessingResult<ForensicsResult> runForensics(List<Evidence> evidence) {
        if (circuitBreaker.isOpen()) {
            log.warn("[AIOrch] Circuit breaker OPEN — AI forensics unavailable.");
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }

        try {
            List<String> imageUrls = evidence != null
                ? evidence.stream()
                    .filter(e -> e.getFileUrl() != null && !e.getFileUrl().isBlank())
                    .map(Evidence::getFileUrl)
                    .toList()
                : List.of();

            log.info("[AIOrch] Running forensics on {} evidence items", imageUrls.size());

            AIClient.ForensicsResult aiResult = aiClient.runForensics(imageUrls);

            ForensicsResult domainResult = ForensicsResult.builder()
                .imagesAuthentic(!aiResult.deepfakeDetected() && !aiResult.narrativeMismatch())
                .manipulatedImageIds(aiResult.deepfakeDetected() ? imageUrls : List.of())
                .anomalyFlags(aiResult.flaggedRegions())
                .overallScore(1.0 - aiResult.manipulationScore())
                .quarantineRecommended(
                    aiResult.deepfakeDetected() || aiResult.narrativeMismatch()
                        || aiResult.manipulationScore() > 0.6)
                .build();

            circuitBreaker.recordSuccess();
            return ProcessingResult.success(domainResult, ProcessingMode.AI_ASSISTED,
                aiResult.aiModelUsed() != null ? aiResult.aiModelUsed() : "gpt-4o-vision");

        } catch (Exception e) {
            log.error("[AIOrch] AI forensics failed: {}", e.getMessage());
            circuitBreaker.recordFailure();
            return ProcessingResult.empty(ProcessingMode.SEMI_AUTOMATIC);
        }
    }

    @Override
    public ProcessingResult<List<DuplicateMatch>> detectDuplicates(Claim claim) {
        // Duplicate detection is handled by TextSimilarityDuplicateDetector in ClaimProcessingService
        // This method is reserved for AI-based duplicate detection if needed
        return ProcessingResult.<List<DuplicateMatch>>builder()
            .data(Collections.emptyList())
            .mode(ProcessingMode.SEMI_AUTOMATIC)
            .aiAvailable(false)
            .build();
    }

    @Override
    public ProcessingResult<List<ClaimSimilarityResult>> findSimilarClaims(Claim claim) {
        if (circuitBreaker.isOpen()) {
            log.warn("[AIOrch] Circuit breaker OPEN — Semantic search unavailable for claim: {}",
                claim.getId());
            return ProcessingResult.<List<ClaimSimilarityResult>>builder()
                .data(Collections.emptyList())
                .mode(ProcessingMode.SEMI_AUTOMATIC)
                .aiAvailable(false)
                .build();
        }

        try {
            List<ClaimSimilarityResult> results = semanticSearchService.findSimilarClaims(claim, 5);
            log.info("[AIOrch] Semantic search found {} similar claims for claim: {}",
                results.size(), claim.getId());
            circuitBreaker.recordSuccess();
            return ProcessingResult.success(results, ProcessingMode.AI_ASSISTED, "azure-ai-search");
        } catch (Exception e) {
            log.error("[AIOrch] Semantic search failed for claim: {}: {}",
                claim.getId(), e.getMessage());
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
            log.warn("[AIOrch] Circuit breaker OPEN — ReAct agent unavailable for claim: {}",
                claim.getId());
            return new ClaimRecommendation("HITL",
                "AI unavailable, defaulting to human review", 0.0, List.of(), "n/a");
        }

        try {
            ClaimRecommendation rec = reactAgent.recommend(claim, context);
            circuitBreaker.recordSuccess();
            log.info("[AIOrch] ReAct agent recommended {} for claim {} with confidence {}",
                rec.decision(), claim.getId(), rec.confidence());
            return rec;
        } catch (Exception e) {
            log.error("[AIOrch] ReAct agent failed for claim: {}: {}", claim.getId(), e.getMessage());
            circuitBreaker.recordFailure();
            return new ClaimRecommendation("HITL",
                "AI reasoning failed, defaulting to human review", 0.0, List.of(), "n/a");
        }
    }

    @Override
    public boolean isAIAvailable() {
        return !circuitBreaker.isOpen() && aiClient.isAvailable();
    }

    @Override
    public void recordSuccess() {
        circuitBreaker.recordSuccess();
    }

    @Override
    public void recordFailure() {
        circuitBreaker.recordFailure();
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /**
     * Build a text representation from FNOLDocument for AI consumption.
     */
    private String buildRawText(FNOLDocument fnol) {
        StringBuilder sb = new StringBuilder();
        if (fnol.getPolicyNumber() != null) {
            sb.append("Policy Number: ").append(fnol.getPolicyNumber()).append("\n");
        }
        if (fnol.getDateOfLoss() != null) {
            sb.append("Date of Loss: ").append(fnol.getDateOfLoss()).append("\n");
        }
        if (fnol.getLossLocation() != null) {
            sb.append("Loss Location: ").append(fnol.getLossLocation()).append("\n");
        }
        if (fnol.getIncidentDescription() != null) {
            sb.append("Incident Description: ").append(fnol.getIncidentDescription()).append("\n");
        }
        return sb.toString().trim();
    }

    private String fnolDocumentDateOfLoss(Claim claim) {
        return claim.getDateOfLoss() != null ? claim.getDateOfLoss().toString() : null;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.debug("[AIOrch] Could not parse date: {}", dateStr);
            return null;
        }
    }

    private BigDecimal parseBigDecimal(String valueStr) {
        if (valueStr == null || valueStr.isBlank()) return null;
        try {
            // Strip non-numeric characters except decimal point and minus
            String cleaned = valueStr.replaceAll("[^\\d.,\\-]", "");
            return new BigDecimal(cleaned.replace(",", ""));
        } catch (Exception e) {
            log.debug("[AIOrch] Could not parse estimated value: {}", valueStr);
            return null;
        }
    }

    private List<String> buildEntityList(ExtractedEntities entities) {
        List<String> names = new java.util.ArrayList<>();
        if (entities.getAssuredNames() != null) names.addAll(entities.getAssuredNames());
        if (entities.getBrokerNames() != null) names.addAll(entities.getBrokerNames());
        if (entities.getVesselNames() != null) names.addAll(entities.getVesselNames());
        if (entities.getImoNumbers() != null) names.addAll(entities.getImoNumbers());
        return names;
    }

    private EntityMatchResult buildEntityMatchResult(List<AIClient.EntityMatch> aiMatches) {
        Long assuredId = null;
        String assuredName = null;
        Long brokerId = null;
        String brokerName = null;
        List<Long> vesselIds = new java.util.ArrayList<>();
        List<String> vesselNames = new java.util.ArrayList<>();
        double totalConfidence = 0.0;
        int matchCount = 0;

        for (AIClient.EntityMatch match : aiMatches) {
            if (match.matchType() != null) {
                switch (match.matchType().toUpperCase()) {
                    case "COMPANY" -> {
                        if (assuredName == null) {
                            assuredName = match.matchedName();
                            assuredId = 1L; // Placeholder — real impl would look up DB
                        }
                    }
                    case "VESSEL" -> {
                        if (match.matchedName() != null) {
                            vesselNames.add(match.matchedName());
                            vesselIds.add(1L); // Placeholder
                        }
                    }
                    case "PERSON" -> {
                        if (brokerName == null) {
                            brokerName = match.matchedName();
                            brokerId = 2L; // Placeholder
                        }
                    }
                }
                totalConfidence += match.confidence();
                matchCount++;
            }
        }

        return EntityMatchResult.builder()
            .assuredId(assuredId)
            .assuredName(assuredName)
            .brokerId(brokerId)
            .brokerName(brokerName)
            .vesselIds(vesselIds)
            .vesselNames(vesselNames)
            .allEntitiesMatched(matchCount > 0)
            .confidenceScore(matchCount > 0 ? totalConfidence / matchCount : 0.0)
            .build();
    }
}
