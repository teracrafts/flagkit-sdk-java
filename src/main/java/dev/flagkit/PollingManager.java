package dev.flagkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages background polling for flag updates.
 */
public class PollingManager {
    private static final Logger logger = LoggerFactory.getLogger(PollingManager.class);

    private final Runnable onPoll;
    private final Duration baseInterval;
    private final Duration jitter;
    private final double backoffMultiplier;
    private final Duration maxInterval;

    private final ScheduledExecutorService executor;
    private final AtomicBoolean running;
    private final AtomicInteger consecutiveErrors;
    private final Random random;

    private ScheduledFuture<?> scheduledTask;
    private Duration currentInterval;

    public PollingManager(Runnable onPoll, Duration interval, Duration jitter,
                          double backoffMultiplier, Duration maxInterval) {
        this.onPoll = onPoll;
        this.baseInterval = interval;
        this.jitter = jitter;
        this.backoffMultiplier = backoffMultiplier;
        this.maxInterval = maxInterval;
        this.executor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "flagkit-polling");
            t.setDaemon(true);
            return t;
        });
        this.running = new AtomicBoolean(false);
        this.consecutiveErrors = new AtomicInteger(0);
        this.random = new Random();
        this.currentInterval = interval;
    }

    public PollingManager(Runnable onPoll, Duration interval) {
        this(onPoll, interval, Duration.ofSeconds(1), 2.0, Duration.ofMinutes(5));
    }

    /**
     * Starts the polling loop.
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            logger.debug("Polling started with interval: {}", currentInterval);
            scheduleNextPoll();
        }
    }

    /**
     * Stops the polling loop.
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            if (scheduledTask != null) {
                scheduledTask.cancel(false);
            }
            logger.debug("Polling stopped");
        }
    }

    /**
     * Shuts down the polling manager.
     */
    public void shutdown() {
        stop();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Checks if polling is active.
     */
    public boolean isActive() {
        return running.get();
    }

    /**
     * Gets the current polling interval.
     */
    public Duration getCurrentInterval() {
        return currentInterval;
    }

    /**
     * Called when a poll succeeds.
     */
    public void onSuccess() {
        consecutiveErrors.set(0);
        currentInterval = baseInterval;
    }

    /**
     * Called when a poll fails.
     */
    public void onError() {
        consecutiveErrors.incrementAndGet();
        Duration newInterval = Duration.ofMillis(
                (long) (currentInterval.toMillis() * backoffMultiplier)
        );
        if (newInterval.compareTo(maxInterval) > 0) {
            newInterval = maxInterval;
        }
        currentInterval = newInterval;
        logger.debug("Polling backoff: interval={}, consecutive_errors={}",
                currentInterval, consecutiveErrors.get());
    }

    /**
     * Resets the polling manager.
     */
    public void reset() {
        consecutiveErrors.set(0);
        currentInterval = baseInterval;
    }

    /**
     * Forces an immediate poll.
     */
    public void pollNow() {
        if (running.get()) {
            executor.execute(this::poll);
        }
    }

    private void scheduleNextPoll() {
        if (!running.get()) {
            return;
        }

        Duration delay = getNextDelay();
        scheduledTask = executor.schedule(this::poll, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void poll() {
        if (!running.get()) {
            return;
        }

        try {
            onPoll.run();
        } catch (Exception e) {
            logger.error("Poll error", e);
            onError();
        }

        scheduleNextPoll();
    }

    private Duration getNextDelay() {
        long jitterAmount = (long) (random.nextDouble() * jitter.toMillis());
        return Duration.ofMillis(currentInterval.toMillis() + jitterAmount);
    }
}
