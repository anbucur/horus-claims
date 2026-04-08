package com.msig.claimsapi.repository;

import com.msig.claimsdomain.entities.SettlementAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementActionRepository extends JpaRepository<SettlementAction, Long> {
    List<SettlementAction> findByClaimIdOrderByCreatedAtDesc(Long claimId);
}
