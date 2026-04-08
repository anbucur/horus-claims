package com.msig.claimsapi.service;

import com.msig.claimsapi.repository.TargetSystemRepository;
import com.msig.claimsdomain.model.TargetSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncScheduler {

    private final SyncEngine syncEngine;
    private final TargetSystemRepository targetSystemRepository;
    private final RestTemplate restTemplate;

    @Value("${claims.sync.scheduler.queue-interval-ms:900000}")
    private long queueIntervalMs;

    @Value("${claims.sync.scheduler.retry-interval-ms:60000}")
    private long retryIntervalMs;

    @Value("${claims.sync.scheduler.health-interval-ms:300000}")
    private long healthIntervalMs;

    @Scheduled(fixedDelayString = "${claims.sync.scheduler.queue-interval-ms:900000}")
    public void processScheduledTriggers() {
        log.info("Processing scheduled triggers for dirty claims");
        syncEngine.syncAllDirtyNow();
    }

    @Scheduled(fixedDelayString = "${claims.sync.scheduler.retry-interval-ms:60000}")
    public void retryFailedEvents() {
        log.debug("Retrying failed sync events");
        syncEngine.retryFailed();
    }

    @Scheduled(fixedDelayString = "${claims.sync.scheduler.health-interval-ms:300000}")
    public void checkTargetSystemHealth() {
        log.info("Checking target system health");
        List<TargetSystem> activeSystems = targetSystemRepository.findByActiveTrue();
        for (TargetSystem ts : activeSystems) {
            checkHealth(ts);
        }
    }

    private void checkHealth(TargetSystem ts) {
        if (ts.getEndpoints() == null || ts.getEndpoints().isEmpty()) {
            return;
        }
        
        try {
            var endpoints = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                    .readTree(ts.getEndpoints());
            String healthCheckUrl = ts.getBaseUrl() + endpoints.get("healthCheck").asText();
            restTemplate.getForObject(healthCheckUrl, String.class);
            log.info("Target system {} is healthy", ts.getName());
        } catch (Exception e) {
            log.warn("Target system {} health check failed: {}", ts.getName(), e.getMessage());
        }
    }
}