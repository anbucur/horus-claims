package com.msig.claimsapi.repository;

import com.msig.claimsdomain.entities.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {
    List<Evidence> findByClaimId(Long claimId);
    List<Evidence> findByClaimIdAndIsQuarantined(Long claimId, Boolean isQuarantined);
}
