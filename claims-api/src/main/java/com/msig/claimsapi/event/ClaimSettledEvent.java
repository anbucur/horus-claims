package com.msig.claimsapi.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimSettledEvent {
    private Long claimId;
    private BigDecimal settlementAmount;
    private String currency;
    private LocalDateTime settledAt;
    private String traceId;
}
