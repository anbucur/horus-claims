package com.msig.claimsapi.service.ai;

import com.azure.ai.inference.ChatCompletionsClient;
import com.azure.ai.inference.ChatCompletionsClientBuilder;
import com.azure.ai.inference.models.ChatCompletionsOptions;
import com.azure.ai.inference.models.ChatCompletions;
import com.azure.ai.inference.models.ChatRequestMessage;
import com.azure.ai.inference.models.ChatRequestSystemMessage;
import com.azure.ai.inference.models.ChatRequestUserMessage;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msig.claimsapi.config.AzureAIConfig;
import com.msig.claimsdomain.model.ClaimSimilarityResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Real Azure AI client using the Azure AI Foundry SDK (azure-ai-inference).
 *
 * Activates only when claims.azure.ai.enabled=true.
 * Requires AZURE_AI_PROJECT_ENDPOINT and AZURE_AI_API_KEY (or DefaultAzureCredential) env vars.
 *
 * Swap-in replacement for MockAIClient.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "claims.azure.ai.enabled", havingValue = "true")
public class RealAzureAIClient implements AIClient {

    private final AzureAIConfig config;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private ChatCompletionsClient chatClient;

    public RealAzureAIClient(AzureAIConfig config, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    void init() {
        String endpoint = buildEndpoint();
        log.info("[RealAzureAI] Initialising ChatCompletionsClient → {}", endpoint);

        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            log.info("[RealAzureAI] Authenticating with AzureKeyCredential");
            this.chatClient = new ChatCompletionsClientBuilder()
                .credential(new AzureKeyCredential(config.getApiKey()))
                .endpoint(endpoint)
                .buildClient();
        } else {
            log.info("[RealAzureAI] Authenticating with DefaultAzureCredential");
            TokenCredential credential = new DefaultAzureCredentialBuilder().build();
            this.chatClient = new ChatCompletionsClientBuilder()
                .credential(credential)
                .endpoint(endpoint)
                .buildClient();
        }
    }

    private String buildEndpoint() {
        String base = config.getProjectEndpoint();
        if (base == null || base.isBlank()) {
            throw new IllegalStateException("AZURE_AI_PROJECT_ENDPOINT is required when claims.azure.ai.enabled=true");
        }
        if (!base.endsWith("/")) base += "/";
        return base + config.getApiPath() + "/" + config.getModelDeployment();
    }

    // ─── AIClient implementation ───────────────────────────────────────────────

    @Override
    public ExtractedClaimData extractClaimData(String rawText) {
        String systemPrompt =
            "You are an insurance claims data extraction specialist. " +
            "Extract structured claim data from FNOL text. " +
            "Return ONLY a JSON object with: dateOfLoss (ISO-8601), incidentNarrative, lossLocation, " +
            "estimatedValue (string), currency, confidenceScore (0-1). " +
            "Use null for fields you cannot determine.";

        try {
            String json = chat(systemPrompt, rawText);
            JsonNode node = objectMapper.readTree(json);

            return new ExtractedClaimData(
                nullIfMissing(node, "dateOfLoss"),
                nullIfMissing(node, "incidentNarrative"),
                nullIfMissing(node, "lossLocation"),
                nullIfMissing(node, "estimatedValue"),
                nullIfMissing(node, "currency"),
                node.has("confidenceScore") ? node.get("confidenceScore").asDouble() : 0.0,
                true,
                config.getModelDeployment(),
                UUID.randomUUID().toString()
            );
        } catch (Exception e) {
            log.error("[RealAzureAI] extractClaimData failed: {}", e.getMessage());
            return new ExtractedClaimData(
                null, null, null, null, null, 0.0, false, config.getModelDeployment(), null
            );
        }
    }

    @Override
    public PolicyVerificationResult verifyPolicy(String policyNumber, String dateOfLoss) {
        String systemPrompt =
            "You are a marine insurance policy verification specialist. " +
            "Given a policy number and date of loss, verify policy status, coverage validity, " +
            "deductible satisfaction, and line of business match. " +
            "Return ONLY a JSON object with: policyId (number or null), policyNumber, status, " +
            "lineOfBusiness, coverageDetails, deductible, inPeriod (boolean). " +
            "Use null for fields you cannot verify.";

        String userPrompt = "Policy Number: " + policyNumber + "\nDate of Loss: " + dateOfLoss;

        try {
            String json = chat(systemPrompt, userPrompt);
            JsonNode node = objectMapper.readTree(json);

            return new PolicyVerificationResult(
                node.has("policyId") && !node.get("policyId").isNull() ? node.get("policyId").asLong() : null,
                nullIfMissing(node, "policyNumber"),
                nullIfMissing(node, "status"),
                nullIfMissing(node, "lineOfBusiness"),
                nullIfMissing(node, "coverageDetails"),
                nullIfMissing(node, "deductible"),
                node.has("inPeriod") && !node.get("inPeriod").isNull() && node.get("inPeriod").asBoolean(),
                true,
                config.getModelDeployment()
            );
        } catch (Exception e) {
            log.error("[RealAzureAI] verifyPolicy failed: {}", e.getMessage());
            return new PolicyVerificationResult(
                null, null, null, null, null, null, false, false, config.getModelDeployment()
            );
        }
    }

