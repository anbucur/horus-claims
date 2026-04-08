package com.msig.claimsapi.service.ai;

import java.util.List;

/**
 * Routing recommendation from the ReAct claim agent.
 */
public record ClaimRecommendation(
    String decision,           // STP | HITL | SIU
    String reasoning,          // Human-readable explanation
    double confidence,         // 0.0–1.0
    List<String> considerations,
    String traceId
) {}
