package com.msig.claimsapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Azure AI Foundry configuration.
 *
 * Used by RealAzureAIClient and LLMSchemaExtractor via the azure-ai-projects SDK (v2.0.0).
 *
 * Environment variables / Spring properties:
 * - claims.azure.ai.enabled             : Enable AI features (default: false)
 * - claims.azure.ai.project-endpoint     : Foundry project endpoint
 *                                          e.g. https://<resource>.services.ai.azure.com/api/projects/<project>
 *                                          (replaces the old ML Studio services.azureml.ms endpoint)
 * - claims.azure.ai.api-key            : API key (optional — omit for DefaultAzureCredential / Entra ID)
 * - claims.azure.ai.model-deployment    : Model deployment name (e.g. gpt-4o, gpt-4o-mini)
 *
 * Azure AI Search (semantic similarity):
 * - claims.azure.ai.search-endpoint     : Azure AI Search endpoint
 * - claims.azure.ai.search-api-key      : Search API key
 * - claims.azure.ai.search-index       : Index name (default: claims)
 *
 * Azure Computer Vision (image forensics):
 * - claims.azure.ai.vision-endpoint     : Computer Vision endpoint
 * - claims.azure.ai.vision-api-key      : Vision API key
 */
@Configuration
@ConfigurationProperties(prefix = "claims.azure.ai")
@Data
public class AzureAIConfig {
    private boolean enabled = false;

    // Azure AI Foundry project endpoint
    // e.g. https://my-resource.services.ai.azure.com/api/projects/my-project
    // This is the Foundry SDK v2.0.0 project endpoint (NOT the old ML Studio azureml.ms endpoint).
    private String projectEndpoint;

    // API key — omit to use DefaultAzureCredential (Entra ID)
    private String apiKey;

    // Model deployment name (e.g. gpt-4o, gpt-4o-mini)
    private String modelDeployment = "gpt-4o";

    // Azure AI Search
    private String searchEndpoint;
    private String searchApiKey;
    private String searchIndex = "claims";

    // Azure Computer Vision (for image forensics)
    private String visionEndpoint;
    private String visionApiKey;
    private String visionApiVersion = "2024-05-01-preview";

    private int maxRetries = 3;
    private int timeoutSeconds = 30;
}