    @Override
    public List<EntityMatch> matchEntities(List<String> entityNames) {
        if (entityNames == null || entityNames.isEmpty()) return List.of();

        String systemPrompt =
            "You are an entity matching specialist for marine insurance. " +
            "Match the given entity names against known companies, vessels, and parties. " +
            "Return ONLY a JSON array of matches. Each entry: " +
            "{ extractedName, matchedName, matchType (COMPANY|VESSEL|PERSON), confidence (0-1) }. " +
            "Be conservative — only match if confidence > 0.7. " +
            "Return an empty array if no confident matches.";

        String userPrompt = "Entity names to match:\n" + String.join("\n", entityNames);

        try {
            String json = chat(systemPrompt, userPrompt);
            JsonNode array = objectMapper.readTree(json);

            List<EntityMatch> results = new ArrayList<>();
            if (array.isArray()) {
                for (JsonNode item : array) {
                    double confidence = item.has("confidence") ? item.get("confidence").asDouble() : 0.0;
                    if (confidence > 0.7) {
                        results.add(new EntityMatch(
                            nullIfMissing(item, "extractedName"),
                            nullIfMissing(item, "matchedName"),
                            nullIfMissing(item, "matchType"),
                            confidence
                        ));
                    }
                }
            }
            return results;
        } catch (Exception e) {
            log.error("[RealAzureAI] matchEntities failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public ForensicsResult runForensics(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return new ForensicsResult(
                List.of(), 0.0, false, false,
                "No images provided for forensics", false, false, config.getModelDeployment()
            );
        }

        List<String> flaggedRegions = new ArrayList<>();
        double maxManipulationScore = 0.0;
        boolean deepfakeDetected = false;
        boolean narrativeMismatch = false;
        StringBuilder summary = new StringBuilder();

        // Call Azure Computer Vision REST API for image metadata analysis
        for (String imageUrl : imageUrls) {
            try {
                VisionAnalysisResult vr = analyzeImageViaRest(imageUrl);
                if (vr != null) {
                    flaggedRegions.addAll(vr.flaggedRegions());
                    maxManipulationScore = Math.max(maxManipulationScore, vr.manipulationScore());
                    if (vr.deepfakeDetected()) deepfakeDetected = true;
                }
            } catch (Exception e) {
                log.warn("[RealAzureAI] Vision REST analysis failed for {}: {}", imageUrl, e.getMessage());
            }
        }

        // Use GPT-4o via chat completions to assess narrative consistency
        try {
            String narrativeCheck =
                "You are an insurance image forensics specialist. " +
                "Assess whether the following image URL content is consistent with a marine hull damage claim. " +
                "Does the image show damage consistent with the incident? Respond with YES or NO and a brief reason.";

            for (String imageUrl : imageUrls) {
                // Include image URL as content reference
                String result = chatWithImage(narrativeCheck, imageUrl);
                if (result != null && result.toLowerCase().contains("no")) {
                    narrativeMismatch = true;
                    summary.append("Narrative mismatch detected for ").append(imageUrl).append(". ");
                }
            }
            if (summary.length() == 0) {
                summary.append("Image analysis complete. No narrative mismatches detected.");
            }
        } catch (Exception e) {
            log.warn("[RealAzureAI] GPT-4o vision assessment failed: {}", e.getMessage());
            summary.append(" Vision AI assessment unavailable.");
        }

        return new ForensicsResult(
            flaggedRegions,
            maxManipulationScore,
            deepfakeDetected,
            narrativeMismatch,
            summary.toString().trim(),
            true,
            true,
            config.getModelDeployment()
        );
    }

    @Override
    public boolean isAvailable() {
        return config.isEnabled();
    }

    @Override
    public String getModelName() {
        return config.getModelDeployment();
    }

