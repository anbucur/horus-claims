package com.msig.claimsapi.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msig.claimsapi.config.AzureAIConfig;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.model.*;
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
public class ReActClaimAgentImpl implements ReActClaimAgent {

    private final AzureAIConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public ClaimRecommendation recommend(Claim claim, ClaimContext context) {
        String traceId = UUID.randomUUID().toString();
        try {
            String prompt = buildPrompt(claim, context);
            String jsonResponse = callGPT4o(prompt);
            return parseRecommendation(jsonResponse, traceId);
        } catch (Exception e) {
            log.error("ReAct agent failed for claim {}: {}", claim.getId(), e.getMessage());
            return new ClaimRecommendation(
                "HITL",
                "AI unavailable, defaulting to human review: " + e.getMessage(),
                0.0,
                List.of("AI reasoning failed"),
                traceId
            );
        }
    }

    private String buildPrompt(Claim claim, ClaimContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert marine insurance claims underwriter.\n");
        sb.append("Analyze the following claim and recommend: STP (auto-approve), HITL (human review), or SIU (special investigations unit).\n\n");
        sb.append("Claim ID: ").append(claim.getId()).append("\n");
        sb.append("Loss Location: ").append(claim.getLossLocation()).append("\n");
        sb.append("Incident Narrative: ").append(claim.getIncidentNarrative()).append("\n\n");

        if (ctx.extractedData() != null) {
            ExtractedClaimData ed = ctx.extractedData();
            sb.append("AI Extracted Data:\n");
            sb.append("- Date of Loss: ").append(ed.getDateOfLoss()).append("\n");
            sb.append("- Narrative: ").append(ed.getIncidentNarrative()).append("\n");
            sb.append("- Estimated Loss: ").append(ed.getEstimatedLoss()).append(" ").append(ed.getCurrency() != null ? ed.getCurrency() : "").append("\n");
            sb.append("- Confidence: ").append(ed.getConfidenceScore()).append("\n\n");
        }

        if (ctx.policyResult() != null) {
            PolicyVerificationResult pv = ctx.policyResult();
            sb.append("Policy Verification:\n");
            sb.append("- Policy Active: ").append(pv.isPolicyActive()).append("\n");
            sb.append("- Coverage Valid: ").append(pv.isCoverageValid()).append("\n");
            sb.append("- Deductible Satisfied: ").append(pv.isDeductibleSatisfied()).append("\n");
            sb.append("- Line of Business: ").append(pv.getLineOfBusinessMatch()).append("\n\n");
        }

        if (ctx.entityResult() != null) {
            EntityMatchResult er = ctx.entityResult();
            sb.append("Entity Matching:\n");
            sb.append("- All Entities Matched: ").append(er.isAllEntitiesMatched()).append("\n");
            sb.append("- Confidence: ").append(er.getConfidenceScore()).append("\n");
            sb.append("- Assured: ").append(er.getAssuredName()).append("\n");
            sb.append("- Vessel: ").append(er.getVesselNames()).append("\n\n");
        }

        if (ctx.forensicsResult() != null) {
            ForensicsResult fr = ctx.forensicsResult();
            sb.append("Image Forensics:\n");
            sb.append("- Overall Score: ").append(fr.getOverallScore()).append("\n");
            sb.append("- Quarantine Recommended: ").append(fr.isQuarantineRecommended()).append("\n");
            sb.append("- Anomaly Flags: ").append(fr.getAnomalyFlags()).append("\n\n");
        }

        if (ctx.duplicates() != null && !ctx.duplicates().isEmpty()) {
            sb.append("Duplicate Alerts: ").append(ctx.duplicates().size()).append(" potential duplicate(s) found\n");
            for (DuplicateMatch dm : ctx.duplicates()) {
                sb.append("  - Similar to claim ").append(dm.getClaimId())
                  .append(" (similarity: ").append(dm.getSimilarityScore()).append(")\n");
            }
            sb.append("\n");
        }

        if (ctx.similarClaims() != null && !ctx.similarClaims().isEmpty()) {
            sb.append("Similar Past Claims: ").append(ctx.similarClaims().size()).append(" similar claim(s) found\n");
            for (ClaimSimilarityResult csr : ctx.similarClaims()) {
                sb.append("  - Claim ").append(csr.claimId())
                  .append(" (score: ").append(csr.similarityScore()).append(")\n");
            }
            sb.append("\n");
        }

        sb.append("\nReturn ONLY a JSON object with this exact structure:\n");
        sb.append("{\"decision\": \"STP|HITL|SIU\", \"reasoning\": \"...\", \"confidence\": 0.0-1.0, \"considerations\": [\"...\", \"...\"]}\n");
        sb.append("Rules:\n");
        sb.append("- Only recommend STP if confidence > 0.90, policy is active, no forensics red flags, and no duplicates.\n");
        sb.append("- Recommend SIU if deepfake detected, high manipulation score (>0.7), or narrative mismatch.\n");
        sb.append("- Default to HITL if uncertain.\n");
        return sb.toString();
    }

    private String callGPT4o(String prompt) {
        String url = config.getOpenAiEndpoint()
            + "/openai/deployments/" + config.getOpenAiDeployment()
            + "/chat/completions?api-version=" + config.getOpenAiApiVersion();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", config.getOpenAiApiKey());

        Map<String, Object> body = new HashMap<>();
        body.put("model", config.getOpenAiDeployment());
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("max_tokens", 600);
        body.put("temperature", 0.1);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        Map<?, ?> responseBody = response.getBody();
        if (responseBody == null) throw new RestClientException("Empty response from Azure OpenAI");

        List<?> choices = (List<?>) responseBody.get("choices");
        if (choices == null || choices.isEmpty()) throw new RestClientException("No choices in response");

        Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
        return message != null ? (String) message.get("content") : "{}";
    }

    private ClaimRecommendation parseRecommendation(String json, String traceId) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            String decision = (String) parsed.get("decision");
            String reasoning = (String) parsed.get("reasoning");
            double confidence = parsed.get("confidence") != null
                ? ((Number) parsed.get("confidence")).doubleValue()
                : 0.5;

            List<String> considerations = new ArrayList<>();
            if (parsed.get("considerations") != null) {
                considerations = new ArrayList<>((List<String>) parsed.get("considerations"));
            }

            // Validate decision
            if (!List.of("STP", "HITL", "SIU").contains(decision)) {
                decision = "HITL";
            }

            return new ClaimRecommendation(decision, reasoning, confidence, considerations, traceId);
        } catch (Exception e) {
            log.warn("Failed to parse ReAct response, defaulting to HITL: {}", e.getMessage());
            return new ClaimRecommendation(
                "HITL", "Parse error, defaulting to human review", 0.0, List.of(), traceId
            );
        }
    }
}
