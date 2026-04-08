package com.msig.claimsapi.event;

import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.model.ProcessingMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimApprovedSTPCEvent {
    private Long claimId;
    private ProcessingMode mode;
    private Double confidenceScore;
    private LocalDateTime timestamp;
    private String traceId;
}