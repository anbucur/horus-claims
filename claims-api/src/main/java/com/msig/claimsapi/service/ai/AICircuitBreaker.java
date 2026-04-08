package com.msig.claimsapi.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class AICircuitBreaker {
    
    private static final int FAILURE_THRESHOLD = 3;
    private static final long RESET_TIMEOUT_MS = 120000;
    
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong failedCalls = new AtomicLong(0);
    
    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
    
    public boolean isOpen() {
        if (state.get() == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime.get() > RESET_TIMEOUT_MS) {
                log.info("Circuit breaker transitioning from OPEN to HALF_OPEN after timeout");
                state.set(State.HALF_OPEN);
                return false;
            }
            return true;
        }
        return false;
    }
    
    public void recordSuccess() {
        totalCalls.incrementAndGet();
        failureCount.set(0);
        if (state.get() == State.HALF_OPEN) {
            log.info("Circuit breaker closing after successful call in HALF_OPEN state");
            state.set(State.CLOSED);
        }
    }
    
    public void recordFailure() {
        totalCalls.incrementAndGet();
        failedCalls.incrementAndGet();
        int failures = failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        log.warn("AI call failed. Failure count: {}/{} in last window", failures, FAILURE_THRESHOLD);
        
        if (failures >= FAILURE_THRESHOLD) {
            log.error("Circuit breaker TRIPPED to OPEN state. AI calls will be blocked for {} ms", RESET_TIMEOUT_MS);
            state.set(State.OPEN);
        }
    }
    
    @Scheduled(fixedRate = 60000)
    public void autoResetCheck() {
        if (state.get() == State.OPEN) {
            if (System.currentTimeMillis() - lastFailureTime.get() > RESET_TIMEOUT_MS) {
                log.info("Circuit breaker auto-resetting from OPEN to HALF_OPEN");
                state.set(State.HALF_OPEN);
            }
        }
    }
    
    public State getState() {
        return state.get();
    }
    
    public double getFailureRate() {
        long total = totalCalls.get();
        if (total == 0) return 0.0;
        return (double) failedCalls.get() / total;
    }
    
    public int getFailureCount() {
        return failureCount.get();
    }
}