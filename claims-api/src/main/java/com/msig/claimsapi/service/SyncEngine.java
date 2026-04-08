package com.msig.claimsapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.msig.claimsapi.repository.*;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncEngine {

    private final TargetSystemRepository targetSystemRepository;
    private final FieldMappingRepository fieldMappingRepository;
    private final SyncTriggerRepository syncTriggerRepository;
    private final SyncEventRepository syncEventRepository;
    private final ClaimSyncStateRepository claimSyncStateRepository;
    private final ClaimRepository claimRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${claims.sync.defaults.max-attempts:3}")
    private int defaultMaxAttempts;

    @Value("${claims.sync.defaults.backoff-ms:1000}")
    private long defaultBackoffMs;

    @Value("${claims.sync.defaults.timeout-ms:30000}")
    private int defaultTimeoutMs;

    @Async
    public CompletableFuture<SyncEvent> executeSync(SyncEvent syncEvent) {
        log.info("Executing sync event {} for claim {}", syncEvent.getId(), syncEvent.getClaimId());
        
        Claim claim = claimRepository.findById(syncEvent.getClaimId()).orElse(null);
        if (claim == null) {
            syncEvent.setStatus(SyncStatus.FAILED);
            syncEvent.setResponseStatus(404);
            syncEvent.setResponseBody("{\"error\":\"Claim not found\"}");
            syncEventRepository.save(syncEvent);
            return CompletableFuture.completedFuture(syncEvent);
        }

        TargetSystem targetSystem = targetSystemRepository.findById(syncEvent.getTargetSystemId()).orElse(null);
        if (targetSystem == null) {
            syncEvent.setStatus(SyncStatus.FAILED);
            syncEvent.setResponseStatus(404);
            syncEvent.setResponseBody("{\"error\":\"Target system not found\"}");
            syncEventRepository.save(syncEvent);
            return CompletableFuture.completedFuture(syncEvent);
        }

        SyncTrigger trigger = syncEvent.getTriggerId() != null 
            ? syncTriggerRepository.findById(syncEvent.getTriggerId()).orElse(null) 
            : null;

        try {
            String payload = buildPayload(trigger, targetSystem, claim);
            syncEvent.setRequestPayload(payload);
            
            if (Boolean.TRUE.equals(targetSystem.getSandboxMode())) {
                log.info("Sandbox mode: logging payload without HTTP call");
                syncEvent.setStatus(SyncStatus.SUCCESS);
                syncEvent.setResponseStatus(200);
                syncEvent.setResponseBody("{\"status\":\"sandbox_success\",\"message\":\"Sandbox mode - no HTTP call made\"}");
            } else {
                executeHttpCall(syncEvent, targetSystem, payload);
            }
            
            if (syncEvent.getStatus() == SyncStatus.SUCCESS) {
                clearDirty(claim, syncEvent.getId());
            }
        } catch (Exception e) {
            log.error("Sync execution failed", e);
            syncEvent.setStatus(SyncStatus.FAILED);
            syncEvent.setResponseBody("{\"error\":\"" + e.getMessage() + "\"}");
        }
        
        syncEvent.setLastAttemptAt(Instant.now());
        syncEvent.setAttemptCount(syncEvent.getAttemptCount() + 1);
        
        if (syncEvent.getStatus() == SyncStatus.FAILED && syncEvent.getAttemptCount() < defaultMaxAttempts) {
            syncEvent.setStatus(SyncStatus.RETRYING);
            syncEvent.setNextRetryAt(calculateNextRetry(syncEvent.getAttemptCount(), defaultBackoffMs));
        }
        
        syncEventRepository.save(syncEvent);
        return CompletableFuture.completedFuture(syncEvent);
    }

    @Transactional
    public SyncEvent enqueueSync(Claim claim, TriggerType triggerType, SyncTrigger trigger) {
        TargetSystem targetSystem = targetSystemRepository.findById(trigger.getTargetSystemId()).orElse(null);
        if (targetSystem == null || !Boolean.TRUE.equals(targetSystem.getActive())) {
            log.warn("Target system not found or inactive for trigger {}", trigger.getId());
            return null;
        }

        SyncEvent syncEvent = SyncEvent.builder()
                .claimId(claim.getId())
                .targetSystemId(targetSystem.getId())
                .triggerId(trigger.getId())
                .triggerType(triggerType)
                .targetEndpoint(trigger.getTargetEndpoint())
                .httpMethod(trigger.getHttpMethod())
                .status(SyncStatus.PENDING)
                .traceId(UUID.randomUUID().toString())
                .build();

        return syncEventRepository.save(syncEvent);
    }

    @Transactional
    public void markDirty(Claim claim) {
        ClaimSyncState syncState = claimSyncStateRepository.findByClaimId(claim.getId())
                .orElse(ClaimSyncState.builder().claimId(claim.getId()).build());
        
        syncState.setDirty(true);
        syncState.setDirtySince(Instant.now());
        syncState.setSyncLocked(false);
        claimSyncStateRepository.save(syncState);
    }

    @Transactional
    public void clearDirty(Claim claim, Long eventId) {
        claimSyncStateRepository.findByClaimId(claim.getId()).ifPresent(syncState -> {
            syncState.setDirty(false);
            syncState.setLastSyncEventId(eventId);
            syncState.setLastSyncAt(Instant.now());
            syncState.setDirtySince(null);
            claimSyncStateRepository.save(syncState);
        });
    }

    @Async
    public CompletableFuture<Void> retryFailed() {
        List<SyncEvent> eventsToRetry = syncEventRepository.findByStatusAndNextRetryAtLessThanEqual(
                SyncStatus.RETRYING, Instant.now());
        
        log.info("Found {} events to retry", eventsToRetry.size());
        
        for (SyncEvent event : eventsToRetry) {
            executeSync(event);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    public boolean evaluateCondition(String conditionJson, Claim claim) {
        if (conditionJson == null || conditionJson.isEmpty()) {
            return true;
        }
        
        try {
            JsonNode condition = objectMapper.readTree(conditionJson);
            String field = condition.get("field") != null ? condition.get("field").asText() : null;
            String equals = condition.get("equals") != null ? condition.get("equals").asText() : null;
            
            if (field == null) {
                return true;
            }
            
            Object fieldValue = getFieldValue(field, claim);
            
            if (equals != null) {
                return equals.equals(String.valueOf(fieldValue));
            }
            
            return fieldValue != null;
        } catch (Exception e) {
            log.error("Failed to evaluate condition", e);
            return false;
        }
    }

    public String applyTransform(String transform, String value) {
        if (transform == null || transform.isEmpty() || value == null) {
            return value;
        }
        
        try {
            if (transform.startsWith("multiply:")) {
                BigDecimal factor = new BigDecimal(transform.substring(9));
                BigDecimal result = new BigDecimal(value).multiply(factor);
                return result.toString();
            }
            return value;
        } catch (Exception e) {
            log.warn("Transform failed: {} with value {}", transform, value);
            return value;
        }
    }

    public Object getFieldValue(String workbenchField, Claim claim) {
        return switch (workbenchField) {
            case "id" -> claim.getId();
            case "workflowStatus" -> claim.getWorkflowStatus() != null ? claim.getWorkflowStatus().name() : null;
            case "dateOfLoss" -> claim.getDateOfLoss() != null ? claim.getDateOfLoss().toString() : null;
            case "incidentNarrative" -> claim.getIncidentNarrative();
            case "lossLocation" -> claim.getLossLocation();
            case "aiConfidenceScore" -> claim.getAiConfidenceScore();
            default -> {
                if (workbenchField.startsWith("financials.")) {
                    String field = workbenchField.substring(10);
                    if (claim.getFinancials() != null && !claim.getFinancials().isEmpty()) {
                        var fin = claim.getFinancials().get(0);
                        yield switch (field) {
                            case "reserveAmount" -> fin.getReserveAmount();
                            case "paidAmount" -> fin.getPaidAmount();
                            case "approvedAmount" -> fin.getApprovedAmount();
                            default -> null;
                        };
                    }
                }
                yield null;
            }
        };
    }

    public String buildPayload(SyncTrigger trigger, TargetSystem targetSystem, Claim claim) {
        try {
            JsonNode payloadTemplate = trigger.getPayloadTemplate() != null 
                ? objectMapper.readTree(trigger.getPayloadTemplate()) 
                : objectMapper.createObjectNode();
            
            List<FieldMapping> mappings = fieldMappingRepository
                    .findByTargetSystemIdAndActiveTrueOrderBySortOrderAsc(targetSystem.getId());
            
            ObjectNode payload = payloadTemplate.deepCopy();
            
            for (FieldMapping mapping : mappings) {
                Object value;
                if (mapping.getStaticValue() != null && !mapping.getStaticValue().isEmpty()) {
                    value = mapping.getStaticValue();
                } else {
                    Object rawValue = getFieldValue(mapping.getWorkbenchField(), claim);
                    value = rawValue != null ? applyTransform(mapping.getTransform(), String.valueOf(rawValue)) : null;
                }
                ((ObjectNode) payload).put(mapping.getTargetField(), value != null ? String.valueOf(value) : "");
            }
            
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to build payload", e);
            return "{}";
        }
    }

    private void executeHttpCall(SyncEvent syncEvent, TargetSystem targetSystem, String payload) {
        try {
            HttpHeaders headers = buildHeaders(targetSystem);
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            
            String url = targetSystem.getBaseUrl() + syncEvent.getTargetEndpoint();
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.valueOf(syncEvent.getHttpMethod()),
                    entity,
                    String.class
            );
            
            syncEvent.setResponseStatus(response.getStatusCode().value());
            syncEvent.setResponseBody(response.getBody());
            syncEvent.setStatus(response.getStatusCode().is2xxSuccessful() ? SyncStatus.SUCCESS : SyncStatus.FAILED);
            
        } catch (Exception e) {
            log.error("HTTP call failed", e);
            syncEvent.setResponseStatus(500);
            syncEvent.setResponseBody("{\"error\":\"" + e.getMessage() + "\"}");
            syncEvent.setStatus(SyncStatus.FAILED);
        }
    }

    private HttpHeaders buildHeaders(TargetSystem targetSystem) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        switch (targetSystem.getAuthType()) {
            case API_KEY -> headers.set("Authorization", "Bearer " + targetSystem.getApiKey());
            case BASIC -> {
                String credentials = targetSystem.getBasicUsername() + ":" + targetSystem.getBasicPassword();
                String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                headers.set("Authorization", "Basic " + encoded);
            }
            case OAUTH2 -> headers.set("Authorization", "Bearer " + targetSystem.getApiKey());
            case NONE -> {}
        }
        
        return headers;
    }

    private Instant calculateNextRetry(int attemptCount, long backoffMs) {
        long delayNanos = backoffMs * 1_000_000L * (long) Math.pow(2, attemptCount - 1);
        return Instant.now().plusNanos(delayNanos);
    }

    @Transactional
    public void scheduleSync(Claim claim, TriggerType triggerType, SyncTrigger trigger) {
        enqueueSync(claim, triggerType, trigger);
    }

    @Transactional
    public void syncAllDirtyNow() {
        List<ClaimSyncState> dirtyStates = claimSyncStateRepository.findByDirtyTrue();
        for (ClaimSyncState state : dirtyStates) {
            Claim claim = claimRepository.findById(state.getClaimId()).orElse(null);
            if (claim != null) {
                List<TargetSystem> activeSystems = targetSystemRepository.findByActiveTrue();
                for (TargetSystem ts : activeSystems) {
                    List<SyncTrigger> triggers = syncTriggerRepository
                            .findByTargetSystemIdAndEnabledTrueAndActiveTrue(ts.getId());
                    for (SyncTrigger trigger : triggers) {
                        if (trigger.getTriggerType() == TriggerType.ON_SCHEDULE) {
                            enqueueSync(claim, TriggerType.ON_SCHEDULE, trigger);
                        }
                    }
                }
            }
        }
    }
}