-- V7__ClaimReview.sql
-- HITL Review Actions

CREATE TABLE claim_review (
    id BIGSERIAL PRIMARY KEY,
    claim_id BIGINT NOT NULL REFERENCES claim(id),
    action VARCHAR(20) NOT NULL,
    reviewer_notes TEXT,
    reviewer VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_claim_review_claim_id ON claim_review(claim_id);
