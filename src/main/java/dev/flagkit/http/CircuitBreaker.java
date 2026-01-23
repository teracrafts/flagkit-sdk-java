package dev.flagkit.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Circuit breaker implementation for resilient API calls.
 */
public class CircuitBreaker {
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold;
    private final int successThreshold;
    private final Duration resetTimeout;
    private final int halfOpenMaxAllowed;

    private State state;
    private int failures;
    private int successes;
    private Instant lastFailureTime;
    private int halfOpenInProgress;
    private final ReentrantLock lock;

    public CircuitBreaker(int failureThreshold, int successThreshold, Duration resetTimeout, int halfOpenMaxAllowed) {
        this.failureThreshold = failureThreshold;
        this.successThreshold = successThreshold;
        this.resetTimeout = resetTimeout;
        this.halfOpenMaxAllowed = halfOpenMaxAllowed;
        this.state = State.CLOSED;
        this.failures = 0;
        this.successes = 0;
        this.halfOpenInProgress = 0;
        this.lock = new ReentrantLock();
    }

    public CircuitBreaker() {
        this(5, 2, Duration.ofSeconds(30), 1);
    }

    /**
     * Checks if a request should be allowed.
     */
    public boolean allow() {
        lock.lock();
        try {
            switch (state) {
                case CLOSED:
                    return true;

                case OPEN:
                    if (lastFailureTime != null &&
                            Instant.now().isAfter(lastFailureTime.plus(resetTimeout))) {
                        transitionTo(State.HALF_OPEN);
                        halfOpenInProgress = 0;
                    } else {
                        return false;
                    }
                    // Fall through to HALF_OPEN

                case HALF_OPEN:
                    if (halfOpenInProgress < halfOpenMaxAllowed) {
                        halfOpenInProgress++;
                        return true;
                    }
                    return false;

                default:
                    return false;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Records a successful request.
     */
    public void recordSuccess() {
        lock.lock();
        try {
            switch (state) {
                case HALF_OPEN:
                    successes++;
                    halfOpenInProgress--;
                    if (successes >= successThreshold) {
                        transitionTo(State.CLOSED);
                    }
                    break;

                case CLOSED:
                    failures = 0;
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Records a failed request.
     */
    public void recordFailure() {
        lock.lock();
        try {
            lastFailureTime = Instant.now();

            switch (state) {
                case CLOSED:
                    failures++;
                    if (failures >= failureThreshold) {
                        transitionTo(State.OPEN);
                    }
                    break;

                case HALF_OPEN:
                    halfOpenInProgress--;
                    transitionTo(State.OPEN);
                    break;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the current state.
     */
    public State getState() {
        lock.lock();
        try {
            return state;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Resets the circuit breaker to closed state.
     */
    public void reset() {
        lock.lock();
        try {
            state = State.CLOSED;
            failures = 0;
            successes = 0;
            halfOpenInProgress = 0;
            logger.debug("Circuit breaker reset");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns circuit breaker statistics.
     */
    public Map<String, Object> getStats() {
        lock.lock();
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("state", state.name());
            stats.put("failures", failures);
            stats.put("successes", successes);
            stats.put("failure_threshold", failureThreshold);
            stats.put("success_threshold", successThreshold);
            stats.put("half_open_in_progress", halfOpenInProgress);
            return stats;
        } finally {
            lock.unlock();
        }
    }

    private void transitionTo(State newState) {
        State oldState = this.state;
        this.state = newState;
        this.failures = 0;
        this.successes = 0;
        logger.debug("Circuit breaker state change: {} -> {}", oldState, newState);
    }
}
