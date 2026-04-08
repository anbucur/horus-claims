package com.msig.claimsapi.temporal;

import com.msig.claimsdomain.entities.Claim;
import com.msig.claimsdomain.model.ProcessingMode;
import com.msig.claimsdomain.model.ProcessingResult;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Temporal workflow implementation for the durable claim processing pipeline.
 *
 * <p>Pipeline steps (each is a separate Temporal Activity with independent retry):
 * <ol>
 *   <li>EXTRACT — AI data extraction from FNOL document</li>
 *   <li>VERIFY_POLICY — policy period and coverage check</li>
 *   <li>MATCH_ENTITIES — entity name resolution</li>
 *   <li>FORENSICS — image manipulation detection</li>
 *   <li>DUPLICATE_CHECK — TF-IDF / Azure AI Search duplicate detection</li>
 *   <li>ROUTE — apply STP / HITL thresholds, persist final status</li>
 * </ol>
 *
 * <p>Retry strategy per activity:
 * <ul>
 *   <li>Initial interval: 1 second</li>
 *   <li>Backoff coefficient: 2.0 (exponential)</li>
 *   <li>Maximum attempts: 3</li>
 *   <li>Non-retryable: {@link IllegalArgumentException} (claim not found)</li>
 * </ul>
 *
 * <p>Task queue: {@code claims-processing} (configured in {@code application.yml}).
 */
public class ClaimProcessingWorkflowImpl implements ClaimProcessingWorkflow {

    private static final Logger log = Workflow.getLogger(ClaimProcessingWorkflowImpl.class);

    private final ActivityOptions defaultOptions = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofMinutes(5))
        .setRetryOptions(RetryOptions.newBuilder()
            .setInitialInterval(Duration.ofSeconds(1))
            .setBackoffCoefficient(2.0)
            .setMaximumAttempts(3)
            .setDoNotRetry(IllegalArgumentException.class.getName())
            .build())
        .build();

    private final ClaimProcessingActivities activities =
        Workflow.newActivityStub(ClaimProcessingActivities.class, defaultOptions);

    @Override
    public ProcessingResult<Claim> processClaim(Long claimId, ProcessingMode mode) {
        String traceId = UUID.randomUUID().toString();
        ProcessingMode effectiveMode = mode != null ? mode : ProcessingMode.AI_ASSISTED;

        log.info("Temporal workflow starting: claimId={} mode={} traceId={}", claimId, effectiveMode, traceId);

        try {
            // Step 1: Extract
            double confidenceScore = activities.extractClaimData(claimId, effectiveMode, traceId);

            // Step 2: Verify policy
            activities.verifyPolicy(claimId, effectiveMode, traceId);

            // Step 3: Match entities
            activities.matchEntities(claimId, effectiveMode, traceId);

            // Step 4: Forensics
            activities.runForensics(claimId, effectiveMode, traceId);

            // Step 5: Duplicate check
            int duplicates = activities.checkDuplicates(claimId, effectiveMode, traceId);
            if (duplicates > 0) {
                log.warn("claimId={} — {} potential duplicate(s) found", claimId, duplicates);
            }

            // Step 6: Route
            String finalStatus = activities.routeClaim(claimId, effectiveMode, confidenceScore, traceId);
            log.info("Temporal workflow complete: claimId={} finalStatus={}", claimId, finalStatus);

            return ProcessingResult.<Claim>builder()
                .mode(effectiveMode)
                .aiAvailable(true)
                .traceId(traceId)
                .build();

        } catch (Exception e) {
            log.error("Temporal workflow failed for claimId={}: {}", claimId, e.getMessage());
            return ProcessingResult.<Claim>builder()
                .mode(effectiveMode)
                .aiAvailable(false)
                .warnings(List.of("Workflow failed: " + e.getMessage()))
                .traceId(traceId)
                .build();
        }
    }
}
