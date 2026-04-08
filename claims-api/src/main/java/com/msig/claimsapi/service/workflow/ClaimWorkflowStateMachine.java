package com.msig.claimsapi.service.workflow;

import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.model.ProcessingMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
@Slf4j
public class ClaimWorkflowStateMachine {
    
    public enum WorkflowStep {
        RECEIVED,
        EXTRACTING,
        VERIFYING,
        ENTITY_MATCHING,
        FORENSICS,
        DUPLICATE_CHECK,
        HITL_REVIEW,
        STP,
        APPROVED,
        REJECTED,
        COMPLETED
    }
    
    private final Map<WorkflowStep, WorkflowStep> stepTransitions = new EnumMap<>(WorkflowStep.class);
    
    public ClaimWorkflowStateMachine() {
        stepTransitions.put(WorkflowStep.RECEIVED, WorkflowStep.EXTRACTING);
        stepTransitions.put(WorkflowStep.EXTRACTING, WorkflowStep.VERIFYING);
        stepTransitions.put(WorkflowStep.VERIFYING, WorkflowStep.ENTITY_MATCHING);
        stepTransitions.put(WorkflowStep.ENTITY_MATCHING, WorkflowStep.FORENSICS);
        stepTransitions.put(WorkflowStep.FORENSICS, WorkflowStep.DUPLICATE_CHECK);
        stepTransitions.put(WorkflowStep.DUPLICATE_CHECK, WorkflowStep.HITL_REVIEW);
        stepTransitions.put(WorkflowStep.HITL_REVIEW, WorkflowStep.STP);
        stepTransitions.put(WorkflowStep.STP, WorkflowStep.COMPLETED);
    }
    
    public WorkflowStep getCurrentStep(Claim claim) {
        return mapWorkflowStatusToStep(claim.getWorkflowStatus());
    }
    
    public WorkflowStep getNextStep(Claim claim) {
        WorkflowStep current = getCurrentStep(claim);
        WorkflowStep next = stepTransitions.get(current);
        log.debug("Current step: {}, Next step: {}", current, next);
        return next;
    }
    
    public boolean canAutoAdvance(Claim claim, ProcessingMode mode, double confidenceScore) {
        if (mode == ProcessingMode.FULL_MANUAL) {
            return false;
        }
        
        WorkflowStep current = getCurrentStep(claim);
        
        if (current == WorkflowStep.DUPLICATE_CHECK) {
            if (mode == ProcessingMode.SEMI_AUTOMATIC && confidenceScore >= 85) {
                log.info("SEMI_AUTOMATIC mode: auto-advancing from {} with confidence {}", current, confidenceScore);
                return true;
            }
        }
        
        return current == WorkflowStep.RECEIVED || current == WorkflowStep.EXTRACTING;
    }
    
    public boolean isBlocked(Claim claim, ProcessingMode mode) {
        WorkflowStep current = getCurrentStep(claim);
        
        if (mode == ProcessingMode.FULL_MANUAL) {
            return current == WorkflowStep.EXTRACTING || 
                   current == WorkflowStep.VERIFYING ||
                   current == WorkflowStep.ENTITY_MATCHING ||
                   current == WorkflowStep.FORENSICS ||
                   current == WorkflowStep.DUPLICATE_CHECK;
        }
        
        return false;
    }
    
    public boolean requiresHumanReview(Claim claim, ProcessingMode mode, double confidenceScore) {
        if (mode == ProcessingMode.FULL_MANUAL) {
            return true;
        }
        
        if (confidenceScore < 60) {
            log.info("Confidence score {} below HITL threshold, routing to human review", confidenceScore);
            return true;
        }
        
        return false;
    }
    
    public WorkflowStep processStep(Claim claim, WorkflowStep targetStep, ProcessingMode mode) {
        WorkflowStep current = getCurrentStep(claim);
        
        if (mode == ProcessingMode.FULL_MANUAL) {
            if (isAIPrimaryStep(targetStep)) {
                log.info("FULL_MANUAL mode: skipping AI primary step {} for claim {}", targetStep, claim.getId());
                return stepTransitions.getOrDefault(current, WorkflowStep.HITL_REVIEW);
            }
        }
        
        log.info("Processing step {} for claim {} in mode {}", targetStep, claim.getId(), mode);
        return targetStep;
    }
    
    private boolean isAIPrimaryStep(WorkflowStep step) {
        return step == WorkflowStep.EXTRACTING ||
               step == WorkflowStep.VERIFYING ||
               step == WorkflowStep.ENTITY_MATCHING ||
               step == WorkflowStep.FORENSICS ||
               step == WorkflowStep.DUPLICATE_CHECK;
    }
    
    private WorkflowStep mapWorkflowStatusToStep(Claim.WorkflowStatus status) {
        return switch (status) {
            case RECEIVED -> WorkflowStep.RECEIVED;
            case EXTRACTING -> WorkflowStep.EXTRACTING;
            case VERIFYING -> WorkflowStep.VERIFYING;
            case HITL -> WorkflowStep.HITL_REVIEW;
            case STP -> WorkflowStep.STP;
            case APPROVED -> WorkflowStep.APPROVED;
            case REJECTED -> WorkflowStep.REJECTED;
            case COMPLETED -> WorkflowStep.COMPLETED;
        };
    }

    public Claim.WorkflowStatus mapStepToWorkflowStatus(WorkflowStep step) {
        return switch (step) {
            case RECEIVED -> Claim.WorkflowStatus.RECEIVED;
            case EXTRACTING -> Claim.WorkflowStatus.EXTRACTING;
            case VERIFYING -> Claim.WorkflowStatus.VERIFYING;
            case ENTITY_MATCHING, FORENSICS, DUPLICATE_CHECK -> Claim.WorkflowStatus.VERIFYING;
            case HITL_REVIEW -> Claim.WorkflowStatus.HITL;
            case STP -> Claim.WorkflowStatus.STP;
            case APPROVED -> Claim.WorkflowStatus.APPROVED;
            case REJECTED -> Claim.WorkflowStatus.REJECTED;
            case COMPLETED -> Claim.WorkflowStatus.COMPLETED;
        };
    }
}