package com.msig.claimsapi.config;

import com.msig.claimsapi.temporal.ClaimProcessingActivitiesImpl;
import com.msig.claimsapi.temporal.ClaimProcessingWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Temporal worker configuration.
 *
 * <p>Activates only when {@code spring.temporal.enabled=true} (default: false) so that
 * the application starts successfully in environments without a Temporal server
 * (e.g., local dev without Docker, CI test runs).
 *
 * <p>When active, registers:
 * <ul>
 *   <li>{@link ClaimProcessingWorkflowImpl} — durable workflow orchestrator</li>
 *   <li>{@link ClaimProcessingActivitiesImpl} — per-step activities</li>
 * </ul>
 * on task queue {@code claims-processing}.
 *
 * <p>The worker factory is started as a Spring-managed bean and shuts down
 * cleanly via {@link WorkerFactory#shutdown()} when the context closes.
 */
@Configuration
@ConditionalOnProperty(name = "spring.temporal.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class TemporalWorkerConfig {

    public static final String TASK_QUEUE = "claims-processing";

    @Value("${spring.temporal.connection.target:localhost:7233}")
    private String temporalTarget;

    private final ClaimProcessingActivitiesImpl claimProcessingActivitiesImpl;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        log.info("[Temporal] Connecting to Temporal server at {}", temporalTarget);
        return WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget(temporalTarget)
                .build()
        );
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs stubs) {
        return WorkflowClient.newInstance(stubs,
            WorkflowClientOptions.newBuilder()
                .setNamespace("default")
                .build());
    }

    @Bean(destroyMethod = "shutdown")
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(ClaimProcessingWorkflowImpl.class);
        worker.registerActivitiesImplementations(claimProcessingActivitiesImpl);
        factory.start();
        log.info("[Temporal] Worker started on task queue '{}'", TASK_QUEUE);
        return factory;
    }

    /**
     * Build workflow options for starting a new claim processing workflow.
     *
     * @param claimId unique claim ID — used as workflow ID to guarantee idempotency
     * @return configured {@link WorkflowOptions}
     */
    public static WorkflowOptions workflowOptionsForClaim(Long claimId) {
        return WorkflowOptions.newBuilder()
            .setTaskQueue(TASK_QUEUE)
            .setWorkflowId("claim-processing-" + claimId)
            .build();
    }
}
