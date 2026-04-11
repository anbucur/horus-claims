-- Minimal schema for horus-claims
CREATE TABLE IF NOT EXISTS claim (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_number VARCHAR(50) NOT NULL UNIQUE,
    policy_id UUID NOT NULL,
    claim_type VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'REPORTED',
    date_of_loss TIMESTAMP NOT NULL,
    date_reported TIMESTAMP NOT NULL,
    description TEXT,
    reserved_amount DECIMAL(19,2) DEFAULT 0,
    paid_amount DECIMAL(19,2) DEFAULT 0,
    excess DECIMAL(19,2),
    coverage_type VARCHAR(100),
    incident_location VARCHAR(500),
    third_party_involved BOOLEAN DEFAULT FALSE,
    fraud_indicator BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS party (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claim(id),
    party_type VARCHAR(50) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    company_name VARCHAR(200),
    email VARCHAR(200),
    phone VARCHAR(50),
    address VARCHAR(500),
    role_in_claim VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS policy (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_number VARCHAR(100) NOT NULL UNIQUE,
    policy_holder_id UUID,
    insurer_id UUID,
    policy_type VARCHAR(100) NOT NULL,
    effective_date TIMESTAMP NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    premium DECIMAL(19,2),
    coverage_limit DECIMAL(19,2),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS financials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claim(id),
    reserve_amount DECIMAL(19,2) DEFAULT 0,
    paid_amount DECIMAL(19,2) DEFAULT 0,
    outstanding_amount DECIMAL(19,2) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'EUR',
    last_reserve_update TIMESTAMP,
    last_payment_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS evidence (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claim(id),
    evidence_type VARCHAR(100) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    mime_type VARCHAR(100),
    file_size BIGINT,
    uploaded_by VARCHAR(200),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description TEXT,
    metadata JSONB
);

CREATE TABLE IF NOT EXISTS claim_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claim(id),
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by VARCHAR(200),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

CREATE TABLE IF NOT EXISTS claim_review (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claim(id),
    reviewer_id VARCHAR(200),
    review_status VARCHAR(50) NOT NULL,
    review_score INTEGER,
    findings TEXT,
    recommendation TEXT,
    reviewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS settlement_action (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claim(id),
    action_type VARCHAR(100) NOT NULL,
    amount DECIMAL(19,2) DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_by VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    executed_at TIMESTAMP,
    notes TEXT
);

CREATE TABLE IF NOT EXISTS subject_matter_insured (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claim(id),
    property_type VARCHAR(100),
    property_description TEXT,
    property_value DECIMAL(19,2),
    property_address VARCHAR(500),
    ownership_interest DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS claim_party (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claim(id),
    party_type VARCHAR(50) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    company_name VARCHAR(200),
    email VARCHAR(200),
    phone VARCHAR(50),
    address VARCHAR(500),
    role_in_claim VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS target_system (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    system_name VARCHAR(100) NOT NULL,
    system_type VARCHAR(100) NOT NULL,
    base_url VARCHAR(500),
    api_key_hash VARCHAR(500),
    auth_token VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    config JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sync_trigger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trigger_name VARCHAR(200) NOT NULL,
    trigger_type VARCHAR(100) NOT NULL,
    target_system_id UUID NOT NULL REFERENCES target_system(id),
    schedule_cron VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    last_triggered TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sync_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trigger_id UUID NOT NULL REFERENCES sync_trigger(id),
    status VARCHAR(50) NOT NULL,
    records_processed INTEGER DEFAULT 0,
    records_succeeded INTEGER DEFAULT 0,
    records_failed INTEGER DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS claim_sync_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claim(id),
    target_system_id UUID NOT NULL REFERENCES target_system(id),
    last_sync_event_id UUID,
    sync_status VARCHAR(50) NOT NULL,
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS field_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    target_system_id UUID NOT NULL REFERENCES target_system(id),
    source_entity VARCHAR(100) NOT NULL,
    source_field VARCHAR(100) NOT NULL,
    target_field VARCHAR(100) NOT NULL,
    transformation_script TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS settlement (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_id UUID NOT NULL REFERENCES claim(id),
    settlement_type VARCHAR(100) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    settlement_date TIMESTAMP,
    payment_reference VARCHAR(200),
    bank_account VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_claim_number ON claim(claim_number);
CREATE INDEX IF NOT EXISTS idx_claim_status ON claim(status);
CREATE INDEX IF NOT EXISTS idx_claim_policy ON claim(policy_id);
CREATE INDEX IF NOT EXISTS idx_party_claim ON party(claim_id);
CREATE INDEX IF NOT EXISTS idx_evidence_claim ON evidence(claim_id);
CREATE INDEX IF NOT EXISTS idx_status_history_claim ON claim_status_history(claim_id);
CREATE INDEX IF NOT EXISTS idx_review_claim ON claim_review(claim_id);
CREATE INDEX IF NOT EXISTS idx_sync_event_trigger ON sync_event(trigger_id);
CREATE INDEX IF NOT EXISTS idx_sync_state_claim ON claim_sync_state(claim_id);
CREATE INDEX IF NOT EXISTS idx_field_mapping_target ON field_mapping(target_system_id);
