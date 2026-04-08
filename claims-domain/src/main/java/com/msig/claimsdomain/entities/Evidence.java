package com.msig.claimsdomain.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "evidence", indexes = {
    @Index(name = "idx_evidence_claim_id", columnList = "claim_id")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column
    private String fileName;

    @Column
    private String fileUrl;

    @Column(columnDefinition = "TEXT")
    private String aiClassificationTags;

    @Column
    private Double forensicsScore;

    @Column(nullable = false)
    private Boolean isQuarantined;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum DocumentType {
        SURVEY_REPORT, IMAGE, EMAIL, INVOICE
    }
}
