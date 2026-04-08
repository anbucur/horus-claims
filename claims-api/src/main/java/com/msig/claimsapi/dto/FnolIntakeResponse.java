package com.msig.claimsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FnolIntakeResponse {
    private Long claimId;
    private String status;
    private String policyNumber;
    private String message;
    private String traceId;
}
