package com.msig.claimsapi.repository;

import com.msig.claimsdomain.entities.Financials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FinancialsRepository extends JpaRepository<Financials, Long> {
    List<Financials> findByClaimId(Long claimId);
    List<Financials> findByClaimIdAndStatus(Long claimId, Financials.TransactionStatus status);
}
