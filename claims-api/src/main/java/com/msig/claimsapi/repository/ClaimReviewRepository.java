package com.msig.claimsapi.repository;

import com.msig.claimsdomain.entities.ClaimReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimReviewRepository extends JpaRepository<ClaimReview, Long> {
    List<ClaimReview> findByClaimIdOrderByCreatedAtDesc(Long claimId);
}
