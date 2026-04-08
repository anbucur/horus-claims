package com.msig.claimsdomain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DuplicateMatch {
    private Long claimId;
    private String claimReference;
    private Double similarityScore;
    private String matchReason;
}