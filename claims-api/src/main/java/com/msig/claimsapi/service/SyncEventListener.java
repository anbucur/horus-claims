package com.msig.claimsapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.msig.claimsapi.event.*;
import com.msig.claimsapi.repository.*;
import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncEventListener {

    private final TargetSystemRepository targetSystemRepository;
    private final SyncTriggerRepository syncTriggerRepository;
    private final SyncEngine syncEngine;

    @EventListener
    public void onClaimStepCompleted(ClaimStepCompletedEvent event) {
        log.info("Claim step completed event: claimId={}, step={}", event.getClaimId(), event.getStepName());
        processForTriggerType(event.getClaimId(), TriggerType.ON_STATUS_CHANGE);
    }

    @EventListener
    public void onClaimRoutedToHITL(ClaimRoutedToHITLEvent event) {
        log.info("Claim routed to HITL event: claimId={}", event.getClaimId());
        processForTriggerType(event.getClaimId(), TriggerType.ON_STATUS_CHANGE);
    }

    @EventListener
    public void onClaimApprovedSTPC(ClaimApprovedSTPCEvent event) {
        log.info("Claim approved STP event: claimId={}", event.getClaimId());
        processForTriggerType(event.getClaimId(), TriggerType.ON_STATUS_CHANGE);
    }

    private void processForTriggerType(Long claimId, TriggerType triggerType) {
        Claim claim = findClaim(claimId);
        if (claim == null) {
            log.warn("Claim not found: {}", claimId);
            return;
        }

        List<TargetSystem> activeSystems = targetSystemRepository.findByActiveTrue();
        
        for (TargetSystem ts : activeSystems) {
            List<SyncTrigger> triggers = syncTriggerRepository
                    .findByTargetSystemIdAndEnabledTrueAndActiveTrue(ts.getId());
            
            for (SyncTrigger trigger : triggers) {
                if (trigger.getTriggerType() != triggerType) {
                    continue;
                }
                
                if (!evaluateTriggerCondition(trigger, claim)) {
                    continue;
                }
                
                syncEngine.scheduleSync(claim, triggerType, trigger);
            }
        }
    }

    private boolean evaluateTriggerCondition(SyncTrigger trigger, Claim claim) {
        if (trigger.getCondition() == null || trigger.getCondition().isEmpty()) {
            return true;
        }
        
        try {
            JsonNode condition = trigger.getCondition() != null 
                ? com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(trigger.getCondition()) 
                : null;
            
            if (condition == null) {
                return true;
            }
            
            String field = condition.has("field") ? condition.get("field").asText() : null;
            String equals = condition.has("equals") ? condition.get("equals").asText() : null;
            
            if (field == null) {
                return true;
            }
            
            Object fieldValue = getFieldValueFromClaim(field, claim);
            
            if (equals != null) {
                return equals.equals(String.valueOf(fieldValue));
            }
            
            return fieldValue != null;
        } catch (Exception e) {
            log.error("Failed to evaluate trigger condition", e);
            return false;
        }
    }

    private Object getFieldValueFromClaim(String field, Claim claim) {
        return switch (field) {
            case "workflowStatus", "status" -> claim.getWorkflowStatus() != null ? claim.getWorkflowStatus().name() : null;
            case "id" -> claim.getId();
            case "dateOfLoss" -> claim.getDateOfLoss() != null ? claim.getDateOfLoss().toString() : null;
            default -> {
                if (claim.getFinancials() != null && !claim.getFinancials().isEmpty()) {
                    var fin = claim.getFinancials().get(0);
                    yield switch (field) {
                        case "reserveAmount" -> fin.getReserveAmount();
                        case "paidAmount" -> fin.getPaidAmount();
                        case "approvedAmount" -> fin.getApprovedAmount();
                        default -> null;
                    };
                }
                yield null;
            }
        };
    }

    private Claim findClaim(Long claimId) {
        return null;
    }
}