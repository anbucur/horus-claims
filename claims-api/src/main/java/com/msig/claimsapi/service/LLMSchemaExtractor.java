package com.msig.claimsapi.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msig.claimsapi.config.AzureAIConfig;
import com.msig.claimsdomain.model.DocumentExtractionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * LLM-powered schema-based document extraction using Azure AI Foundry SDK (azure-ai-projects).
 *
 * Pipeline:
 * 1. [DocumentExtractionService]  OCR + layout from Azure DI → raw text
 * 2. [LLMSchemaExtractor]         Send raw text + schema to GPT-4o → structured JSON with per-field confidence
 *
 * Uses azure-ai-openai (OpenAIClient) for Chat Completions, obtained via the
 * azure-ai-projects AIProjectClientBuilder pattern.
 *
 * Each schema field has a description to guide extraction, and the LLM is instructed to
 * assign a per-field confidence score based on how certain it is about the extracted value.
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "claims.azure.ai.enabled", havingValue = "true")
public class LLMSchemaExtractor {

    private final AzureAIConfig azureConfig;
    private final DocumentExtractionService docService;
    private final ObjectMapper objectMapper;
    private OpenAIClient openAIClient;

    public LLMSchemaExtractor(
            AzureAIConfig azureConfig,
            DocumentExtractionService docService,
            ObjectMapper objectMapper) {
        this.azureConfig = azureConfig;
        this.docService = docService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void init() {
        String endpoint = resolveEndpoint();
        log.info("[LLMSchemaExtractor] Initialising OpenAIClient via AIProjectClientBuilder → {}", endpoint);

        OpenAIClientBuilder builder = new OpenAIClientBuilder().endpoint(endpoint);

        if (azureConfig.getApiKey() != null && !azureConfig.getApiKey().isBlank()) {
            log.info("[LLMSchemaExtractor] Authenticating with AzureKeyCredential");
            builder.credential(new AzureKeyCredential(azureConfig.getApiKey()));
        } else {
            log.info("[LLMSchemaExtractor] Authenticating with DefaultAzureCredential (Entra ID)");
            builder.credential(new DefaultAzureCredentialBuilder().build());
        }

        this.openAIClient = builder.buildClient();
    }

    private String resolveEndpoint() {
        String ep = azureConfig.getProjectEndpoint();
        if (ep == null || ep.isBlank()) {
            throw new IllegalStateException(
                "Azure AI Foundry project endpoint is required. " +
                "Set claims.azure.ai.project-endpoint to " +
                "https://<resource>.services.ai.azure.com/api/projects/<project>");
        }
        return ep.endsWith("/") ? ep.substring(0, ep.length() - 1) : ep;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Extract structured data from a document's raw bytes according to the given schema.
     *
     * @param documentBytes  Raw file bytes (PDF, image, Office)
     * @param fileName      Display name for the document
     * @param schema        Extraction schema defining fields, types, and descriptions
     * @return DocumentExtractionResult with per-field confidence scores
     */
    public DocumentExtractionResult extract(byte[] documentBytes, String fileName,
                                            ExtractionSchema schema) {
        DocumentExtractionService.RawDocumentContent rawDoc =
            docService.analyseDocument(documentBytes, fileName);

        return extractFromRawDocument(rawDoc, fileName, schema);
    }

    /**
     * Extract from a URL-hosted document.
     */
    public DocumentExtractionResult extractFromUrl(String documentUrl, String fileName,
                                                   ExtractionSchema schema) {
        DocumentExtractionService.RawDocumentContent rawDoc =
            docService.analyseDocumentFromUrl(documentUrl, fileName);

        return extractFromRawDocument(rawDoc, fileName, schema);
    }

    /**
     * Extract from pre-extracted raw document content (from DI).
     */
    public DocumentExtractionResult extractFromRawDocument(
            DocumentExtractionService.RawDocumentContent rawDoc,
            String sourceDocumentId,
            ExtractionSchema schema) {

        if (!rawDoc.successful()) {
            return DocumentExtractionResult.builder()
                .extracted(false)
                .sourceDocumentId(sourceDocumentId)
                .modelUsed(azureConfig.getModelDeployment())
                .errorMessage("Document analysis failed: " + rawDoc.errorMessage())
                .build();
        }

        // Step 2: Convert DI output to LLM-friendly text
        String llmInput = docService.toLLMInputText(rawDoc);

        // Step 3: Build schema prompt
        String schemaPrompt = buildSchemaPrompt(schema);
        String userPrompt = schemaPrompt + "\n\n## Document Content\n" + llmInput;

        // Step 4: Call GPT-4o
        try {
            String response = callGPT4o(schema, userPrompt);
            return parseExtractionResult(response, sourceDocumentId, llmInput);

        } catch (Exception e) {
            log.error("[LLMSchemaExtractor] Extraction failed for {}: {}", sourceDocumentId, e.getMessage());
            return DocumentExtractionResult.builder()
                .extracted(false)
                .sourceDocumentId(sourceDocumentId)
                .modelUsed(azureConfig.getModelDeployment())
                .rawExtractedText(llmInput)
                .errorMessage("LLM extraction failed: " + e.getMessage())
                .build();
        }
    }

    // ─── Schema prompt builder ───────────────────────────────────────────────

    /**
     * Build the system prompt instructing GPT-4o how to extract each schema field
     * and return confidence scores.
     */
    private String buildSchemaPrompt(ExtractionSchema schema) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a precise insurance document data extraction specialist.\n");
        sb.append("Given the schema below and the document content, extract field values.\n");
        sb.append("For each field, assign a confidence score (0.0-1.0) based on how certain you are.\n\n");

        sb.append("## Extraction Schema\n");
        sb.append("Return ONLY a valid JSON object with this exact structure:\n");
        sb.append("{\n");
        sb.append("  \"fields\": {\n");

        List<ExtractionSchema.Field> fields = schema.getFields();
        for (int i = 0; i < fields.size(); i++) {
            ExtractionSchema.Field field = fields.get(i);
            // Field is a record — use record accessor (field.name()), not getter (field.getName())
            sb.append("    \"").append(field.name()).append("\": {\n");
            sb.append("      \"value\": \"<extracted value or null if not found>\",\n");
            sb.append("      \"confidenceScore\": 0.0-1.0,\n");
            sb.append("      \"sourceTextSpan\": \"<the exact text from the document used for extraction or null>\",\n");
            sb.append("      \"warning\": \"<any issue: 'not_found', 'ambiguous', 'contradictory', or null if clean>\"\n");
            sb.append("    }");
            if (i < fields.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("  },\n");
        sb.append("  \"overallConfidenceScore\": 0.0-1.0,\n");
        sb.append("  \"extractionWarnings\": [\"<any document-level warning or observation>\"]\n");
        sb.append("}\n\n");

        sb.append("## Field Definitions\n");
        for (ExtractionSchema.Field field : fields) {
            // Field is a record — use record accessors
            sb.append("- ").append(field.name())
              .append(" (").append(field.type()).append("): ")
              .append(field.description()).append("\n");
        }

        sb.append("\n## Rules\n");
        sb.append("- Use null (JSON null) if a field is not found in the document.\n");
        sb.append("- confidenceScore: 1.0 = completely certain, 0.5 = uncertain, 0.0 = not attempted.\n");
        sb.append("- sourceTextSpan: Copy the exact text snippet from the document that supports this extraction.\n");
        sb.append("- warning values: 'not_found' | 'ambiguous' | 'contradictory' | null.\n");
        sb.append("- overallConfidenceScore: Weighted average of field scores.\n");
        sb.append("- If the document is unreadable or empty, return all nulls with confidence 0.0.\n");
        sb.append("- Do NOT make up values. Only extract from the provided document content.\n");

        return sb.toString();
    }

    private String callGPT4o(ExtractionSchema schema, String userPrompt) {
        List<ChatRequestMessage> messages = new ArrayList<>();
        messages.add(new ChatRequestSystemMessage(
            "You are a precise insurance document data extraction specialist. " +
            "Return ONLY valid JSON matching the requested schema. " +
            "No markdown, no explanation, no preamble."
        ));
        messages.add(new ChatRequestUserMessage(userPrompt));

        ChatCompletionsOptions options = new ChatCompletionsOptions(messages);
        options.setMaxTokens(2000);
        options.setTemperature(0.1);

        ChatCompletions response = openAIClient.getChatCompletions(
            azureConfig.getModelDeployment(),
            options
        );

        String content = response.getChoices().get(0).getMessage().getContent();
        log.debug("[LLMSchemaExtractor] raw LLM response: {}", content);
        return content;
    }

    private DocumentExtractionResult parseExtractionResult(String json,
            String sourceDocumentId, String rawText) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);

            Map<String, DocumentExtractionResult.ExtractedField> fields = new HashMap<>();

            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> fieldMap =
                (Map<String, Map<String, Object>>) parsed.get("fields");

            if (fieldMap != null) {
                for (Map.Entry<String, Map<String, Object>> entry : fieldMap.entrySet()) {
                    Map<String, Object> fieldData = entry.getValue();
                    Object valueObj = fieldData.get("value");
                    String value = valueObj != null ? valueObj.toString() : null;

                    double confidence = 0.0;
                    Object confObj = fieldData.get("confidenceScore");
                    if (confObj instanceof Number) {
                        confidence = ((Number) confObj).doubleValue();
                    }

                    String sourceSpan = fieldData.get("sourceTextSpan") != null
                        ? fieldData.get("sourceTextSpan").toString()
                        : null;

                    String warning = fieldData.get("warning") != null
                        ? fieldData.get("warning").toString()
                        : null;

                    fields.put(entry.getKey(), DocumentExtractionResult.ExtractedField.builder()
                        .value(value)
                        .confidenceScore(confidence)
                        .sourceTextSpan(sourceSpan)
                        .warning(warning)
                        .build());
                }
            }

            double overallScore = 0.0;
            Object overallObj = parsed.get("overallConfidenceScore");
            if (overallObj instanceof Number) {
                overallScore = ((Number) overallObj).doubleValue();
            }

            return DocumentExtractionResult.builder()
                .extracted(true)
                .sourceDocumentId(sourceDocumentId)
                .modelUsed(azureConfig.getModelDeployment())
                .fields(fields)
                .overallConfidenceScore(overallScore)
                .rawExtractedText(rawText)
                .build();

        } catch (Exception e) {
            log.error("[LLMSchemaExtractor] Failed to parse LLM response as JSON: {}\nResponse was: {}",
                e.getMessage(), json);
            return DocumentExtractionResult.builder()
                .extracted(false)
                .sourceDocumentId(sourceDocumentId)
                .modelUsed(azureConfig.getModelDeployment())
                .rawExtractedText(rawText)
                .errorMessage("JSON parse error: " + e.getMessage() + "\nRaw response: " + json)
                .build();
        }
    }

