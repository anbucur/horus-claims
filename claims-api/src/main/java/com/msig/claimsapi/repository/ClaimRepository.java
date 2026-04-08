package com.msig.claimsapi.repository;

import com.msig.claimsdomain.entities.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    
    @Query("SELECT c FROM Claim c WHERE c.policy.id = :policyId")
    List<Claim> findByPolicyId(@Param("policyId") Long policyId);
    
    @Query("SELECT c FROM Claim c WHERE c.workflowStatus = :status")
    List<Claim> findByWorkflowStatus(@Param("status") Claim.WorkflowStatus status);
    
    @Query("SELECT c FROM Claim c JOIN FETCH c.policy WHERE c.id = :id")
    Optional<Claim> findByIdWithPolicy(@Param("id") Long id);
}
