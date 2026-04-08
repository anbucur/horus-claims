package com.msig.claimsdomain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "claim_party", indexes = {
    @Index(name = "idx_claim_party_claim_id", columnList = "claim_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimParty {

    @EmbeddedId
    private ClaimPartyId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("claimId")
    @JoinColumn(name = "claim_id")
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("partyId")
    @JoinColumn(name = "party_id")
    private Party party;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyRole partyRole;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ClaimPartyId implements Serializable {
        private Long claimId;
        private Long partyId;
    }

    public enum PartyRole {
        ASSURED, BROKER, THIRD_PARTY
    }
}
