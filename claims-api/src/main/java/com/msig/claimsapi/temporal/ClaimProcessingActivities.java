package com.msig.claimsapi.temporal;

import com.msig.claimsdomain.model.ProcessingMode;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity interface covering each individual step of the claim processing pipeline.
 *
 * <p>Each activity method maps to one processing step and is identified only by the
 * claim's database ID (not the full entity) to keep Temporal history payloads small.
 * The activity implementations load the claim from the database and persist state changes
 * using the existing {@link com.msig.claimsapi.service.ClaimProcessingService} methods.
 *
 * <p>Default timeouts and retry policies are configured in {@link ClaimProcessingWorkflowImpl}.
 */
@ActivityInterface
public interface ClaimProcessingActivities {

    /**
     * EXTRACT step — invoke AI to extract structured data from the FNOL document.
     *
     * @param claimId       claim database ID
     * @param mode          effective processing mode
     * @param traceId       workflow-level trace ID for correlation
     * @return confidence score returned by the AI extractor (0.0 if AI skipped/unavailable)
     */
    @ActivityMethod
    double extractClaimData(Long claimId, ProcessingMode mode, String traceId);

    /**
     * VERIFY step — verify policy coverage and period for the claim.
     *
     * @param claimId  claim database ID
     * @param mode     effective processing mode
     * @param traceId  workflow-level trace ID
     * @return true if policy verification passed (or was skipped for FULL_MANUAL)
     */
    @ActivityMethod
    boolean verifyPolicy(Long claimId, ProcessingMode mode, String traceId);

    /**
     * ENTITY_MATCHING step — match extracted entity names to the core system registry.
     *
     * @param claimId  claim database ID
     * @param mode     effective processing mode
     * @param traceId  workflow-level trace ID
     * @return number of entities matched
     */
    @ActivityMethod
    int matchEntities(Long claimId, ProcessingMode mode, String traceId);

    /**
     * FORENSICS step — run image forensics analysis on evidence attachments.
     *
     * @param claimId  claim database ID
     * @param mode     effective processing mode
     * @param traceId  workflow-level trace ID
     * @return true if no manipulation was detected (or if step was skipped)
     */
    @ActivityMethod
    boolean runForensics(Long claimId, ProcessingMode mode, String traceId);

    /**
     * DUPLICATE_CHECK step — detect potential duplicate claims.
     *
     * @param claimId  claim database ID
     * @param mode     effective processing mode
     * @param traceId  workflow-level trace ID
     * @return number of potential duplicates found (0 = no duplicates)
     */
    @ActivityMethod
    int checkDuplicates(Long claimId, ProcessingMode mode, String traceId);

    /**
     * ROUTE step — apply routing thresholds and set final workflow status (STP or HITL).
     *
     * @param claimId          claim database ID
     * @param mode             effective processing mode
     * @param confidenceScore  aggregated AI confidence score
     * @param traceId          workflow-level trace ID
     * @return the resulting {@link com.msig.claimsdomain.entities.Claim.WorkflowStatus} name
     */
    @ActivityMethod
    String routeClaim(Long claimId, ProcessingMode mode, double confidenceScore, String traceId);
}
