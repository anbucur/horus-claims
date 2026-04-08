package com.msig.claimsapi.repository;

import com.msig.claimsdomain.entities.ClaimParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimPartyRepository extends JpaRepository<ClaimParty, ClaimParty.ClaimPartyId> {
    List<ClaimParty> findByClaimId(Long claimId);
    List<ClaimParty> findByPartyId(Long partyId);
}
