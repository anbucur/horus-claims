package com.msig.claimsdomain.model;

public record ClaimSimilarityResult(
    Long claimId,
    String claimReference,
    String incidentNarrative,
    double similarityScore,
    String matchReason
) {}
