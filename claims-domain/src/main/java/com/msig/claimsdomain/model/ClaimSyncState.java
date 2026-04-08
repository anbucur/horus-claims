package com.msig.claimsdomain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "claim_sync_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimSyncState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", unique = true)
    private Long claimId;

    private Boolean dirty;

    @Column(name = "sync_locked")
    private Boolean syncLocked;

    @Column(name = "last_sync_event_id")
    private Long lastSyncEventId;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "dirty_since")
    private Instant dirtySince;
}
