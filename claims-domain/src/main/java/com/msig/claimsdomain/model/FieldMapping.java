package com.msig.claimsdomain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "field_mappings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_system_id")
    private Long targetSystemId;

    @Column(name = "workbench_field", nullable = false)
    private String workbenchField;

    @Column(name = "target_field", nullable = false)
    private String targetField;

    @Column(columnDefinition = "text")
    private String transform;

    @Column(name = "static_value")
    private String staticValue;

    @Column(name = "sort_order")
    private Integer sortOrder;

    private Boolean active;
}
