-- Seed: MSIG Core API target system with field mappings and triggers

INSERT INTO target_systems (name, base_url, auth_type, api_key, endpoints, retry_policy, sandbox_mode, active, created_at, updated_at)
VALUES (
    'MSIG Core API',
    'https://api.msigcore.com/v1',
    'API_KEY',
    'msig_demo_key_placeholder',
    '{"createClaim": "/claims", "updateClaim": "/claims/{id}", "updateStatus": "/claims/{id}/status", "getClaim": "/claims/{id}", "healthCheck": "/health"}',
    '{"maxAttempts": 3, "backoffMs": 1000, "timeoutMs": 30000}',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 8 Field Mappings for MSIG Core API
INSERT INTO field_mappings (target_system_id, workbench_field, target_field, transform, static_value, sort_order, active)
VALUES
    (1, 'claim.id', 'claim_reference', NULL, NULL, 1, TRUE),
    (1, 'claim.dateOfLoss', 'loss_date', 'date:YYYY-MM-DD', NULL, 2, TRUE),
    (1, 'claim.incidentNarrative', 'description', NULL, NULL, 3, TRUE),
    (1, 'claim.estimatedValue', 'reserve_amount', 'multiply:0.85', NULL, 4, TRUE),
    (1, 'party.assuredName', 'policy_holder', NULL, NULL, 5, TRUE),
    (1, 'vessel.name', 'vessel_name', NULL, NULL, 6, TRUE),
    (1, 'vessel.imoNumber', 'vessel_id', NULL, NULL, 7, TRUE),
    (1, 'status.STP', 'outcome_code', NULL, 'SETTLED', 8, TRUE);

-- 3 Sync Triggers
INSERT INTO sync_triggers (target_system_id, name, trigger_type, condition, filter_condition, target_endpoint, http_method, enabled, active)
VALUES
    (1, 'Auto-sync STP claims', 'ON_STATUS_CHANGE', '{"field": "workflowStatus", "equals": "STP"}', NULL, '/claims/{id}/status', 'PUT', TRUE, TRUE),
    (1, 'Reserve amount sync', 'ON_FIELD_CHANGE', '{"field": "reserveAmount", "set": true}', NULL, '/claims/{id}', 'PUT', TRUE, TRUE),
    (1, 'Fraud alert', 'ON_FIELD_CHANGE', '{"field": "fraudFlag", "equals": "true"}', NULL, '/claims/{id}/alert', 'POST', FALSE, TRUE);
