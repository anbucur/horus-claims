package com.msig.claimsapi.service.ai;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Structured AI call logging and Micrometer metrics.
 *
 * Metrics emitted:
 *   ai.calls.total         — total calls per operation
 *   ai.calls.success       — successful calls per operation
 *   ai.calls.failure       — failed calls per operation
 *   ai.calls.latencyMs     — timer per operation (records call latency)
 *
 * Structured log format:
 *   [AI] traceId=<uuid> model=<model> operation=<op> latency=<ms> success=<true|false> error=<msg>
 */
@Service
@Slf4j
public class AIMetricsService {

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, Counter> totalCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> successCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> failureCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> latencyTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DistributionSummary> promptTokenSummaries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DistributionSummary> completionTokenSummaries = new ConcurrentHashMap<>();

    // GPT-4o pricing per 1K tokens (USD)
    private static final double PROMPT_COST_PER_1K = 0.005;
    private static final double COMPLETION_COST_PER_1K = 0.015;

    public AIMetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordCall(String operation, String model, long latencyMs, boolean success, String error) {
        String op = operation != null ? operation : "unknown";

        totalCounters.computeIfAbsent(op, k ->
            Counter.builder("ai.calls.total")
                .tag("operation", op)
                .description("Total AI calls")
                .register(registry)
        ).increment();

        if (success) {
            successCounters.computeIfAbsent(op, k ->
                Counter.builder("ai.calls.success")
                    .tag("operation", op)
                    .description("Successful AI calls")
                    .register(registry)
            ).increment();
        } else {
            failureCounters.computeIfAbsent(op, k ->
                Counter.builder("ai.calls.failure")
                    .tag("operation", op)
                    .description("Failed AI calls")
                    .register(registry)
            ).increment();
        }

        Timer latencyTimer = latencyTimers.computeIfAbsent(op, k ->
            Timer.builder("ai.calls.latency")
                .tag("operation", op)
                .description("AI call latency in milliseconds")
                .register(registry)
        );
        latencyTimer.record(Duration.ofMillis(latencyMs));

        log.info("[AI] operation={} model={} latency={}ms success={} error={}",
            op, model, latencyMs, success, error != null ? error : "");
    }

    /**
     * Timed wrapper — execute a supplier and record metrics automatically.
     */
    public <T> T timed(String operation, String model, Supplier<T> supplier) {
        long start = System.currentTimeMillis();
        try {
            T result = supplier.get();
            recordCall(operation, model, System.currentTimeMillis() - start, true, null);
            return result;
        } catch (Exception e) {
            recordCall(operation, model, System.currentTimeMillis() - start, false, e.getMessage());
            throw e;
        }
    }

    /**
     * Record token usage for a completed AI call and estimate cost.
     *
     * <p>Registers two DistributionSummary metrics (tagged by {@code operation}):
     * <ul>
     *   <li>{@code ai.tokens.prompt} — prompt token count per call</li>
     *   <li>{@code ai.tokens.completion} — completion token count per call</li>
     * </ul>
     * Cost is logged at INFO using GPT-4o pricing ($0.005/1K prompt, $0.015/1K completion).
     *
     * @param operation      logical operation name (e.g. "extractClaimData")
     * @param promptTokens   number of prompt tokens consumed
     * @param completionTokens number of completion tokens generated
     */
    public void recordTokenUsage(String operation, int promptTokens, int completionTokens) {
        String op = operation != null ? operation : "unknown";

        promptTokenSummaries.computeIfAbsent(op, k ->
            DistributionSummary.builder("ai.tokens.prompt")
                .tag("operation", op)
                .description("Prompt token count per AI call")
                .baseUnit("tokens")
                .register(registry)
        ).record(promptTokens);

        completionTokenSummaries.computeIfAbsent(op, k ->
            DistributionSummary.builder("ai.tokens.completion")
                .tag("operation", op)
                .description("Completion token count per AI call")
                .baseUnit("tokens")
                .register(registry)
        ).record(completionTokens);

        double estimatedCostUsd =
            (promptTokens / 1_000.0 * PROMPT_COST_PER_1K) +
            (completionTokens / 1_000.0 * COMPLETION_COST_PER_1K);

        log.info("[AI] operation={} promptTokens={} completionTokens={} estimatedCostUsd={}",
            op, promptTokens, completionTokens, String.format("%.6f", estimatedCostUsd));
    }
}
