package com.msig.claimsdomain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sync_triggers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_system_id")
    private Long targetSystemId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private TriggerType triggerType;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String condition;

    @Column(columnDefinition = "jsonb")
    private String filterCondition;

    @Column(name = "target_endpoint")
    private String targetEndpoint;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "payload_template", columnDefinition = "text")
    private String payloadTemplate;

    private Boolean enabled;

    private Boolean active;
}
