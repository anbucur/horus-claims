package com.msig.claimsapi.repository;

import com.msig.claimsdomain.model.ClaimSyncState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimSyncStateRepository extends JpaRepository<ClaimSyncState, Long> {
    Optional<ClaimSyncState> findByClaimId(Long claimId);
    List<ClaimSyncState> findByDirtyTrue();
    long countByDirtyTrue();
}
