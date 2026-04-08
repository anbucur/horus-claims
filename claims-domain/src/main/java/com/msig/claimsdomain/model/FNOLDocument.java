package com.msig.claimsdomain.model;

import com.msig.claimsdomain.entities.Policy;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FNOLDocument {
    private Long claimId;
    private String policyNumber;
    private String dateOfLoss;
    private String lossLocation;
    private String incidentDescription;
    private Policy policy;
}