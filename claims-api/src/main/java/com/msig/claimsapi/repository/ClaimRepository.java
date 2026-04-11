package com.msig.claimsapi.repository;

import com.msig.claimsdomain.entities.Claim;
import org.springframework.data.jpa.repository.EntityGraph;
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
    
    @Query(value = """
        SELECT c.id, c.workflow_status, c.date_of_loss, c.settlement_amount, c.settlement_currency,
               c.created_at, c.updated_at, c.incident_narrative, c.loss_location,
               p.line_of_business, p.policy_number,
               v.name as vessel_name,
               pa.name as insured_name
        FROM claim c
        LEFT JOIN policy p ON c.policy_id = p.id
        LEFT JOIN subject_matter_insured v ON c.id = v.claim_id AND v.type = 'VESSEL'
        LEFT JOIN claim_party cp ON c.id = cp.claim_id AND cp.party_role = 'ASSURED'
        LEFT JOIN party pa ON cp.party_id = pa.id
        """, nativeQuery = true)
    List<Object[]> findAllClaimSummaries();

    @Query("SELECT DISTINCT c FROM Claim c LEFT JOIN FETCH c.claimParties cp LEFT JOIN FETCH cp.party LEFT JOIN FETCH c.policy")
    List<Claim> findAllWithEagerFetch();
    
    @Query("SELECT c FROM Claim c WHERE c.workflowStatus = :status")
    List<Claim> findByWorkflowStatus(@Param("status") Claim.WorkflowStatus status);
    
    @Query("SELECT c FROM Claim c JOIN FETCH c.policy WHERE c.id = :id")
    Optional<Claim> findByIdWithPolicy(@Param("id") Long id);

    @Query(value = "SELECT c.id, c.claim_reference, c.incident_narrative, " +
        "ts_rank(to_tsvector('english', coalesce(c.incident_narrative,'') || ' ' || coalesce(c.loss_location,'')), " +
        "plainto_tsquery('english', :query)) as rank " +
        "FROM claims c " +
        "WHERE c.id != :excludeClaimId " +
        "ORDER BY rank DESC LIMIT :limit", nativeQuery = true)
    List<Object[]> findSimilarByText(@Param("query") String query,
                                     @Param("excludeClaimId") Long excludeClaimId,
                                     @Param("limit") int limit);
}
