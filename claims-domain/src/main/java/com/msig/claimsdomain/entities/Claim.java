package com.msig.claimsdomain.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "claim", indexes = {
    @Index(name = "idx_claim_policy_id", columnList = "policy_id"),
    @Index(name = "idx_claim_workflow_status", columnList = "workflowStatus")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(nullable = false)
    private LocalDate dateOfLoss;

    @Column(columnDefinition = "TEXT")
    private String incidentNarrative;

    @Column
    private String lossLocation;

    @Column
    private Double aiConfidenceScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus workflowStatus;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SubjectMatterInsured> subjectMattersInsured = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Financials> financials = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Evidence> evidences = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ClaimParty> claimParties = new ArrayList<>();

    public enum WorkflowStatus {
        RECEIVED, EXTRACTING, VERIFYING, HITL, STP, COMPLETED
    }
}
