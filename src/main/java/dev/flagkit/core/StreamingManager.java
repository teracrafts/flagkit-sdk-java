package dev.flagkit.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import dev.flagkit.types.FlagState;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages Server-Sent Events (SSE) connection for real-time flag updates.
 *
 * <p>Security: Uses token exchange pattern to avoid exposing API keys in URLs.
 * <ol>
 *   <li>Fetches short-lived token via POST with API key in header</li>
 *   <li>Connects to SSE endpoint with disposable token in URL</li>
 * </ol>
 *
 * <p>Features:
 * <ul>
 *   <li>Secure token-based authentication</li>
 *   <li>Automatic token refresh before expiry</li>
 *   <li>Automatic reconnection with exponential backoff</li>
 *   <li>Graceful degradation to polling after max failures</li>
 *   <li>Heartbeat monitoring for connection health</li>
 * </ul>
 */
public class StreamingManager {
    private static final Logger logger = LoggerFactory.getLogger(StreamingManager.class);
    private static final Gson gson = new Gson();

    /**
     * Connection states for streaming.
     */
    public enum StreamingState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        FAILED
    }

    /**
     * Streaming configuration.
     */
    public static class StreamingConfig {
        private final boolean enabled;
        private final Duration reconnectInterval;
        private final int maxReconnectAttempts;
        private final Duration heartbeatInterval;

        public StreamingConfig() {
            this(true, Duration.ofMillis(3000), 3, Duration.ofMillis(30000));
        }

        public StreamingConfig(boolean enabled, Duration reconnectInterval,
                               int maxReconnectAttempts, Duration heartbeatInterval) {
            this.enabled = enabled;
            this.reconnectInterval = reconnectInterval;
            this.maxReconnectAttempts = maxReconnectAttempts;
            this.heartbeatInterval = heartbeatInterval;
        }

        public boolean isEnabled() { return enabled; }
        public Duration getReconnectInterval() { return reconnectInterval; }
        public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
        public Duration getHeartbeatInterval() { return heartbeatInterval; }
    }

    private static class StreamTokenResponse {
        String token;
        int expiresIn;
    }

    private final String baseUrl;
    private final Supplier<String> getApiKey;
    private final StreamingConfig config;
    private final Consumer<FlagState> onFlagUpdate;
    private final Consumer<String> onFlagDelete;
    private final Consumer<List<FlagState>> onFlagsReset;
    private final Runnable onFallbackToPolling;

    private final OkHttpClient httpClient;
    private final ScheduledExecutorService executor;
    private final AtomicReference<StreamingState> state;
    private final AtomicInteger consecutiveFailures;
    private final AtomicLong lastHeartbeat;

    private volatile Call currentCall;
    private volatile ScheduledFuture<?> tokenRefreshTask;
    private volatile ScheduledFuture<?> heartbeatTask;
    private volatile ScheduledFuture<?> retryTask;

    public StreamingManager(
            String baseUrl,
            Supplier<String> getApiKey,
            StreamingConfig config,
            Consumer<FlagState> onFlagUpdate,
            Consumer<String> onFlagDelete,
            Consumer<List<FlagState>> onFlagsReset,
            Runnable onFallbackToPolling) {
        this.baseUrl = baseUrl;
        this.getApiKey = getApiKey;
        this.config = config != null ? config : new StreamingConfig();
        this.onFlagUpdate = onFlagUpdate;
        this.onFlagDelete = onFlagDelete;
        this.onFlagsReset = onFlagsReset;
        this.onFallbackToPolling = onFallbackToPolling;

        this.httpClient = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for SSE
                .build();
        this.executor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "flagkit-streaming");
            t.setDaemon(true);
            return t;
        });
        this.state = new AtomicReference<>(StreamingState.DISCONNECTED);
        this.consecutiveFailures = new AtomicInteger(0);
        this.lastHeartbeat = new AtomicLong(System.currentTimeMillis());
    }

    /**
     * Gets the current connection state.
     */
    public StreamingState getState() {
        return state.get();
    }

    /**
     * Checks if streaming is connected.
     */
    public boolean isConnected() {
        return state.get() == StreamingState.CONNECTED;
    }

    /**
     * Starts the streaming connection.
     */
    public void connect() {
        if (!state.compareAndSet(StreamingState.DISCONNECTED, StreamingState.CONNECTING) &&
            !state.compareAndSet(StreamingState.FAILED, StreamingState.CONNECTING) &&
            !state.compareAndSet(StreamingState.RECONNECTING, StreamingState.CONNECTING)) {
            return;
        }

        executor.execute(this::initiateConnection);
    }

    /**
     * Stops the streaming connection.
     */
    public void disconnect() {
        cleanup();
        state.set(StreamingState.DISCONNECTED);
        consecutiveFailures.set(0);
        logger.debug("Streaming disconnected");
    }

    /**
     * Retries the streaming connection.
     */
    public void retryConnection() {
        StreamingState current = state.get();
        if (current == StreamingState.CONNECTED || current == StreamingState.CONNECTING) {
            return;
        }
        consecutiveFailures.set(0);
        connect();
    }

    private void initiateConnection() {
        try {
            // Step 1: Fetch short-lived stream token
            StreamTokenResponse tokenResponse = fetchStreamToken();

            // Step 2: Schedule token refresh at 80% of TTL
            scheduleTokenRefresh((long) (tokenResponse.expiresIn * 0.8 * 1000));

            // Step 3: Create SSE connection with token
            createConnection(tokenResponse.token);
        } catch (Exception e) {
            logger.error("Failed to fetch stream token", e);
            handleConnectionFailure();
        }
    }

    private StreamTokenResponse fetchStreamToken() throws IOException {
        String tokenUrl = baseUrl + "/sdk/stream/token";

        RequestBody body = RequestBody.create("{}", MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(tokenUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-API-Key", getApiKey.get())
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch stream token: " + response.code());
            }
            return gson.fromJson(response.body().string(), StreamTokenResponse.class);
        }
    }

    private void scheduleTokenRefresh(long delayMs) {
        if (tokenRefreshTask != null) {
            tokenRefreshTask.cancel(false);
        }

        tokenRefreshTask = executor.schedule(() -> {
            try {
                StreamTokenResponse tokenResponse = fetchStreamToken();
                scheduleTokenRefresh((long) (tokenResponse.expiresIn * 0.8 * 1000));
            } catch (Exception e) {
                logger.warn("Failed to refresh stream token, reconnecting", e);
                disconnect();
                connect();
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    private void createConnection(String token) {
        String streamUrl = baseUrl + "/sdk/stream?token=" + token;

        Request request = new Request.Builder()
                .url(streamUrl)
                .addHeader("Accept", "text/event-stream")
                .addHeader("Cache-Control", "no-cache")
                .build();

        currentCall = httpClient.newCall(request);

        try {
            Response response = currentCall.execute();
            if (!response.isSuccessful()) {
                logger.error("SSE connection failed: {}", response.code());
                handleConnectionFailure();
                return;
            }

            handleOpen();
            readEvents(response);
        } catch (IOException e) {
            if (currentCall != null && currentCall.isCanceled()) {
                return; // Normal cancellation
            }
            logger.error("SSE connection error", e);
            handleConnectionFailure();
        }
    }

    private void handleOpen() {
        state.set(StreamingState.CONNECTED);
        consecutiveFailures.set(0);
        lastHeartbeat.set(System.currentTimeMillis());
        startHeartbeatMonitor();
        logger.info("Streaming connected");
    }

    private void readEvents(Response response) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body().byteStream()))) {
            String eventType = null;
            StringBuilder dataBuilder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Empty line = end of event
                if (line.isEmpty()) {
                    if (eventType != null && dataBuilder.length() > 0) {
                        processEvent(eventType, dataBuilder.toString());
                        eventType = null;
                        dataBuilder.setLength(0);
                    }
                    continue;
                }

                // Parse SSE format
                if (line.startsWith("event:")) {
                    eventType = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                    dataBuilder.append(line.substring(5).trim());
                }
            }
        }

        // Connection closed
        if (state.get() == StreamingState.CONNECTED) {
            handleConnectionFailure();
        }
    }

    private void processEvent(String eventType, String data) {
        try {
            switch (eventType) {
                case "flag_updated":
                    FlagState flag = gson.fromJson(data, FlagState.class);
                    onFlagUpdate.accept(flag);
                    break;

                case "flag_deleted":
                    JsonObject deleteObj = gson.fromJson(data, JsonObject.class);
                    onFlagDelete.accept(deleteObj.get("key").getAsString());
                    break;

                case "flags_reset":
                    Type listType = new TypeToken<List<FlagState>>() {}.getType();
                    List<FlagState> flags = gson.fromJson(data, listType);
                    onFlagsReset.accept(flags);
                    break;

                case "heartbeat":
                    lastHeartbeat.set(System.currentTimeMillis());
                    break;
            }
        } catch (Exception e) {
            logger.warn("Failed to process event: {}", eventType, e);
        }
    }

    private void handleConnectionFailure() {
        cleanup();
        int failures = consecutiveFailures.incrementAndGet();

        if (failures >= config.getMaxReconnectAttempts()) {
            state.set(StreamingState.FAILED);
            logger.warn("Streaming failed, falling back to polling. Failures: {}", failures);
            onFallbackToPolling.run();
            scheduleStreamingRetry();
        } else {
            state.set(StreamingState.RECONNECTING);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        long delay = getReconnectDelay();
        logger.debug("Scheduling reconnect in {}ms, attempt {}", delay, consecutiveFailures.get());

        executor.schedule(this::connect, delay, TimeUnit.MILLISECONDS);
    }

    private long getReconnectDelay() {
        long baseDelay = config.getReconnectInterval().toMillis();
        double backoff = Math.pow(2, consecutiveFailures.get() - 1);
        long delay = (long) (baseDelay * backoff);
        // Cap at 30 seconds
        return Math.min(delay, 30000);
    }

    private void scheduleStreamingRetry() {
        if (retryTask != null) {
            retryTask.cancel(false);
        }

        retryTask = executor.schedule(() -> {
            logger.info("Retrying streaming connection");
            retryConnection();
        }, 5, TimeUnit.MINUTES);
    }

    private void startHeartbeatMonitor() {
        stopHeartbeatMonitor();

        long checkInterval = (long) (config.getHeartbeatInterval().toMillis() * 1.5);

        heartbeatTask = executor.schedule(() -> {
            long timeSince = System.currentTimeMillis() - lastHeartbeat.get();
            long threshold = config.getHeartbeatInterval().toMillis() * 2;

            if (timeSince > threshold) {
                logger.warn("Heartbeat timeout, reconnecting. Time since: {}ms", timeSince);
                handleConnectionFailure();
            } else {
                startHeartbeatMonitor();
            }
        }, checkInterval, TimeUnit.MILLISECONDS);
    }

    private void stopHeartbeatMonitor() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
    }

    private void cleanup() {
        if (currentCall != null) {
            currentCall.cancel();
            currentCall = null;
        }
        if (tokenRefreshTask != null) {
            tokenRefreshTask.cancel(false);
            tokenRefreshTask = null;
        }
        stopHeartbeatMonitor();
        if (retryTask != null) {
            retryTask.cancel(false);
            retryTask = null;
        }
    }

    /**
     * Shuts down the streaming manager.
     */
    public void shutdown() {
        disconnect();
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
}
