package com.msig.claimsapi.service.ai;

import com.msig.claimsdomain.model.ClaimSimilarityResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Stub AI client — returns deterministic empty responses.
 * Works without any external AI credentials.
 * Swap for RealAzureAIClient when Azure AI Foundry credentials are available.
 */
@Service
@Slf4j
public class MockAIClient implements AIClient {

    @Override
    public ExtractedClaimData extractClaimData(String rawText) {
        log.info("[MOCK AI] extractClaimData called — AI unavailable, returning empty result");
        return new ExtractedClaimData(
                null, null, null, null, null,
                0.0, false, "mock-ai-stub", "no-llm-call"
        );
    }

    @Override
    public PolicyVerificationResult verifyPolicy(String policyNumber, String dateOfLoss) {
        log.info("[MOCK AI] verifyPolicy called — AI unavailable");
        return new PolicyVerificationResult(
                null, null, null, null, null, null,
                false, false, "mock-ai-stub"
        );
    }

    @Override
    public List<EntityMatch> matchEntities(List<String> entityNames) {
        log.info("[MOCK AI] matchEntities called — AI unavailable, returning empty list");
        return List.of();
    }

    @Override
    public ForensicsResult runForensics(List<String> imageUrls) {
        log.info("[MOCK AI] runForensics called — AI unavailable");
        return new ForensicsResult(
                List.of(), 0.0, false, false,
                "Image forensics skipped — AI service unavailable",
                false, false, "mock-ai-stub"
        );
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String getModelName() {
        return "mock-ai-stub";
    }

    @Override
    public List<ClaimSimilarityResult> findSimilarClaims(String queryText, int limit) {
        log.info("[MOCK AI] findSimilarClaims called — AI unavailable, returning empty list");
        return List.of();
    }
}
