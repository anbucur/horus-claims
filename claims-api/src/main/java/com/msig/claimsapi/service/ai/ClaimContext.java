package com.msig.claimsapi.service.ai;

import com.msig.claimsdomain.model.ClaimSimilarityResult;
import com.msig.claimsdomain.model.DuplicateMatch;
import com.msig.claimsdomain.model.EntityMatchResult;
import com.msig.claimsdomain.model.ExtractedClaimData;
import com.msig.claimsdomain.model.ForensicsResult;
import com.msig.claimsdomain.model.PolicyVerificationResult;

import java.util.List;

/**
 * Aggregated AI context for a claim — passed to the ReAct agent for reasoning.
 */
public record ClaimContext(
    ExtractedClaimData extractedData,
    PolicyVerificationResult policyResult,
    EntityMatchResult entityResult,
    ForensicsResult forensicsResult,
    List<DuplicateMatch> duplicates,
    List<ClaimSimilarityResult> similarClaims
) {}
