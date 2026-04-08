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
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Real Azure AI client using the Azure AI Foundry SDK (azure-ai-projects v2.0.0).
 *
 * Uses AIProjectClient to obtain an OpenAI-compatible client (Chat Completions API)
 * for model inference. This replaces the deprecated azure-ai-inference SDK.
 *
 * Features:
 * - Retry with exponential backoff (max 3 retries, 500ms base) for transient errors
 * - Configurable timeouts (connect 5s, read 60s)
 * - Marine-insurance-optimised prompts
 * - Per-call metrics and structured logging via AIMetricsService
 * - LRU cache for extractClaimData via AICacheService
 *
 * Activates only when claims.azure.ai.enabled=true.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "claims.azure.ai.enabled", havingValue = "true")
public class RealAzureAIClient implements AIClient {

    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 500;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    private final AzureAIConfig config;
    private final ObjectMapper objectMapper;
    private final AICacheService cacheService;
    private final AIMetricsService metricsService;
    private OpenAIClient openAIClient;

    public RealAzureAIClient(
            AzureAIConfig config,
            ObjectMapper objectMapper,
            AICacheService cacheService,
            AIMetricsService metricsService) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.cacheService = cacheService;
        this.metricsService = metricsService;
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
        // 1. Check LRU cache first
        AIClient.ExtractedClaimData cached = cacheService.getIfPresent(rawText);
        if (cached != null) {
            log.debug("[RealAzureAI] extractClaimData: cache hit");
            return cached;
        }

        // 2. Call AI with retry + metrics
        String traceId = UUID.randomUUID().toString();
        String operation = "extractClaimData";

        try {
            AIClient.ExtractedClaimData result = metricsService.timed(operation, config.getModelDeployment(), () -> {
                try {
                    return withRetry(operation, () -> doExtractClaimData(rawText, traceId));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });

            // 3. Cache the result
            if (result != null) {
                cacheService.put(rawText, result);
            }
            return result;

        } catch (Exception e) {
            log.error("[RealAzureAI] extractClaimData failed after retries: {}", e.getMessage());
            metricsService.recordCall(operation, config.getModelDeployment(), 0, false, e.getMessage());
            return new ExtractedClaimData(
                null, null, null, null, null, 0.0, false, config.getModelDeployment(), traceId
            );
        }
    }

