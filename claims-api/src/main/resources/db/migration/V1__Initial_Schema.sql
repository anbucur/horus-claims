-- V1__Initial_Schema.sql
-- Claims Processing Platform Initial Schema

CREATE TABLE policy (
    id BIGSERIAL PRIMARY KEY,
    policy_number VARCHAR(100) NOT NULL UNIQUE,
    effective_date DATE NOT NULL,
    expiration_date DATE NOT NULL,
    line_of_business VARCHAR(100) NOT NULL,
    coverage_limits DECIMAL(19,2),
    deductibles DECIMAL(19,2),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_policy_number ON policy(policy_number);

CREATE TABLE claim (
    id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT NOT NULL REFERENCES policy(id),
    date_of_loss DATE NOT NULL,
    incident_narrative TEXT,
    loss_location VARCHAR(500),
    ai_confidence_score DOUBLE PRECISION,
    workflow_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_claim_policy_id ON claim(policy_id);
CREATE INDEX idx_claim_workflow_status ON claim(workflow_status);

CREATE TABLE party (
    id BIGSERIAL PRIMARY KEY,
    party_type VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    address VARCHAR(500),
    country VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subject_matter_insured (
    id BIGSERIAL PRIMARY KEY,
    claim_id BIGINT NOT NULL REFERENCES claim(id),
    type VARCHAR(20) NOT NULL,
    name VARCHAR(255),
    imo_number VARCHAR(50),
    vessel_type VARCHAR(100),
    cargo_description TEXT,
    tonnage DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE financials (
    id BIGSERIAL PRIMARY KEY,
    claim_id BIGINT NOT NULL REFERENCES claim(id),
    category VARCHAR(20) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    transaction_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE evidence (
    id BIGSERIAL PRIMARY KEY,
    claim_id BIGINT NOT NULL REFERENCES claim(id),
    document_type VARCHAR(20) NOT NULL,
    file_name VARCHAR(255),
    file_url VARCHAR(1000),
    ai_classification_tags TEXT,
    forensics_score DOUBLE PRECISION,
    is_quarantined BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evidence_claim_id ON evidence(claim_id);

CREATE TABLE claim_party (
    claim_id BIGINT NOT NULL REFERENCES claim(id),
    party_id BIGINT NOT NULL REFERENCES party(id),
    party_role VARCHAR(20) NOT NULL,
    PRIMARY KEY (claim_id, party_id)
);

CREATE INDEX idx_claim_party_claim_id ON claim_party(claim_id);

-- Audit trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply audit triggers
CREATE TRIGGER update_policy_updated_at
    BEFORE UPDATE ON policy
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_claim_updated_at
    BEFORE UPDATE ON claim
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
