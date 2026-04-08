package com.msig.claimsdomain.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ExtractedClaimData {
    private Long claimId;
    private LocalDate dateOfLoss;
    private String lossLocation;
    private String incidentNarrative;
    private BigDecimal estimatedLoss;
    private String currency;
    private List<String> extractedParties;
    private List<String> extractedVessels;
    private Double confidenceScore;
}