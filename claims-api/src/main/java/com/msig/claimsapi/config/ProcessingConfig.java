package com.msig.claimsapi.config;

import com.msig.claimsdomain.model.ProcessingMode;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "claims.processing")
@Data
public class ProcessingConfig {
    private ProcessingMode defaultMode = ProcessingMode.AI_ASSISTED;
    private int stpConfidenceThreshold = 85;
    private int hitlConfidenceThreshold = 60;
    private boolean aiEnabled = true;
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    
    @Data
    public static class CircuitBreakerConfig {
        private int failureThreshold = 3;
        private int resetMinutes = 2;
    }
}