package com.msig.claimsapi.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimSubmittedEvent {
    private Long claimId;
    private String policyNumber;
    private LocalDateTime submittedAt;
    private String traceId;
}
