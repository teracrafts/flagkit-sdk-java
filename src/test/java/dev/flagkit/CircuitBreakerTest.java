package dev.flagkit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

    @Test
    void testInitialState() {
        CircuitBreaker cb = new CircuitBreaker(5, 2, Duration.ofSeconds(30), 1);

        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertTrue(cb.allow());
    }

    @Test
    void testOpensAfterThreshold() {
        CircuitBreaker cb = new CircuitBreaker(3, 2, Duration.ofSeconds(30), 1);

        cb.recordFailure();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());

        cb.recordFailure();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());

        cb.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        assertFalse(cb.allow());
    }

    @Test
    void testResetsOnSuccess() {
        CircuitBreaker cb = new CircuitBreaker(3, 2, Duration.ofSeconds(30), 1);

        cb.recordFailure();
        cb.recordFailure();

        cb.recordSuccess();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }

    @Test
    void testHalfOpenState() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(2, 1, Duration.ofMillis(50), 1);

        // Open the circuit
        cb.recordFailure();
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        // Wait for reset timeout
        Thread.sleep(100);

        // Should transition to half-open
        assertTrue(cb.allow());
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());
    }

    @Test
    void testHalfOpenToClosedOnSuccess() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(2, 1, Duration.ofMillis(50), 1);

        // Open the circuit
        cb.recordFailure();
        cb.recordFailure();

        // Wait for half-open
        Thread.sleep(100);
        cb.allow();

        // Success in half-open should close
        cb.recordSuccess();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }

    @Test
    void testHalfOpenToOpenOnFailure() throws InterruptedException {
        CircuitBreaker cb = new CircuitBreaker(2, 1, Duration.ofMillis(50), 1);

        // Open the circuit
        cb.recordFailure();
        cb.recordFailure();

        // Wait for half-open
        Thread.sleep(100);
        cb.allow();

        // Failure in half-open should open again
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }

    @Test
    void testReset() {
        CircuitBreaker cb = new CircuitBreaker(2, 2, Duration.ofSeconds(30), 1);

        // Open the circuit
        cb.recordFailure();
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        // Reset should close
        cb.reset();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertTrue(cb.allow());
    }

    @Test
    void testStats() {
        CircuitBreaker cb = new CircuitBreaker(5, 2, Duration.ofSeconds(30), 1);

        cb.recordFailure();
        cb.recordFailure();

        Map<String, Object> stats = cb.getStats();
        assertEquals("CLOSED", stats.get("state"));
        assertEquals(2, stats.get("failures"));
        assertEquals(5, stats.get("failure_threshold"));
    }
}
