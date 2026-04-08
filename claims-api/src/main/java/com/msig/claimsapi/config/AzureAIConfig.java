package com.msig.claimsapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "claims.azure.ai")
@Data
public class AzureAIConfig {
    private boolean enabled = false;
    private String openAiEndpoint;
    private String openAiApiKey;
    private String openAiDeployment = "gpt-4o";
    private String openAiApiVersion = "2024-06-01";
    private String visionEndpoint;
    private String visionApiKey;
    private String visionApiVersion = "2024-05-01-preview";
    private String searchEndpoint;
    private String searchApiKey;
    private String searchIndex = "claims";
    private int maxRetries = 3;
    private int timeoutSeconds = 30;
}
