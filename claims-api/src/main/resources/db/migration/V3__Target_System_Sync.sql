-- Target System Sync Engine Schema
-- V3: Core sync tables

CREATE TABLE target_systems (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    auth_type VARCHAR(50),
    api_key VARCHAR(1000),
    oauth_client_id VARCHAR(255),
    oauth_client_secret VARCHAR(500),
    basic_username VARCHAR(255),
    basic_password VARCHAR(500),
    endpoints TEXT,
    retry_policy TEXT,
    sandbox_mode BOOLEAN DEFAULT TRUE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE field_mappings (
    id BIGSERIAL PRIMARY KEY,
    target_system_id BIGINT REFERENCES target_systems(id) ON DELETE CASCADE,
    workbench_field VARCHAR(255) NOT NULL,
    target_field VARCHAR(255) NOT NULL,
    transform TEXT,
    static_value VARCHAR(500),
    sort_order INTEGER DEFAULT 0,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE sync_triggers (
    id BIGSERIAL PRIMARY KEY,
    target_system_id BIGINT REFERENCES target_systems(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    condition JSONB NOT NULL,
    filter_condition JSONB,
    target_endpoint VARCHAR(500),
    http_method VARCHAR(10) DEFAULT 'POST',
    payload_template TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE sync_events (
    id BIGSERIAL PRIMARY KEY,
    claim_id BIGINT REFERENCES claims(id) ON DELETE SET NULL,
    target_system_id BIGINT REFERENCES target_systems(id) ON DELETE SET NULL,
    trigger_id BIGINT REFERENCES sync_triggers(id) ON DELETE SET NULL,
    trigger_type VARCHAR(50),
    target_endpoint VARCHAR(500),
    http_method VARCHAR(10),
    request_payload TEXT,
    response_status INTEGER,
    response_body TEXT,
    attempt_count INTEGER DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) DEFAULT 'PENDING',
    trace_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE claim_sync_state (
    id BIGSERIAL PRIMARY KEY,
    claim_id BIGINT UNIQUE REFERENCES claims(id) ON DELETE CASCADE,
    dirty BOOLEAN DEFAULT FALSE,
    sync_locked BOOLEAN DEFAULT FALSE,
    last_sync_event_id BIGINT,
    last_sync_at TIMESTAMP WITH TIME ZONE,
    dirty_since TIMESTAMP WITH TIME ZONE
);

-- Indexes
CREATE INDEX idx_sync_events_status ON sync_events(status);
CREATE INDEX idx_sync_events_claim_id ON sync_events(claim_id);
CREATE INDEX idx_sync_events_created_at ON sync_events(created_at DESC);
CREATE INDEX idx_sync_events_next_retry ON sync_events(status, next_retry_at) WHERE status = 'RETRYING';
CREATE INDEX idx_sync_triggers_target_system ON sync_triggers(target_system_id);
CREATE INDEX idx_field_mappings_target_system ON field_mappings(target_system_id);
CREATE INDEX idx_claim_sync_state_dirty ON claim_sync_state(dirty) WHERE dirty = TRUE;
CREATE INDEX idx_claim_sync_state_claim ON claim_sync_state(claim_id);
