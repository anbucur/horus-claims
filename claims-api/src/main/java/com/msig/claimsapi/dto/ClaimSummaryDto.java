package com.msig.claimsapi.dto;

import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.entities.ClaimParty;
import com.msig.claimsdomain.entities.SubjectMatterInsured;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaimSummaryDto {
    private Long id;
    private String claimNumber;
    private String insuredName;
    private String vesselName;
    private String lineOfBusiness;
    private String workflowStatus;
    private BigDecimal estimatedValue;
    private String currency;
    private LocalDate dateOfLoss;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ClaimSummaryDto fromEntity(Claim claim) {
        String vesselName = claim.getSubjectMattersInsured().stream()
            .filter(sm -> sm.getType() == SubjectMatterInsured.SubjectType.VESSEL)
            .map(SubjectMatterInsured::getName)
            .findFirst()
            .orElse("—");

        String insuredName = claim.getClaimParties().stream()
            .filter(cp -> cp.getPartyRole() == ClaimParty.PartyRole.ASSURED)
            .map(cp -> cp.getParty().getName())
            .findFirst()
            .orElse("—");

        return ClaimSummaryDto.builder()
            .id(claim.getId())
            .claimNumber("CLM-" + String.format("%03d", claim.getId()))
            .insuredName(insuredName)
            .vesselName(vesselName)
            .lineOfBusiness(claim.getPolicy() != null ? claim.getPolicy().getLineOfBusiness() : "—")
            .workflowStatus(claim.getWorkflowStatus().name())
            .estimatedValue(claim.getSettlementAmount())
            .currency(claim.getSettlementCurrency() != null ? claim.getSettlementCurrency() : "EUR")
            .dateOfLoss(claim.getDateOfLoss())
            .createdAt(claim.getCreatedAt())
            .updatedAt(claim.getUpdatedAt())
            .build();
    }
}
