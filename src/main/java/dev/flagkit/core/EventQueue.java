package dev.flagkit.core;

import dev.flagkit.http.HttpClient;
import dev.flagkit.types.EvaluationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Event queue with batching for analytics events.
 */
public class EventQueue {
    private static final Logger logger = LoggerFactory.getLogger(EventQueue.class);

    private final HttpClient httpClient;
    private final String sessionId;
    private final String sdkVersion;
    private final int maxSize;
    private final int batchSize;
    private final Duration flushInterval;

    private final List<Event> events;
    private final ReentrantLock lock;
    private final ScheduledExecutorService executor;
    private volatile String environmentId;
    private volatile boolean running;
    private ScheduledFuture<?> flushTask;

    public EventQueue(HttpClient httpClient, String sessionId, String sdkVersion,
                      int maxSize, int batchSize, Duration flushInterval) {
        this.httpClient = httpClient;
        this.sessionId = sessionId;
        this.sdkVersion = sdkVersion;
        this.maxSize = maxSize;
        this.batchSize = batchSize;
        this.flushInterval = flushInterval;
        this.events = new ArrayList<>();
        this.lock = new ReentrantLock();
        this.executor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "flagkit-events");
            t.setDaemon(true);
            return t;
        });
        this.running = false;
    }

    public EventQueue(HttpClient httpClient, String sessionId, String sdkVersion) {
        this(httpClient, sessionId, sdkVersion, 1000, 10, Duration.ofSeconds(30));
    }

    /**
     * Starts the background flush loop.
     */
    public void start() {
        lock.lock();
        try {
            if (running) {
                return;
            }
            running = true;
            flushTask = executor.scheduleAtFixedRate(
                    this::flush,
                    flushInterval.toMillis(),
                    flushInterval.toMillis(),
                    TimeUnit.MILLISECONDS
            );
            logger.debug("Event queue started");
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stops the event queue and flushes remaining events.
     */
    public void stop() {
        lock.lock();
        try {
            if (!running) {
                return;
            }
            running = false;
            if (flushTask != null) {
                flushTask.cancel(false);
            }
        } finally {
            lock.unlock();
        }

        // Final flush
        flush();

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.debug("Event queue stopped");
    }

    /**
     * Sets the environment ID.
     */
    public void setEnvironmentId(String environmentId) {
        this.environmentId = environmentId;
    }

    /**
     * Tracks an event.
     */
    public void track(String eventType, Map<String, Object> data) {
        lock.lock();
        try {
            if (events.size() >= maxSize) {
                logger.warn("Event queue full, dropping event: {}", eventType);
                return;
            }

            Event event = new Event(
                    eventType,
                    Instant.now().toString(),
                    sessionId,
                    environmentId,
                    sdkVersion,
                    data,
                    null
            );

            events.add(event);
            logger.debug("Event tracked: {} (queue size: {})", eventType, events.size());

            // Trigger flush if batch size reached
            if (events.size() >= batchSize) {
                executor.execute(this::flush);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Tracks an event with context.
     */
    public void trackWithContext(String eventType, Map<String, Object> data, EvaluationContext context) {
        lock.lock();
        try {
            if (events.size() >= maxSize) {
                return;
            }

            Map<String, Object> contextMap = context != null ?
                    context.stripPrivateAttributes().toMap() : null;

            Event event = new Event(
                    eventType,
                    Instant.now().toString(),
                    sessionId,
                    environmentId,
                    sdkVersion,
                    data,
                    contextMap
            );

            events.add(event);

            if (events.size() >= batchSize) {
                executor.execute(this::flush);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Flushes all queued events to the server.
     */
    public void flush() {
        List<Event> eventsToSend;

        lock.lock();
        try {
            if (events.isEmpty()) {
                return;
            }

            eventsToSend = new ArrayList<>(events);
            events.clear();
        } finally {
            lock.unlock();
        }

        logger.debug("Flushing {} events", eventsToSend.size());
        sendEvents(eventsToSend);
    }

    /**
     * Returns the number of queued events.
     */
    public int size() {
        lock.lock();
        try {
            return events.size();
        } finally {
            lock.unlock();
        }
    }

    private void sendEvents(List<Event> events) {
        if (httpClient == null) {
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("events", events);

            httpClient.post("/sdk/events/batch", payload);
            logger.debug("Events sent successfully: {}", events.size());
        } catch (Exception e) {
            logger.warn("Failed to send events: {}", e.getMessage());
            // Could implement retry logic here
        }
    }

    /**
     * Internal event representation.
     */
    public static class Event {
        private final String type;
        private final String timestamp;
        private final String sessionId;
        private final String environmentId;
        private final String sdkVersion;
        private final Map<String, Object> data;
        private final Map<String, Object> context;

        public Event(String type, String timestamp, String sessionId, String environmentId,
                     String sdkVersion, Map<String, Object> data, Map<String, Object> context) {
            this.type = type;
            this.timestamp = timestamp;
            this.sessionId = sessionId;
            this.environmentId = environmentId;
            this.sdkVersion = sdkVersion;
            this.data = data;
            this.context = context;
        }

        public String getType() {
            return type;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getEnvironmentId() {
            return environmentId;
        }

        public String getSdkVersion() {
            return sdkVersion;
        }

        public Map<String, Object> getData() {
            return data;
        }

        public Map<String, Object> getContext() {
            return context;
        }
    }
}
