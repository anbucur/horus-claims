package com.msig.claimsapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Azure AI Foundry configuration.
 * Used by RealAzureAIClient and ReActClaimAgentImpl via the azure-ai-inference SDK.
 *
 * Environment variables:
 * - AZURE_AI_PROJECT_ENDPOINT  : Azure AI Foundry project endpoint (e.g. https://my-project.services.azureml.ms/)
 * - AZURE_AI_API_KEY            : API key (optional if using DefaultAzureCredential)
 * - AZURE_OPENAI_DEPLOYMENT      : Model deployment name (e.g. gpt-4o)
 * - AZURE_OPENAI_API_VERSION     : API version (default 2024-06-01)
 * - AZURE_VISION_ENDPOINT        : Azure Computer Vision endpoint
 * - AZURE_VISION_API_KEY         : Computer Vision API key
 * - AZURE_SEARCH_ENDPOINT         : Azure AI Search endpoint
 * - AZURE_SEARCH_API_KEY          : Azure AI Search API key
 * - AZURE_SEARCH_INDEX            : Search index name (default claims)
 */
@Configuration
@ConfigurationProperties(prefix = "claims.azure.ai")
@Data
public class AzureAIConfig {
    private boolean enabled = false;

    // Azure AI Foundry (Inference SDK)
    private String projectEndpoint;        // e.g. https://my-project.services.azureml.ms/
    private String apiKey;
    private String modelDeployment = "gpt-4o";
    private String apiVersion = "2024-06-01";
    private String apiPath = "deployments"; // SDK path segment (default: deployments)

    // Azure Computer Vision (for image forensics)
    private String visionEndpoint;
    private String visionApiKey;
    private String visionApiVersion = "2024-05-01-preview";

    // Azure AI Search (for semantic similarity)
    private String searchEndpoint;
    private String searchApiKey;
    private String searchIndex = "claims";

    private int maxRetries = 3;
    private int timeoutSeconds = 30;
}
