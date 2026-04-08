package com.msig.claimsapi.controller;

import com.msig.claimsapi.repository.*;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.model.*;
import com.msig.claimsapi.service.SyncEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class SyncController {

    private final TargetSystemRepository targetSystemRepository;
    private final FieldMappingRepository fieldMappingRepository;
    private final SyncTriggerRepository syncTriggerRepository;
    private final SyncEventRepository syncEventRepository;
    private final ClaimSyncStateRepository claimSyncStateRepository;
    private final ClaimRepository claimRepository;
    private final SyncEngine syncEngine;

    // ─── Target Systems ───────────────────────────────────────────────────────

    @GetMapping("/systems")
    public List<TargetSystemDto> listSystems() {
        return targetSystemRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @PostMapping("/systems")
    public TargetSystem createSystem(@RequestBody TargetSystemRequest req) {
        TargetSystem ts = TargetSystem.builder()
                .name(req.name())
                .baseUrl(req.baseUrl())
                .authType(req.authType() != null ? AuthType.valueOf(req.authType()) : AuthType.NONE)
                .apiKey(req.apiKey())
                .oauthClientId(req.oauthClientId())
                .oauthClientSecret(req.oauthClientSecret())
                .basicUsername(req.basicUsername())
                .basicPassword(req.basicPassword())
                .endpoints(req.endpoints())
                .retryPolicy(req.retryPolicy())
                .sandboxMode(req.sandboxMode() != null ? req.sandboxMode() : true)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return targetSystemRepository.save(ts);
    }

    @GetMapping("/systems/{id}")
    public TargetSystemDto getSystem(@PathVariable("id") Long id) {
        TargetSystem ts = targetSystemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Target system not found: " + id));
        return toDto(ts);
    }

    @PutMapping("/systems/{id}")
    public TargetSystemDto updateSystem(@PathVariable("id") Long id, @RequestBody TargetSystemRequest req) {
        TargetSystem ts = targetSystemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Target system not found: " + id));
        if (req.name() != null) ts.setName(req.name());
        if (req.baseUrl() != null) ts.setBaseUrl(req.baseUrl());
        if (req.authType() != null) ts.setAuthType(AuthType.valueOf(req.authType()));
        if (req.apiKey() != null) ts.setApiKey(req.apiKey());
        if (req.oauthClientId() != null) ts.setOauthClientId(req.oauthClientId());
        if (req.oauthClientSecret() != null) ts.setOauthClientSecret(req.oauthClientSecret());
        if (req.basicUsername() != null) ts.setBasicUsername(req.basicUsername());
        if (req.basicPassword() != null) ts.setBasicPassword(req.basicPassword());
        if (req.endpoints() != null) ts.setEndpoints(req.endpoints());
        if (req.retryPolicy() != null) ts.setRetryPolicy(req.retryPolicy());
        if (req.sandboxMode() != null) ts.setSandboxMode(req.sandboxMode());
        ts.setUpdatedAt(Instant.now());
        return toDto(targetSystemRepository.save(ts));
    }

    @DeleteMapping("/systems/{id}")
    public void deleteSystem(@PathVariable("id") Long id) {
        TargetSystem ts = targetSystemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Target system not found: " + id));
        ts.setActive(false);
        targetSystemRepository.save(ts);
    }

    @PostMapping("/systems/{id}/activate")
    public TargetSystemDto activateSystem(@PathVariable("id") Long id) {
        // Deactivate all target systems
        targetSystemRepository.findAll().forEach(ts -> {
            ts.setActive(false);
            ts.setSandboxMode(true);
            targetSystemRepository.save(ts);
        });
        // Activate and enable the specified one
        TargetSystem ts = targetSystemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Target system not found: " + id));
        ts.setActive(true);
        ts.setSandboxMode(false);
        ts.setUpdatedAt(Instant.now());
        return toDto(targetSystemRepository.save(ts));
    }

    // ─── Field Mappings ─────────────────────────────────────────────────────

    @GetMapping("/systems/{systemId}/mappings")
    public List<FieldMapping> getMappings(@PathVariable Long systemId) {
        return fieldMappingRepository.findByTargetSystemIdAndActiveTrueOrderBySortOrderAsc(systemId);
    }

    @PostMapping("/systems/{systemId}/mappings")
    public FieldMapping createMapping(@PathVariable Long systemId, @RequestBody FieldMappingRequest req) {
        FieldMapping m = FieldMapping.builder()
                .targetSystemId(systemId)
                .workbenchField(req.workbenchField())
                .targetField(req.targetField())
                .transform(req.transform())
                .staticValue(req.staticValue())
                .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
                .active(true)
                .build();
        return fieldMappingRepository.save(m);
    }

    @PutMapping("/systems/{systemId}/mappings/{mappingId}")
    public FieldMapping updateMapping(@PathVariable Long mappingId, @RequestBody FieldMappingRequest req) {
        FieldMapping m = fieldMappingRepository.findById(mappingId)
                .orElseThrow(() -> new RuntimeException("Mapping not found: " + mappingId));
        if (req.workbenchField() != null) m.setWorkbenchField(req.workbenchField());
        if (req.targetField() != null) m.setTargetField(req.targetField());
        if (req.transform() != null) m.setTransform(req.transform());
        if (req.staticValue() != null) m.setStaticValue(req.staticValue());
        if (req.sortOrder() != null) m.setSortOrder(req.sortOrder());
        return fieldMappingRepository.save(m);
    }

    @DeleteMapping("/systems/{systemId}/mappings/{mappingId}")
    public void deleteMapping(@PathVariable Long mappingId) {
        FieldMapping m = fieldMappingRepository.findById(mappingId)
                .orElseThrow(() -> new RuntimeException("Mapping not found: " + mappingId));
        m.setActive(false);
        fieldMappingRepository.save(m);
    }

    // ─── Triggers ────────────────────────────────────────────────────────────

    @GetMapping("/systems/{systemId}/triggers")
    public List<SyncTrigger> getTriggers(@PathVariable Long systemId) {
        return syncTriggerRepository.findByTargetSystemIdAndActiveTrue(systemId);
    }

    @PostMapping("/systems/{systemId}/triggers")
    public SyncTrigger createTrigger(@PathVariable Long systemId, @RequestBody TriggerRequest req) {
        SyncTrigger t = SyncTrigger.builder()
                .targetSystemId(systemId)
                .name(req.name())
                .triggerType(TriggerType.valueOf(req.triggerType()))
                .condition(req.condition())
                .filterCondition(req.filterCondition())
                .targetEndpoint(req.targetEndpoint())
                .httpMethod(req.httpMethod() != null ? req.httpMethod() : "POST")
                .payloadTemplate(req.payloadTemplate())
                .enabled(req.enabled() != null ? req.enabled() : true)
                .active(true)
                .build();
        return syncTriggerRepository.save(t);
    }

    @PutMapping("/systems/{systemId}/triggers/{triggerId}")
    public SyncTrigger updateTrigger(@PathVariable Long triggerId, @RequestBody TriggerRequest req) {
        SyncTrigger t = syncTriggerRepository.findById(triggerId)
                .orElseThrow(() -> new RuntimeException("Trigger not found: " + triggerId));
        if (req.name() != null) t.setName(req.name());
        if (req.triggerType() != null) t.setTriggerType(TriggerType.valueOf(req.triggerType()));
        if (req.condition() != null) t.setCondition(req.condition());
        if (req.filterCondition() != null) t.setFilterCondition(req.filterCondition());
        if (req.targetEndpoint() != null) t.setTargetEndpoint(req.targetEndpoint());
        if (req.httpMethod() != null) t.setHttpMethod(req.httpMethod());
        if (req.payloadTemplate() != null) t.setPayloadTemplate(req.payloadTemplate());
        if (req.enabled() != null) t.setEnabled(req.enabled());
        return syncTriggerRepository.save(t);
    }

    @DeleteMapping("/systems/{systemId}/triggers/{triggerId}")
    public void deleteTrigger(@PathVariable Long triggerId) {
        SyncTrigger t = syncTriggerRepository.findById(triggerId)
                .orElseThrow(() -> new RuntimeException("Trigger not found: " + triggerId));
        t.setActive(false);
        syncTriggerRepository.save(t);
    }

    // ─── Sync Queue & History ─────────────────────────────────────────────────

    @GetMapping("/sync/queue")
    public List<SyncEventDto> getQueue() {
        return syncEventRepository.findByStatusOrderByCreatedAtAsc(SyncStatus.PENDING).stream()
                .map(this::toEventDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/sync/history")
    public List<SyncEventDto> getHistory() {
        return syncEventRepository.findTop100ByStatusInOrderByCreatedAtDesc(
                List.of(SyncStatus.SUCCESS, SyncStatus.FAILED)).stream()
                .map(this::toEventDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/sync/stats")
    public SyncStatsDto getStats() {
        long pending = syncEventRepository.countByStatus(SyncStatus.PENDING);
        long failed = syncEventRepository.countByStatus(SyncStatus.FAILED);
        long successToday = syncEventRepository.countByStatusAndCreatedAtAfter(
                SyncStatus.SUCCESS, LocalDate.now().atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant());
        TargetSystem ts = targetSystemRepository.findByActiveTrue().stream().findFirst().orElse(null);
        return new SyncStatsDto(pending, successToday, failed,
                ts != null ? ts.getName() : "None configured",
                ts != null && Boolean.TRUE.equals(ts.getActive()));
    }

    @PostMapping("/sync/events/{eventId}/retry")
    public void retryEvent(@PathVariable Long eventId) {
        SyncEvent event = syncEventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Sync event not found: " + eventId));
        event.setStatus(SyncStatus.RETRYING);
        event.setNextRetryAt(Instant.now());
        syncEventRepository.save(event);
    }

    @PostMapping("/sync/claims/{claimId}/sync")
    public void manualSync(@PathVariable Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found: " + claimId));
        syncEngine.markDirty(claim);
    }

    @PostMapping("/sync/sync-all")
    public Map<String, Object> syncAll() {
        List<ClaimSyncState> dirty = claimSyncStateRepository.findByDirtyTrue();
        int count = 0;
        for (ClaimSyncState cs : dirty) {
            Claim claim = claimRepository.findById(cs.getClaimId()).orElse(null);
            if (claim != null) {
                syncEngine.markDirty(claim);
                count++;
            }
        }
        return Map.of("queued", count, "message", "Sync queued for " + count + " dirty claims");
    }

    @GetMapping("/sync/health")
    public Map<String, Object> healthCheck() {
        TargetSystem ts = targetSystemRepository.findByActiveTrue().stream().findFirst().orElse(null);
        if (ts == null) {
            return Map.of("status", "no_system", "message", "No active target system configured");
        }
        return Map.of(
                "status", "configured",
                "targetSystem", ts.getName(),
                "baseUrl", ts.getBaseUrl(),
                "sandboxMode", Boolean.TRUE.equals(ts.getSandboxMode()),
                "lastChecked", Instant.now().toString()
        );
    }

    // ─── Claim Sync State ─────────────────────────────────────────────────────

    @GetMapping("/claims/{claimId}/sync-state")
    public ClaimSyncStateDto getClaimSyncState(@PathVariable Long claimId) {
        ClaimSyncState cs = claimSyncStateRepository.findByClaimId(claimId).orElse(null);
        if (cs == null) return new ClaimSyncStateDto(claimId, false, false, null, null);
        return new ClaimSyncStateDto(cs.getClaimId(), cs.getDirty(), cs.getSyncLocked(),
                cs.getLastSyncAt(), cs.getDirtySince());
    }

    @PostMapping("/claims/{claimId}/mark-dirty")
    public void markDirty(@PathVariable Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new RuntimeException("Claim not found: " + claimId));
        syncEngine.markDirty(claim);
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    private TargetSystemDto toDto(TargetSystem ts) {
        return new TargetSystemDto(
                ts.getId(), ts.getName(), ts.getBaseUrl(),
                ts.getAuthType() != null ? ts.getAuthType().name() : null,
                ts.getMaskedApiKey(),
                ts.getEndpoints(), ts.getRetryPolicy(),
                ts.getSandboxMode(), ts.getActive());
    }

    private SyncEventDto toEventDto(SyncEvent e) {
        return new SyncEventDto(
                e.getId(), e.getClaimId(), e.getTriggerType() != null ? e.getTriggerType().name() : null,
                e.getTargetEndpoint(), e.getHttpMethod(),
                e.getStatus() != null ? e.getStatus().name() : null,
                e.getResponseStatus(), e.getAttemptCount(),
                e.getLastAttemptAt(), e.getCreatedAt());
    }

    // Record DTOs
    public record TargetSystemRequest(
            String name, String baseUrl, String authType, String apiKey,
            String oauthClientId, String oauthClientSecret,
            String basicUsername, String basicPassword,
            String endpoints, String retryPolicy, Boolean sandboxMode
    ) {}

    public record TargetSystemDto(
            Long id, String name, String baseUrl, String authType, String maskedApiKey,
            String endpoints, String retryPolicy, Boolean sandboxMode, Boolean active
    ) {}

    public record FieldMappingRequest(
            String workbenchField, String targetField, String transform,
            String staticValue, Integer sortOrder
    ) {}

    public record TriggerRequest(
            String name, String triggerType, String condition, String filterCondition,
            String targetEndpoint, String httpMethod, String payloadTemplate, Boolean enabled
    ) {}

    public record SyncEventDto(
            Long id, Long claimId, String triggerType, String targetEndpoint, String httpMethod,
            String status, Integer responseStatus, Integer attemptCount,
            Instant lastAttemptAt, Instant createdAt
    ) {}

    public record SyncStatsDto(
            long pending, long successToday, long failed,
            String targetSystem, boolean targetSystemActive
    ) {}

    public record ClaimSyncStateDto(
            Long claimId, boolean dirty, boolean syncLocked, Instant lastSyncAt, Instant dirtySince
    ) {}
}
