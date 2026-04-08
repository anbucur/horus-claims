package com.msig.claimsdomain.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ForensicsResult {
    private boolean imagesAuthentic;
    private List<String> manipulatedImageIds;
    private List<String> anomalyFlags;
    private Double overallScore;
    private boolean quarantineRecommended;
}