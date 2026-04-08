package com.msig.claimsapi.repository;

import com.msig.claimsdomain.model.SyncTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SyncTriggerRepository extends JpaRepository<SyncTrigger, Long> {
    List<SyncTrigger> findByTargetSystemIdAndEnabledTrueAndActiveTrue(Long targetSystemId);
    List<SyncTrigger> findByTargetSystemIdAndActiveTrue(Long targetSystemId);
}
