package com.msig.claimsapi.repository;

import com.msig.claimsdomain.model.SyncEvent;
import com.msig.claimsdomain.model.SyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;

@Repository
public interface SyncEventRepository extends JpaRepository<SyncEvent, Long> {
    List<SyncEvent> findByStatusOrderByCreatedAtAsc(SyncStatus status);
    List<SyncEvent> findTop100ByStatusInOrderByCreatedAtDesc(List<SyncStatus> statuses);
    long countByStatus(SyncStatus status);
    long countByStatusAndCreatedAtAfter(SyncStatus status, Instant after);
    List<SyncEvent> findByClaimIdOrderByCreatedAtDesc(Long claimId);
    List<SyncEvent> findByStatusAndNextRetryAtBefore(SyncStatus status, Instant before);
    List<SyncEvent> findByStatusAndNextRetryAtLessThanEqual(SyncStatus status, Instant before);
}
