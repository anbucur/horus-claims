package com.msig.claimsdomain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "sync_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id")
    private Long claimId;

    @Column(name = "target_system_id")
    private Long targetSystemId;

    @Column(name = "trigger_id")
    private Long triggerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type")
    private TriggerType triggerType;

    @Column(name = "target_endpoint")
    private String targetEndpoint;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "request_payload", columnDefinition = "text")
    private String requestPayload;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "attempt_count")
    private Integer attemptCount;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SyncStatus status;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "created_at")
    private Instant createdAt;
}
