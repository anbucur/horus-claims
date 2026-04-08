package com.msig.claimsdomain.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EntityMatchResult {
    private Long assuredId;
    private String assuredName;
    private Long brokerId;
    private String brokerName;
    private List<Long> vesselIds;
    private List<String> vesselNames;
    private boolean allEntitiesMatched;
    private Double confidenceScore;
}