package com.msig.claimsapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveReserveRequest {

    @NotNull(message = "Financials ID is required")
    private Long financialsId;

    @NotNull(message = "Approved amount is required")
    @Positive(message = "Approved amount must be positive")
    private BigDecimal approvedAmount;

    @NotBlank(message = "Performed by is required")
    private String performedBy;
}
