package com.msig.claimsdomain.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "financials")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Financials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BigDecimal getReserveAmount() {
        return Category.INDEMNITY.equals(category) && TransactionStatus.RESERVE.equals(status) ? amount : null;
    }

    public BigDecimal getPaidAmount() {
        return TransactionStatus.PAYMENT.equals(status) ? amount : null;
    }

    public BigDecimal getApprovedAmount() {
        return TransactionStatus.PAYMENT.equals(status) ? amount : null;
    }

    public enum Category {
        INDEMNITY, ALAE
    }

    public enum TransactionStatus {
        RESERVE, PAYMENT
    }
}
