-- V6__ClaimStatusHistory.sql
-- Claim Status History / Audit Trail

CREATE TABLE claim_status_history (
    id BIGSERIAL PRIMARY KEY,
    claim_id BIGINT NOT NULL REFERENCES claim(id),
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    changed_by VARCHAR(255),
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_claim_status_history_claim_id ON claim_status_history(claim_id);
