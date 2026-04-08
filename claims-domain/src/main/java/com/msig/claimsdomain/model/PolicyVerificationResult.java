package com.msig.claimsdomain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PolicyVerificationResult {
    private boolean policyActive;
    private boolean coverageValid;
    private boolean deductibleSatisfied;
    private String lineOfBusinessMatch;
    private String mismatchReason;
    private Double confidenceScore;
}