package com.msig.claimsapi.repository;

import com.msig.claimsdomain.entities.SubjectMatterInsured;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectMatterInsuredRepository extends JpaRepository<SubjectMatterInsured, Long> {
    List<SubjectMatterInsured> findByClaimId(Long claimId);
}
