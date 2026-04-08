package com.msig.claimsapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FnolIntakeRequest {
    private String policyNumber;
    private LocalDate dateOfLoss;
    private String incidentNarrative;
    private String lossLocation;
    private List<String> evidenceUrls;
    private String submittedBy;
}
