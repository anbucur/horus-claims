package com.msig.claimsapi.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AICircuitBreakerTest {

    private AICircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        circuitBreaker = new AICircuitBreaker();
    }

    @Test
    void initialState_isClosed() {
        assertThat(circuitBreaker.getState()).isEqualTo(AICircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.isOpen()).isFalse();
    }

    @Test
    void recordFailure_belowThreshold_remainsClosed() {
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();

        assertThat(circuitBreaker.getState()).isEqualTo(AICircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.isOpen()).isFalse();
    }

    @Test
    void recordFailure_atThreshold_opensCircuit() {
        // Default threshold is 3
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();

        assertThat(circuitBreaker.getState()).isEqualTo(AICircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.isOpen()).isTrue();
    }

    @Test
    void isOpen_afterTimeout_transitionsToHalfOpen() throws Exception {
        // Trip the circuit
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();

        assertThat(circuitBreaker.isOpen()).isTrue();

        // Simulate timeout by manipulating the last failure time via reflection
        var lastFailureTimeField = AICircuitBreaker.class.getDeclaredField("lastFailureTime");
        lastFailureTimeField.setAccessible(true);
        var atomicLong = (java.util.concurrent.atomic.AtomicLong) lastFailureTimeField.get(circuitBreaker);
        // Set last failure time to 130 seconds ago (beyond the 120s reset timeout)
        atomicLong.set(System.currentTimeMillis() - 130_000);

        // isOpen() should now transition to HALF_OPEN and return false
        assertThat(circuitBreaker.isOpen()).isFalse();
        assertThat(circuitBreaker.getState()).isEqualTo(AICircuitBreaker.State.HALF_OPEN);
    }

    @Test
    void recordSuccess_inHalfOpen_closesCircuit() throws Exception {
        // Trip the circuit
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();

        // Force to HALF_OPEN state
        var stateField = AICircuitBreaker.class.getDeclaredField("state");
        stateField.setAccessible(true);
        var atomicRef = (java.util.concurrent.atomic.AtomicReference<?>) stateField.get(circuitBreaker);
        atomicRef.set(AICircuitBreaker.State.HALF_OPEN);

        circuitBreaker.recordSuccess();

        assertThat(circuitBreaker.getState()).isEqualTo(AICircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.isOpen()).isFalse();
    }

    @Test
    void getFailureRate_calculatedCorrectly() {
        // 2 successes, then 2 failures → failure rate = 0.5
        circuitBreaker.recordSuccess();
        circuitBreaker.recordSuccess();
        circuitBreaker.recordFailure();
        circuitBreaker.recordFailure();

        assertThat(circuitBreaker.getFailureRate()).isEqualTo(0.5);
    }
}