    // ─── Schema definition ───────────────────────────────────────────────────

    /**
     * Extraction schema definition — passed to extract() to guide GPT-4o.
     *
     * Example usage:
     * <pre>
     * ExtractionSchema schema = ExtractionSchema.builder()
     *     .addField("dateOfLoss", "string", "Date of the incident/loss in ISO-8601 or natural language")
     *     .addField("incidentNarrative", "string", "Full description of how the incident occurred")
     *     .addField("estimatedLoss", "string", "Estimated monetary value of the loss with currency")
     *     .addField("vesselName", "string", "Name of the vessel involved in the incident")
     *     .addField("location", "string", "Geographic location where the incident occurred")
     *     .build();
     * </pre>
     */
    public static class ExtractionSchema {
        private final List<Field> fields;

        private ExtractionSchema(List<Field> fields) {
            this.fields = fields;
        }

        public static ExtractionSchemaBuilder builder() { return new ExtractionSchemaBuilder(); }

        public List<Field> getFields() { return fields; }

        public static class ExtractionSchemaBuilder {
            private final List<Field> fields = new ArrayList<>();

            public ExtractionSchemaBuilder addField(String name, String type, String description) {
                fields.add(new Field(name, type, description));
                return this;
            }

            public ExtractionSchema build() {
                return new ExtractionSchema(List.copyOf(fields));
            }
        }

        /** Java record — use field.name(), field.type(), field.description() (not getters) */
        public record Field(String name, String type, String description) {}
    }
}