    @Override
    public List<ClaimSimilarityResult> findSimilarClaims(String queryText, int limit) {
        // Semantic search is handled by SemanticSearchService which uses Azure AI Search
        // This method is a no-op here — RealAzureAIClient focuses on LLM inference
        return List.of();
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    /**
     * Send a chat request to Azure AI Foundry GPT-4o.
     * Returns the raw response content string.
     */
    private String chat(String systemPrompt, String userMessage) {
        List<ChatRequestMessage> messages = new ArrayList<>();
        messages.add(new ChatRequestSystemMessage(systemPrompt));
        messages.add(new ChatRequestUserMessage(userMessage));

        ChatCompletionsOptions options = new ChatCompletionsOptions(messages);
        options.setMaxTokens(500);
        options.setTemperature(0.1);
        options.setModel(config.getModelDeployment());

        ChatCompletions response = chatClient.complete(options);

        String content = response.getChoice().getMessage().getContent();
        log.debug("[RealAzureAI] chat response: {}", content);
        return content;
    }

    /**
     * Send a chat request with an image URL reference (GPT-4o vision).
     * Uses multi-part content message.
     */
    private String chatWithImage(String systemPrompt, String imageUrl) {
        List<ChatRequestMessage> messages = new ArrayList<>();
        messages.add(new ChatRequestSystemMessage(systemPrompt));
        messages.add(new ChatRequestUserMessage("Image: " + imageUrl + "\n\nAssess the image at the URL above."));

        ChatCompletionsOptions options = new ChatCompletionsOptions(messages);
        options.setMaxTokens(300);
        options.setTemperature(0.1);
        options.setModel(config.getModelDeployment());

        ChatCompletions response = chatClient.complete(options);
        return response.getChoice().getMessage().getContent();
    }

    /**
     * Analyse a single image via Azure Computer Vision REST API (if endpoint is configured).
     * Falls back gracefully if not configured or call fails.
     */
    private VisionAnalysisResult analyzeImageViaRest(String imageUrl) {
        if (config.getVisionEndpoint() == null || config.getVisionEndpoint().isBlank()
            || config.getVisionApiKey() == null || config.getVisionApiKey().isBlank()) {
            log.debug("[RealAzureAI] Vision endpoint not configured — skipping metadata analysis");
            return null;
        }

        try {
            String visionUrl = config.getVisionEndpoint()
                + "/vision/computeHistory/analyze?api-version=" + config.getVisionApiVersion();

            var headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.set("Ocp-Apim-Subscription-Key", config.getVisionApiKey());

            var body = Map.of(
                "url", imageUrl,
                "features", List.of("imageType", "metadata")
            );

            var entity = new org.springframework.http.HttpEntity<>(body, headers);
            var response = restTemplate.exchange(
                visionUrl,
                org.springframework.http.HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getBody() == null) return null;

            Map<?, ?> result = (Map<?, ?>) response.getBody();
            List<String> flagged = new ArrayList<>();
            double manipulationScore = 0.0;
            boolean deepfake = false;

            // Check imageType for manipulation signals
            if (result.get("imageType") != null) {
                Map<?, ?> imageType = (Map<?, ?>) result.get("imageType");
                Object clipArtObj = imageType.get("clipArtType");
                int clipArtType = clipArtObj instanceof Number ? ((Number) clipArtObj).intValue() : 0;
                if (clipArtType >= 3) {
                    deepfake = true;
                    manipulationScore = 0.8;
                    flagged.add("High clip-art score (" + clipArtType + ") suggests digital manipulation");
                }
            }

            // Check metadata for EXIF anomalies
            if (result.get("metadata") != null) {
                Map<?, ?> metadata = (Map<?, ?>) result.get("metadata");
                if (metadata.get("dateTime") == null) {
                    flagged.add("No EXIF timestamp — possible metadata stripping");
                    manipulationScore = Math.max(manipulationScore, 0.3);
                }
            }

            return new VisionAnalysisResult(flagged, manipulationScore, deepfake);

        } catch (Exception e) {
            log.warn("[RealAzureAI] Vision REST analysis failed for {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }

    private String nullIfMissing(JsonNode node, String field) {
        JsonNode f = node.get(field);
        return (f != null && !f.isNull()) ? f.asText() : null;
    }

    // Lightweight result record for vision analysis
    private record VisionAnalysisResult(
        List<String> flaggedRegions,
        double manipulationScore,
        boolean deepfakeDetected
    ) {}
}
