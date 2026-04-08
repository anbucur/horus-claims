package com.msig.claimsapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
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

    @NotBlank(message = "Policy number is required")
    private String policyNumber;

    @NotNull(message = "Date of loss is required")
    @PastOrPresent(message = "Date of loss must be in the past or today")
    private LocalDate dateOfLoss;

    private String incidentNarrative;

    private String lossLocation;

    private List<String> evidenceUrls;

    private String submittedBy;
}
