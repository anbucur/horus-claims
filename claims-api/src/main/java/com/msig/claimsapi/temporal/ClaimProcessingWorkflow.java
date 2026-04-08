package com.msig.claimsapi.temporal;

import com.msig.claimsdomain.model.ProcessingMode;
import com.msig.claimsdomain.model.ProcessingResult;
import com.msig.claimsdomain.entities.Claim;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Temporal workflow interface for durable, multi-step claim processing.
 *
 * <p>The workflow orchestrates the full AI pipeline:
 * EXTRACT → VERIFY_POLICY → MATCH_ENTITIES → FORENSICS → DUPLICATE_CHECK → ROUTE
 *
 * <p>Each step is a separate Temporal Activity, providing:
 * <ul>
 *   <li>Durable execution — survives API restarts mid-pipeline</li>
 *   <li>Configurable retries per step</li>
 *   <li>Full execution history in the Temporal UI</li>
 *   <li>No idempotency guard needed — Temporal deduplicates by workflow ID</li>
 * </ul>
 *
 * <p>Task queue: {@code claims-processing}
 */
@WorkflowInterface
public interface ClaimProcessingWorkflow {

    /**
     * Process a claim through the full AI pipeline.
     *
     * @param claimId the database ID of the claim to process
     * @param mode    processing mode (AI_ASSISTED / SEMI_AUTOMATIC / FULL_MANUAL); if null uses default
     * @return a {@link ProcessingResult} carrying the final claim state and metadata
     */
    @WorkflowMethod
    ProcessingResult<Claim> processClaim(Long claimId, ProcessingMode mode);
}
