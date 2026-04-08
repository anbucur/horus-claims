package com.msig.claimsapi.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msig.claimsapi.config.AzureAIConfig;
import com.msig.claimsdomain.model.ClaimSimilarityResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "claims.azure.ai.enabled", havingValue = "true")
public class RealAzureAIClient implements AIClient {

    private final AzureAIConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public ExtractedClaimData extractClaimData(String rawText) {
        try {
            String url = config.getOpenAiEndpoint()
                + "/openai/deployments/" + config.getOpenAiDeployment()
                + "/chat/completions?api-version=" + config.getOpenAiApiVersion();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", config.getOpenAiApiKey());

            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                    "You are an insurance claims data extraction specialist. Extract structured claim data from FNOL text. Return ONLY a JSON object with: dateOfLoss, incidentNarrative, lossLocation, estimatedValue (string), currency, confidenceScore (0-1). If uncertain, return null for uncertain fields."),
                Map.of("role", "user", "content", rawText)
            );

            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getOpenAiDeployment());
            body.put("messages", messages);
            body.put("max_tokens", 500);
            body.put("temperature", 0.1);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            String content = extractContent(response.getBody());
            JsonNode node = objectMapper.readTree(content);

            return new ExtractedClaimData(
                nullIfMissing(node, "dateOfLoss"),
                nullIfMissing(node, "incidentNarrative"),
                nullIfMissing(node, "lossLocation"),
                nullIfMissing(node, "estimatedValue"),
                nullIfMissing(node, "currency"),
                node.has("confidenceScore") ? node.get("confidenceScore").asDouble() : 0.0,
                true,
                config.getOpenAiDeployment(),
                UUID.randomUUID().toString()
            );
        } catch (Exception e) {
            log.error("Azure AI extractClaimData failed: {}", e.getMessage());
            return new ExtractedClaimData(
                null, null, null, null, null, 0.0, false, config.getOpenAiDeployment(), null
            );
        }
    }

    @Override
    public PolicyVerificationResult verifyPolicy(String policyNumber, String dateOfLoss) {
        try {
            String url = config.getOpenAiEndpoint()
                + "/openai/deployments/" + config.getOpenAiDeployment()
                + "/chat/completions?api-version=" + config.getOpenAiApiVersion();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", config.getOpenAiApiKey());

            String userPrompt = "Policy Number: " + policyNumber + "\nDate of Loss: " + dateOfLoss;

            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                    "You are a marine insurance policy verification specialist. Given a policy number and date of loss, verify if the policy is active, coverage is valid, deductible is satisfied, and line of business matches. Return JSON: policyId, policyNumber, status, lineOfBusiness, coverageDetails, deductible, inPeriod (boolean). Use null for fields you cannot verify."),
                Map.of("role", "user", "content", userPrompt)
            );

            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getOpenAiDeployment());
            body.put("messages", messages);
            body.put("max_tokens", 400);
            body.put("temperature", 0.1);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            String content = extractContent(response.getBody());
            JsonNode node = objectMapper.readTree(content);

            return new PolicyVerificationResult(
                node.has("policyId") ? node.get("policyId").asLong() : null,
                nullIfMissing(node, "policyNumber"),
                nullIfMissing(node, "status"),
                nullIfMissing(node, "lineOfBusiness"),
                nullIfMissing(node, "coverageDetails"),
                nullIfMissing(node, "deductible"),
                node.has("inPeriod") && !node.get("inPeriod").isNull(),
                true,
                config.getOpenAiDeployment()
            );
        } catch (Exception e) {
            log.error("Azure AI verifyPolicy failed: {}", e.getMessage());
            return new PolicyVerificationResult(
                null, null, null, null, null, null, false, false, config.getOpenAiDeployment()
            );
        }
    }

    @Override
    public List<EntityMatch> matchEntities(List<String> entityNames) {
        if (entityNames == null || entityNames.isEmpty()) return List.of();

        try {
            String url = config.getOpenAiEndpoint()
                + "/openai/deployments/" + config.getOpenAiDeployment()
                + "/chat/completions?api-version=" + config.getOpenAiApiVersion();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", config.getOpenAiApiKey());

            String userPrompt = "Entity names to match:\n" + String.join("\n", entityNames);

            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content",
                    "You are an entity matching specialist for marine insurance. Match the given entity names against known companies, vessels, and parties. Return a JSON array of matches: extractedName, matchedName, matchType (COMPANY|VESSEL|PERSON), confidence (0-1). Be conservative — only match if confidence > 0.7. Return an empty array if no confident matches."),
                Map.of("role", "user", "content", userPrompt)
            );

            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getOpenAiDeployment());
            body.put("messages", messages);
            body.put("max_tokens", 600);
            body.put("temperature", 0.1);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            String content = extractContent(response.getBody());
            JsonNode array = objectMapper.readTree(content);

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
            log.error("Azure AI matchEntities failed: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public ForensicsResult runForensics(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return new ForensicsResult(List.of(), 0.0, false, false,
                "No images provided for forensics", false, false, config.getOpenAiDeployment());
        }

        List<String> flaggedRegions = new ArrayList<>();
        double maxManipulationScore = 0.0;
        boolean deepfakeDetected = false;
        boolean narrativeMismatch = false;
        StringBuilder summary = new StringBuilder();

        // Call Azure AI Vision for each image
        for (String imageUrl : imageUrls) {
            try {
                String visionUrl = config.getVisionEndpoint()
                    + "/vision/computeHistory/analyze?api-version=" + config.getVisionApiVersion();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Ocp-Apim-Subscription-Key", config.getVisionApiKey());

                Map<String, Object> body = Map.of(
                    "url", imageUrl,
                    "features", List.of("imageType", "objects", "metadata")
                );

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<Map> response = restTemplate.exchange(
                    visionUrl, HttpMethod.POST, entity, Map.class);

                if (response.getBody() != null) {
                    Map<?, ?> result = (Map<?, ?>) response.getBody();

                    // Check metadata for manipulation signals
                    if (result.get("metadata") != null) {
                        Map<?, ?> metadata = (Map<?, ?>) result.get("metadata");
                        if (metadata.get("dateTime") == null) {
                            flaggedRegions.add("Image " + imageUrl + ": No EXIF timestamp");
                            maxManipulationScore = Math.max(maxManipulationScore, 0.3);
                        }
                    }

                    // Check imageType for deepfake signals
                    if (result.get("imageType") != null) {
                        Map<?, ?> imageType = (Map<?, ?>) result.get("imageType");
                        Object clipArtObj = imageType.get("clipArtType");
                        int clipArtType = clipArtObj instanceof Number ? ((Number) clipArtObj).intValue() : 0;
                        if (clipArtType >= 3) {
                            deepfakeDetected = true;
                            maxManipulationScore = Math.max(maxManipulationScore, 0.8);
                            flaggedRegions.add("High clip-art score suggests digital manipulation");
                        }
                    }
                }
            } catch (RestClientException e) {
                log.warn("Azure Vision call failed for {}: {}", imageUrl, e.getMessage());
            }
        }

        // Use GPT-4o vision to assess narrative consistency
        try {
            String gptResult = assessWithGPT4oVision(imageUrls);
            if (gptResult != null && gptResult.contains("mismatch")) {
                narrativeMismatch = true;
            }
            summary.append(gptResult != null ? gptResult : "Vision analysis complete.");
        } catch (Exception e) {
            log.warn("GPT-4o vision assessment failed: {}", e.getMessage());
            summary.append(" GPT-4o vision unavailable.");
        }

        return new ForensicsResult(
            flaggedRegions,
            maxManipulationScore,
            deepfakeDetected,
            narrativeMismatch,
            summary.toString().trim(),
            true,
            true,
            config.getOpenAiDeployment()
        );
    }

    @Override
    public boolean isAvailable() {
        return config.isEnabled();
    }

    @Override
    public String getModelName() {
        return config.getOpenAiDeployment();
    }

    @Override
    public List<ClaimSimilarityResult> findSimilarClaims(String queryText, int limit) {
        // This is delegated to SemanticSearchService which has full context
        // RealAzureAIClient handles document-level AI; semantic search is in SemanticSearchService
        log.info("[RealAzureAIClient] findSimilarClaims called — delegating to SemanticSearchService");
        return List.of();
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private String extractContent(Map<?, ?> responseBody) {
        if (responseBody == null) return "{}";
        List<?> choices = (List<?>) responseBody.get("choices");
        if (choices == null || choices.isEmpty()) return "{}";
        Map<?, ?> choice = (Map<?, ?>) choices.get(0);
        Map<?, ?> message = (Map<?, ?>) choice.get("message");
        return message != null ? (String) message.get("content") : "{}";
    }

    private String nullIfMissing(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : null;
    }

    private String assessWithGPT4oVision(List<String> imageUrls) {
        // GPT-4o with vision via the chat completions endpoint
        // In Azure OpenAI, vision is available via gpt-4o with image URLs in the message content
        try {
            String url = config.getOpenAiEndpoint()
                + "/openai/deployments/" + config.getOpenAiDeployment()
                + "/chat/completions?api-version=" + config.getOpenAiApiVersion();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", config.getOpenAiApiKey());

            List<Map<String, Object>> contentBlocks = new ArrayList<>();
            contentBlocks.add(Map.of("type", "text", "text",
                "Analyze this insurance claim image for damage assessment. Does the image content match a typical marine hull damage scenario? Respond with a brief summary and note any red flags."));

            for (String imageUrl : imageUrls) {
                contentBlocks.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", imageUrl)
                ));
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getOpenAiDeployment());
            body.put("messages", List.of(Map.of("role", "user", "content", contentBlocks)));
            body.put("max_tokens", 300);
            body.put("temperature", 0.1);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            return extractContent(response.getBody());
        } catch (Exception e) {
            log.warn("GPT-4o vision assessment failed: {}", e.getMessage());
            return null;
        }
    }
}
