package com.msig.claimsapi.config;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Azure Document Intelligence configuration.
 *
 * Uses the official azure-ai-documentintelligence SDK (v1.0.7, API 2024-11-30).
 * This replaces the older azure-ai-formrecognizer library which only supported API v2023-07-31.
 *
 * Environment variables:
 * - DOCUMENT_INTELLIGENCE_ENDPOINT : e.g. https://<resource>.cognitiveservices.azure.com/
 * - DOCUMENT_INTELLIGENCE_API_KEY  : API key (or omit for DefaultAzureCredential)
 * - DOCUMENT_INTELLIGENCE_MODEL_ID : Model ID (default: prebuilt-layout)
 *                                     Use prebuilt-layout for general layout/OCR,
 *                                     prebuilt-receipt, prebuilt-invoice, prebuilt-idDocument for specific types.
 */
@Configuration
@ConfigurationProperties(prefix = "claims.document-intelligence")
@Data
@Slf4j
public class DocumentIntelligenceConfig {
    private boolean enabled = false;
    private String endpoint;
    private String apiKey;
    /** Model ID - default is "prebuilt-layout" (general layout + OCR, replaces retired prebuilt-document) */
    private String modelId = "prebuilt-layout";

    /**
     * Build a synchronous DocumentIntelligenceClient.
     * Supports both API-key auth and DefaultAzureCredential (Entra ID).
     */
    @Bean
    public DocumentIntelligenceClient documentIntelligenceClient() {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException(
                "Document Intelligence endpoint is required. Set DOCUMENT_INTELLIGENCE_ENDPOINT.");
        }

        log.info("[DocIntel] Building DocumentIntelligenceClient → {}", endpoint);

        DocumentIntelligenceClientBuilder builder = new DocumentIntelligenceClientBuilder()
            .endpoint(endpoint);

        if (apiKey != null && !apiKey.isBlank()) {
            log.info("[DocIntel] Authenticating with AzureKeyCredential");
            builder.credential(new AzureKeyCredential(apiKey));
        } else {
            log.info("[DocIntel] Authenticating with DefaultAzureCredential (Entra ID)");
            TokenCredential credential = new DefaultAzureCredentialBuilder().build();
            builder.credential(credential);
        }

        return builder.buildClient();
    }
}