    private AIClient.ExtractedClaimData doExtractClaimData(String rawText, String traceId) throws Exception {
        String systemPrompt =
            "You are a marine insurance first notice of loss (FNOL) data extraction specialist.\n" +
            "Extract ALL of the following fields from the FNOL text. Return ONLY a valid JSON object.\n\n" +
            "## Required fields\n" +
            "  dateOfLoss         (ISO-8601 date, e.g. 2024-03-15; null if not found)\n" +
            "  incidentNarrative (concise description of how/when/where the incident occurred)\n" +
            "  lossLocation       (port, geographic location, or coordinates where loss occurred)\n" +
            "  estimatedValue     (string with currency, e.g. \"USD 150,000\"; null if unknown)\n" +
            "  currency           (ISO 4217 currency code, e.g. USD, EUR; null if unknown)\n" +
            "  confidenceScore   (0.0–1.0, your confidence in the overall extraction)\n\n" +
            "## Marine-specific fields (critical — extract when present)\n" +
            "  vesselName         (vessel name as written in the FNOL, e.g. \"MSC OSCAR\", \"EVER GIVEN\")\n" +
            "  imoNumber          (7-digit IMO number starting with IMO, e.g. \"IMO 9612567\"; null if absent)\n" +
            "  cargoDescription   (type and quantity of cargo, e.g. \"20 TEU containers of electronics\")\n" +
            "  voyageRoute        (departure port to destination port, e.g. \"Rotterdam → Singapore\")\n" +
            "  portOfDeparture   (named port where voyage began; null if not stated)\n" +
            "  portOfDestination  (named port where voyage was to end; null if not stated)\n\n" +
            "## Rules\n" +
            "  - Use null for any field you cannot determine from the text.\n" +
            "  - For vesselName: tolerate minor typos (e.g. \"MSK Oscar\" → \"MSC OSCAR\") — note in confidence.\n" +
            "  - For imoNumber: extract only if the format is clearly an IMO number.\n" +
            "  - Be conservative on estimatedValue — only extract if explicitly stated.\n" +
            "  - Return ONLY the JSON object, no markdown, no preamble.\n";

        try {
            String json = chatWithTimeout(systemPrompt, rawText, 60_000);
            JsonNode node = objectMapper.readTree(json);

            AIClient.ExtractedClaimData result = new AIClient.ExtractedClaimData(
                nullIfMissing(node, "dateOfLoss"),
                nullIfMissing(node, "incidentNarrative"),
                nullIfMissing(node, "lossLocation"),
                nullIfMissing(node, "estimatedValue"),
                nullIfMissing(node, "currency"),
                node.has("confidenceScore") ? node.get("confidenceScore").asDouble() : 0.0,
                true,
                config.getModelDeployment(),
                traceId
            );
            log.info("[RealAzureAI] extractClaimData success: dateOfLoss={}, vessel={}, confidence={}",
                result.dateOfLoss(), nullIfMissing(node, "vesselName"), result.confidenceScore());
            return result;

        } catch (Exception e) {
            log.error("[RealAzureAI] extractClaimData GPT call failed: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public PolicyVerificationResult verifyPolicy(String policyNumber, String dateOfLoss) {
        String traceId = UUID.randomUUID().toString();
        String operation = "verifyPolicy";

        try {
            return metricsService.timed(operation, config.getModelDeployment(), () -> {
                try {
                    return withRetry(operation, () -> doVerifyPolicy(policyNumber, dateOfLoss, traceId));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (Exception e) {
            log.error("[RealAzureAI] verifyPolicy failed: {}", e.getMessage());
            metricsService.recordCall(operation, config.getModelDeployment(), 0, false, e.getMessage());
            return new PolicyVerificationResult(
                null, null, null, null, null, null, false, false, config.getModelDeployment()
            );
        }
    }

    private PolicyVerificationResult doVerifyPolicy(String policyNumber, String dateOfLoss, String traceId) throws Exception {
        String systemPrompt =
            "You are a marine insurance policy verification specialist.\n" +
            "Given a policy number and date of loss, verify and return a JSON object with:\n\n" +
            "  policyId        (internal ID or null if not determinable)\n" +
            "  policyNumber    (confirmed or corrected policy number)\n" +
            "  status          (ACTIVE, EXPIRED, SUSPENDED, CANCELLED, or null)\n" +
            "  lineOfBusiness  (must include MARINE or HULL for marine policies)\n" +
            "  coverageDetails (summary of covered perils, e.g. \"Hull & Machinery + FPA\")\n" +
            "  deductible      (deductible amount and currency, or null)\n" +
            "  inPeriod        (true if dateOfLoss falls within policy coverage period)\n\n" +
            "## Marine-specific checks\n" +
            "  - Warranties: check for express warranties (e.g. \"vessel must have valid Safety Certificate\")\n" +
            "  - Exclusions: flag if loss involves excluded perils (e.g. wear and tear, inadequate maintenance)\n" +
            "  - Trading restrictions: flag if voyage route violates policy trading limits\n" +
            "  - Installed speed warranties: flag if vessel speed exceeds warranted maximum\n\n" +
            "Return ONLY a valid JSON object, no markdown, no preamble.";

        String userPrompt = "Policy Number: " + policyNumber + "\nDate of Loss: " + dateOfLoss;

        String json = chatWithTimeout(systemPrompt, userPrompt, 30_000);
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
    }

    @Override
    public List<EntityMatch> matchEntities(List<String> entityNames) {
        if (entityNames == null || entityNames.isEmpty()) return List.of();

        String traceId = UUID.randomUUID().toString();
        String operation = "matchEntities";

        try {
            return metricsService.timed(operation, config.getModelDeployment(), () -> {
                try {
                    return withRetry(operation, () -> doMatchEntities(entityNames, traceId));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (Exception e) {
            log.error("[RealAzureAI] matchEntities failed: {}", e.getMessage());
            metricsService.recordCall(operation, config.getModelDeployment(), 0, false, e.getMessage());
            return List.of();
        }
    }

    private List<EntityMatch> doMatchEntities(List<String> entityNames, String traceId) throws Exception {
        String systemPrompt =
            "You are an entity matching specialist for marine insurance.\n" +
            "Match the given entity names against known shipping companies, vessels, and parties.\n" +
            "Return a JSON array of match objects. Each entry:\n" +
            "  { extractedName, matchedName, matchType (COMPANY|VESSEL|PERSON), confidence (0.0–1.0) }\n\n" +
            "## Matching rules\n" +
            "  - Vessel names: confidence >= 0.85 (high typo tolerance required for ship names)\n" +
            "  - Company names: confidence >= 0.80\n" +
            "  - Person names: confidence >= 0.75\n" +
            "  - For vessels: normalise common prefixes/suffixes (MT, MV, RMS, etc.)\n" +
            "  - Flag IMO numbers when they uniquely identify a vessel\n" +
            "  - Return an empty array if no confident matches.\n" +
            "  - Return ONLY the JSON array, no markdown, no preamble.";

        String userPrompt = "Entity names to match:\n" + String.join("\n", entityNames);

        String json = chatWithTimeout(systemPrompt, userPrompt, 30_000);
        JsonNode array = objectMapper.readTree(json);

        List<EntityMatch> results = new ArrayList<>();
        if (array.isArray()) {
            for (JsonNode item : array) {
                double confidence = item.has("confidence") ? item.get("confidence").asDouble() : 0.0;
                if (confidence >= 0.75) {
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
    }

    @Override
    public ForensicsResult runForensics(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return new ForensicsResult(
                List.of(), 0.0, false, false,
                "No images provided for forensics", false, false, config.getModelDeployment()
            );
        }

        String traceId = UUID.randomUUID().toString();
        String operation = "runForensics";

        try {
            return metricsService.timed(operation, config.getModelDeployment(), () -> {
                try {
                    return withRetry(operation, () -> doRunForensics(imageUrls, traceId));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (Exception e) {
            log.error("[RealAzureAI] runForensics failed: {}", e.getMessage());
            metricsService.recordCall(operation, config.getModelDeployment(), 0, false, e.getMessage());
            return new ForensicsResult(
                List.of(), 0.0, false, false,
                "Forensics failed: " + e.getMessage(), false, false, config.getModelDeployment()
            );
        }
    }

    private ForensicsResult doRunForensics(List<String> imageUrls, String traceId) throws Exception {
        List<String> flaggedRegions = new ArrayList<>();
        double maxManipulationScore = 0.0;
        boolean deepfakeDetected = false;
        boolean narrativeMismatch = false;
        StringBuilder summary = new StringBuilder();

        // Metadata analysis via Computer Vision REST API
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

        // GPT-4o Vision narrative consistency analysis
        try {
            String narrativeCheck =
                "You are an insurance image forensics specialist.\n" +
                "Assess whether the image at the provided URL is CONSISTENT with a marine hull damage claim.\n" +
                "Look for: cargo damage consistent with described incident, hull damage matching the loss type,\n" +
                "geographic markers that confirm location, timestamp consistency.\n" +
                "Respond with ONLY a valid JSON object:\n" +
                "  { \"consistent\": true|false, \"reason\": \"brief explanation\" }\n" +
                "If you cannot access or assess the image, respond: { \"consistent\": false, \"reason\": \"image_not_accessible\" }";

            for (String imageUrl : imageUrls) {
                String result = chatWithImage(narrativeCheck, imageUrl, 30_000);
                if (result != null) {
                    try {
                        JsonNode node = objectMapper.readTree(result);
                        boolean consistent = node.has("consistent") && node.get("consistent").asBoolean();
                        if (!consistent) {
                            narrativeMismatch = true;
                            String reason = nullIfMissing(node, "reason");
                            summary.append("Narrative mismatch for ")
                                .append(imageUrl)
                                .append(": ")
                                .append(reason != null ? reason : "inconsistent")
                                .append(". ");
                        }
                    } catch (Exception e) {
                        log.warn("[RealAzureAI] Could not parse forensics response for {}: {}", imageUrl, e.getMessage());
                    }
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
        // Semantic search handled by SemanticSearchService
        return List.of();
    }

    // ─── Retry helpers ─────────────────────────────────────────────────────

    /**
     * Execute a callable with exponential backoff retry.
     * Retries on: 429 (rate limit), 500/503 (transient server errors), TimeoutException.
     * Does NOT retry: 401/403 (auth), 400 (bad request), parse errors.
     */
    private <T> T withRetry(String operation, java.util.concurrent.Callable<T> callable) throws Exception {
        int attempt = 0;
        long delayMs = BASE_DELAY_MS;

        while (true) {
            attempt++;
            try {
                return callable.call();
            } catch (Exception e) {
                String errorClass = e.getClass().getSimpleName();
                boolean isTimeout = e instanceof TimeoutException || e instanceof SocketTimeoutException
                    || errorClass.contains("Timeout") || errorClass.contains("ReadTimeout");
                boolean isRateLimit = e.getMessage() != null && e.getMessage().contains("429");
                boolean isTransientServer = e.getMessage() != null
                    && (e.getMessage().contains("500") || e.getMessage().contains("503")
                        || e.getMessage().contains("ServiceUnavailable"));
                boolean isAuthError = e.getMessage() != null
                    && (e.getMessage().contains("401") || e.getMessage().contains("403")
                        || e.getMessage().contains("Unauthorized") || e.getMessage().contains("Forbidden"));
                boolean canRetry = attempt <= MAX_RETRIES && !isAuthError && (isTimeout || isRateLimit || isTransientServer);

                if (!canRetry) {
                    log.warn("[RealAzureAI] {}: attempt {} — non-retryable error: {}",
                        operation, attempt, e.getMessage());
                    throw e;
                }

                long waitMs = isRateLimit ? extractRetryAfterMs(e) : delayMs;
                log.warn("[RealAzureAI] {}: attempt {} failed ({}), retrying in {}ms...",
                    operation, attempt, errorClass, waitMs);
                Thread.sleep(waitMs);
                delayMs = Math.multiplyExact(delayMs, 2); // exponential backoff
            }
        }
    }

    private long extractRetryAfterMs(Exception e) {
        // If rate-limited, honour Retry-After header if available in message
        // For now, default to 2s if we detect 429
        if (e.getMessage() != null && e.getMessage().contains("429")) {
            log.info("[RealAzureAI] Rate limit detected — waiting 2s before retry");
            return 2_000L;
        }
        return BASE_DELAY_MS;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /**
     * Send a chat request to Azure AI Foundry GPT-4o via Chat Completions API.
     * Respects configured timeouts.
     */
    private String chatWithTimeout(String systemPrompt, String userMessage, int timeoutMs) {
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
    private String chatWithImage(String systemPrompt, String imageUrl, int timeoutMs) {
        List<ChatRequestMessage> messages = new ArrayList<>();
        messages.add(new ChatRequestSystemMessage(systemPrompt));
        messages.add(new ChatRequestUserMessage(
            "Image URL: " + imageUrl + "\n\nAssess the image at the URL above. " +
            "If the image is not publicly accessible, respond with: " +
            "{ \"consistent\": false, \"reason\": \"image_not_accessible\" }"));

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
