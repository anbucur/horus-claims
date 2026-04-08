package com.msig.claimsapi.service.ai;

import java.util.List;

/**
 * AI client interface — abstracts away the AI provider.
 * Implementations: MockAIClient (no credentials), RealAzureAIClient (with Azure AI Foundry).
 */
public interface AIClient {

    /**
     * Extract structured claim data from raw FNOL text/document.
     */
    AIClient.ExtractedClaimData extractClaimData(String rawText);

    /**
     * Verify that a claim's date/location is consistent with policy terms.
     */
    AIClient.PolicyVerificationResult verifyPolicy(String policyNumber, String dateOfLoss);

    /**
     * Match extracted entity names against the core system registry.
     */
    List<AIClient.EntityMatch> matchEntities(List<String> entityNames);

    /**
     * Run image forensics on evidence URLs.
     */
    AIClient.ForensicsResult runForensics(List<String> imageUrls);

    /**
     * Returns true if the AI service is reachable and configured.
     */
    boolean isAvailable();

    /**
     * Returns the model name being used (e.g. "gpt-4o", "mock-ai-stub").
     */
    String getModelName();

    // ─── Shared response records ───────────────────────────────────────────────

    record ExtractedClaimData(
            String dateOfLoss,
            String incidentNarrative,
            String lossLocation,
            String estimatedValue,
            String currency,
            double confidenceScore,
            boolean aiAvailable,
            String aiModelUsed,
            String traceId
    ) {}

    record PolicyVerificationResult(
            Long policyId,
            String policyNumber,
            String status,
            String lineOfBusiness,
            String coverageDetails,
            String deductible,
            boolean inPeriod,
            boolean aiAvailable,
            String aiModelUsed
    ) {}

    record EntityMatch(
            String extractedName,
            String matchedName,
            String matchType,
            double confidence
    ) {}

    record ForensicsResult(
            List<String> flaggedRegions,
            double manipulationScore,
            boolean deepfakeDetected,
            boolean narrativeMismatch,
            String summary,
            boolean aiAvailable,
            boolean processed,
            String aiModelUsed
    ) {}
}
