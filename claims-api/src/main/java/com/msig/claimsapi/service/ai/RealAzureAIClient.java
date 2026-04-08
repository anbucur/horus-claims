package com.msig.claimsapi.service.ai;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestAssistantMessage;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
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

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Real Azure AI client using the Azure AI Foundry SDK (azure-ai-projects v2.0.0).
 *
 * Uses AIProjectClient to obtain an OpenAI-compatible client (Chat Completions API)
 * for model inference. This replaces the deprecated azure-ai-inference SDK.
 *
 * Activates only when claims.azure.ai.enabled=true.
 *
 * Endpoint: https://<resource>.services.ai.azure.com/api/projects/<project>
 * Auth:     API key (AzureKeyCredential) OR DefaultAzureCredential (Entra ID)
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "claims.azure.ai.enabled", havingValue = "true")
public class RealAzureAIClient implements AIClient {

    private final AzureAIConfig config;
    private final ObjectMapper objectMapper;
    private OpenAIClient openAIClient;

    public RealAzureAIClient(AzureAIConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        String endpoint = resolveEndpoint();
        log.info("[RealAzureAI] Initialising OpenAIClient via AIProjectClientBuilder → {}", endpoint);

        OpenAIClientBuilder builder = new OpenAIClientBuilder().endpoint(endpoint);

        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            log.info("[RealAzureAI] Authenticating with AzureKeyCredential");
            builder.credential(new AzureKeyCredential(config.getApiKey()));
        } else {
            log.info("[RealAzureAI] Authenticating with DefaultAzureCredential (Entra ID)");
            builder.credential(new DefaultAzureCredentialBuilder().build());
        }

        this.openAIClient = builder.buildClient();
    }

    /**
     * Resolve the Foundry project endpoint.
     * The project endpoint should be:
     *   https://<resource>.services.ai.azure.com/api/projects/<project>
     */
    private String resolveEndpoint() {
        String ep = config.getProjectEndpoint();
        if (ep == null || ep.isBlank()) {
            throw new IllegalStateException(
                "Azure AI Foundry project endpoint is required. " +
                "Set claims.azure.ai.project-endpoint to " +
                "https://<resource>.services.ai.azure.com/api/projects/<project>");
        }
        return ep.endsWith("/") ? ep.substring(0, ep.length() - 1) : ep;
    }

    // ─── AIClient implementation ───────────────────────────────────────────

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
            "{ extractedName, matchedName, matchType (COMPANY|VESSEL|PERSON), confidence (0-1)}. " +
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

        try {
            String narrativeCheck =
                "You are an insurance image forensics specialist. " +
                "Assess whether the image at the URL is consistent with a marine hull damage claim. " +
                "Does the image show damage consistent with the incident? Respond with YES or NO and a brief reason.";

            for (String imageUrl : imageUrls) {
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
        return List.of();
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /**
     * Send a chat request to Azure AI Foundry GPT-4o via Chat Completions API.
     */
    private String chat(String systemPrompt, String userMessage) {
        List<ChatRequestMessage> messages = new ArrayList<>();
        messages.add(new ChatRequestSystemMessage(systemPrompt));
        messages.add(new ChatRequestUserMessage(userMessage));

        ChatCompletionsOptions options = new ChatCompletionsOptions(messages);
        options.setMaxTokens(500);
        options.setTemperature(0.1);

        ChatCompletions response = openAIClient.getChatCompletions(
            config.getModelDeployment(),
            options
        );

        String content = response.getChoices().get(0).getMessage().getContent();
        log.debug("[RealAzureAI] chat response: {}", content);
        return content;
    }

    /**
     * Send a chat request with an image URL reference (GPT-4o vision).
     */
    private String chatWithImage(String systemPrompt, String imageUrl) {
        List<ChatRequestMessage> messages = new ArrayList<>();
        messages.add(new ChatRequestSystemMessage(systemPrompt));
        messages.add(new ChatRequestUserMessage("Image: " + imageUrl + "\n\nAssess the image at the URL above."));

        ChatCompletionsOptions options = new ChatCompletionsOptions(messages);
        options.setMaxTokens(300);
        options.setTemperature(0.1);

        ChatCompletions response = openAIClient.getChatCompletions(
            config.getModelDeployment(),
            options
        );
        return response.getChoices().get(0).getMessage().getContent();
    }

    /**
     * Analyse a single image via Azure Computer Vision REST API (if configured).
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
            var response = new org.springframework.web.client.RestTemplate()
                .exchange(visionUrl, org.springframework.http.HttpMethod.POST, entity, Map.class);

            if (response.getBody() == null) return null;

            Map<?, ?> result = (Map<?, ?>) response.getBody();
            List<String> flagged = new ArrayList<>();
            double manipulationScore = 0.0;
            boolean deepfake = false;

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

    private record VisionAnalysisResult(
        List<String> flaggedRegions,
        double manipulationScore,
        boolean deepfakeDetected
    ) {}
}
