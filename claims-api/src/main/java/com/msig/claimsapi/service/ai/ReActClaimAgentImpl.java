package com.msig.claimsapi.service.ai;

import com.azure.ai.inference.ChatCompletionsClient;
import com.azure.ai.inference.ChatCompletionsClientBuilder;
import com.azure.ai.inference.models.ChatCompletionsOptions;
import com.azure.ai.inference.models.ChatCompletions;
import com.azure.ai.inference.models.ChatRequestMessage;
import com.azure.ai.inference.models.ChatRequestUserMessage;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msig.claimsapi.config.AzureAIConfig;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * ReAct-style claim routing agent backed by Azure AI Foundry GPT-4o.
 * Activates only when claims.azure.ai.enabled=true.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "claims.azure.ai.enabled", havingValue = "true")
public class ReActClaimAgentImpl implements ReActClaimAgent {

    private final AzureAIConfig config;
    private final ObjectMapper objectMapper;
    private ChatCompletionsClient chatClient;

    @PostConstruct
    void init() {
        String base = config.getProjectEndpoint();
        if (base == null || base.isBlank()) {
            throw new IllegalStateException("AZURE_AI_PROJECT_ENDPOINT is required when claims.azure.ai.enabled=true");
        }
        if (!base.endsWith("/")) base += "/";
        String endpoint = base + config.getApiPath() + "/" + config.getModelDeployment();

        log.info("[ReActAgent] Connecting to Azure AI Foundry → {}", endpoint);

        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            this.chatClient = new ChatCompletionsClientBuilder()
                .credential(new AzureKeyCredential(config.getApiKey()))
                .endpoint(endpoint)
                .buildClient();
        } else {
            TokenCredential credential = new DefaultAzureCredentialBuilder().build();
            this.chatClient = new ChatCompletionsClientBuilder()
                .credential(credential)
                .endpoint(endpoint)
                .buildClient();
        }
    }

    @Override
    public ClaimRecommendation recommend(Claim claim, ClaimContext context) {
        String traceId = UUID.randomUUID().toString();
        try {
            String prompt = buildPrompt(claim, context);
            String json = callGPT4o(prompt);
            return parseRecommendation(json, traceId);
        } catch (Exception e) {
            log.error("[ReActAgent] Failed for claim {}: {}", claim.getId(), e.getMessage());
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
        sb.append("Analyse the following claim and recommend one of three routing decisions:\n");
        sb.append("  STP  — Auto-approve (straight-through processing)\n");
        sb.append("  HITL — Route to human-in-the-loop review\n");
        sb.append("  SIU  — Route to special investigations unit\n\n");
        sb.append("Claim ID: ").append(claim.getId()).append("\n");
        sb.append("Loss Location: ").append(claim.getLossLocation()).append("\n");
        sb.append("Incident Narrative: ").append(claim.getIncidentNarrative()).append("\n\n");

        if (ctx.extractedData() != null) {
            ExtractedClaimData ed = ctx.extractedData();
            sb.append("AI Extracted Data:\n");
            sb.append("  Date of Loss: ").append(ed.getDateOfLoss()).append("\n");
            sb.append("  Narrative: ").append(ed.getIncidentNarrative()).append("\n");
            sb.append("  Estimated Loss: ").append(ed.getEstimatedLoss())
              .append(" ").append(ed.getCurrency() != null ? ed.getCurrency() : "").append("\n");
            sb.append("  Confidence: ").append(ed.getConfidenceScore()).append("\n\n");
        }

        if (ctx.policyResult() != null) {
            PolicyVerificationResult pv = ctx.policyResult();
            sb.append("Policy Verification:\n");
            sb.append("  Policy Active: ").append(pv.isPolicyActive()).append("\n");
            sb.append("  Coverage Valid: ").append(pv.isCoverageValid()).append("\n");
            sb.append("  Deductible Satisfied: ").append(pv.isDeductibleSatisfied()).append("\n");
            sb.append("  Line of Business: ").append(pv.getLineOfBusinessMatch()).append("\n\n");
        }

        if (ctx.entityResult() != null) {
            EntityMatchResult er = ctx.entityResult();
            sb.append("Entity Matching:\n");
            sb.append("  All Entities Matched: ").append(er.isAllEntitiesMatched()).append("\n");
            sb.append("  Confidence: ").append(er.getConfidenceScore()).append("\n");
            sb.append("  Assured: ").append(er.getAssuredName()).append("\n");
            sb.append("  Vessels: ").append(er.getVesselNames()).append("\n\n");
        }

        if (ctx.forensicsResult() != null) {
            ForensicsResult fr = ctx.forensicsResult();
            sb.append("Image Forensics:\n");
            sb.append("  Overall Score: ").append(fr.getOverallScore()).append("\n");
            sb.append("  Quarantine Recommended: ").append(fr.isQuarantineRecommended()).append("\n");
            sb.append("  Anomaly Flags: ").append(fr.getAnomalyFlags()).append("\n\n");
        }

        if (ctx.duplicates() != null && !ctx.duplicates().isEmpty()) {
            sb.append("Potential Duplicates: ").append(ctx.duplicates().size()).append(" found\n");
            for (DuplicateMatch dm : ctx.duplicates()) {
                sb.append("  - Claim ").append(dm.getClaimId())
                  .append(" (similarity: ").append(dm.getSimilarityScore()).append(")\n");
            }
            sb.append("\n");
        }

        if (ctx.similarClaims() != null && !ctx.similarClaims().isEmpty()) {
            sb.append("Similar Past Claims: ").append(ctx.similarClaims().size()).append(" found\n");
            for (ClaimSimilarityResult csr : ctx.similarClaims()) {
                sb.append("  - Claim ").append(csr.claimId())
                  .append(" (score: ").append(csr.similarityScore()).append(")\n");
            }
            sb.append("\n");
        }

        sb.append("Return ONLY a valid JSON object with exactly this structure:\n");
        sb.append("{\"decision\": \"STP|HITL|SIU\", \"reasoning\": \"...\", \"confidence\": 0.0-1.0, \"considerations\": [\"...\", \"...\"]}\n\n");
        sb.append("Decision rules:\n");
        sb.append("  - STP: confidence > 0.90, policy active, no forensics red flags, no duplicates.\n");
        sb.append("  - SIU: deepfake detected, quarantine recommended, high anomaly flags, or narrative mismatch.\n");
        sb.append("  - HITL: all other cases (default).\n");
        return sb.toString();
    }

    private String callGPT4o(String prompt) {
        List<ChatRequestMessage> messages = new ArrayList<>();
        messages.add(new ChatRequestUserMessage(prompt));

        ChatCompletionsOptions options = new ChatCompletionsOptions(messages);
        options.setMaxTokens(600);
        options.setTemperature(0.1);
        options.setModel(config.getModelDeployment());

        ChatCompletions response = chatClient.complete(options);

        String content = response.getChoice().getMessage().getContent();
        log.debug("[ReActAgent] raw response: {}", content);
        return content;
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

            if (!List.of("STP", "HITL", "SIU").contains(decision)) {
                decision = "HITL";
            }

            return new ClaimRecommendation(decision, reasoning, confidence, considerations, traceId);
        } catch (Exception e) {
            log.warn("[ReActAgent] Failed to parse recommendation JSON: {}", e.getMessage());
            return new ClaimRecommendation(
                "HITL", "Parse error, defaulting to human review", 0.0, List.of(), traceId
            );
        }
    }
}
