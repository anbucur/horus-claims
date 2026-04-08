-- V5__ClaimSettlement.sql
-- Financial Settlement Workflow Schema

ALTER TABLE financials ADD COLUMN approved_by VARCHAR(255);
ALTER TABLE financials ADD COLUMN approved_at TIMESTAMP;
ALTER TABLE financials ADD COLUMN paid_at TIMESTAMP;

ALTER TABLE claim ADD COLUMN settlement_amount DECIMAL(19,2);
ALTER TABLE claim ADD COLUMN settlement_currency VARCHAR(3);
ALTER TABLE claim ADD COLUMN settled_at TIMESTAMP;
ALTER TABLE claim ADD COLUMN rejection_reason TEXT;

CREATE TABLE settlement_action (
    id BIGSERIAL PRIMARY KEY,
    claim_id BIGINT REFERENCES claim(id),
    action VARCHAR(20) NOT NULL,
    amount DECIMAL(19,2),
    currency VARCHAR(3),
    performed_by VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
