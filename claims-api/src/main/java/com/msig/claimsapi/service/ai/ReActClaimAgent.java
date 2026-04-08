package com.msig.claimsapi.service.ai;

import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.model.*;

import java.util.List;

/**
 * ReAct-style (Reason + Act) claim routing agent.
 * Uses GPT-4o to reason about complex claims and recommend STP | HITL | SIU.
 */
public interface ReActClaimAgent {

    /**
     * Recommend a routing decision for the given claim.
     *
     * @param claim   the claim to evaluate
     * @param context aggregated AI context (extracted data, forensics, duplicates, etc.)
     * @return structured recommendation with decision, reasoning, and confidence
     */
    ClaimRecommendation recommend(Claim claim, ClaimContext context);
}
