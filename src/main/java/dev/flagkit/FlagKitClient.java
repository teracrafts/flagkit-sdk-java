package dev.flagkit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.flagkit.core.Cache;
import dev.flagkit.core.EventQueue;
import dev.flagkit.core.PollingManager;
import dev.flagkit.error.FlagKitException;
import dev.flagkit.http.HttpClient;
import dev.flagkit.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * FlagKit SDK client for feature flag evaluation.
 */
public class FlagKitClient {
    private static final Logger logger = LoggerFactory.getLogger(FlagKitClient.class);

    private final FlagKitOptions options;
    private final Cache cache;
    private final HttpClient httpClient;
    private final EventQueue eventQueue;
    private final Gson gson;
    private final String sessionId;
    private final ReentrantLock lock;

    private PollingManager pollingManager;
    private EvaluationContext context;
    private String lastUpdateTime;
    private volatile boolean ready;
    private volatile boolean closed;
    private CountDownLatch readyLatch;

    FlagKitClient(FlagKitOptions options) {
        this.options = options;
        this.cache = new Cache(options.getCacheTtl(), 1000);
        this.httpClient = new HttpClient(
                HttpClient.getBaseUrl(options.isLocal()),
                options.getApiKey(),
                options.getTimeout(),
                options.getRetries()
        );
        this.sessionId = UUID.randomUUID().toString();
        this.eventQueue = new EventQueue(httpClient, sessionId, FlagKitOptions.SDK_VERSION);
        this.gson = new Gson();
        this.lock = new ReentrantLock();
        this.ready = false;
        this.closed = false;
        this.readyLatch = new CountDownLatch(1);

        // Apply bootstrap values
        applyBootstrap();

        logger.info("FlagKit client created (offline: {})", options.isOffline());
    }

    /**
     * Initializes the SDK by fetching flag configurations.
     */
    public void initialize() {
        lock.lock();
        try {
            if (closed) {
                throw FlagKitException.initError("Client is closed");
            }
        } finally {
            lock.unlock();
        }

        if (options.isOffline()) {
            logger.info("Offline mode enabled, skipping initialization");
            setReady();
            return;
        }

        logger.debug("Initializing SDK");

        try {
            HttpClient.HttpResponse response = httpClient.get("/sdk/init");
            JsonObject data = gson.fromJson(response.getBody(), JsonObject.class);

            // Parse and cache flags
            if (data.has("flags")) {
                List<FlagState> flags = parseFlags(data.getAsJsonArray("flags"));
                cache.setMany(flags, options.getCacheTtl());
                logger.info("SDK initialized with {} flags", flags.size());
            }

            // Set environment ID
            if (data.has("environmentId")) {
                eventQueue.setEnvironmentId(data.get("environmentId").getAsString());
            }

            // Store server time
            if (data.has("serverTime")) {
                lastUpdateTime = data.get("serverTime").getAsString();
            }

            // Start polling if enabled
            if (options.isEnablePolling()) {
                int pollingSeconds = data.has("pollingIntervalSeconds") ?
                        data.get("pollingIntervalSeconds").getAsInt() : 30;
                Duration pollingInterval = Duration.ofSeconds(
                        Math.max(pollingSeconds, options.getPollingInterval().getSeconds())
                );
                startPolling(pollingInterval);
            }

            // Start event queue
            eventQueue.start();

            setReady();

        } catch (FlagKitException e) {
            logger.error("SDK initialization failed: {}", e.getMessage());
            if (options.getOnError() != null) {
                options.getOnError().accept(e);
            }
            // Mark as ready anyway (will use cache/bootstrap/defaults)
            setReady();
            throw e;
        }
    }

    /**
     * Checks if the SDK is ready.
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * Waits for the SDK to be ready.
     */
    public void waitForReady() {
        waitForReady(Duration.ofSeconds(30));
    }

    /**
     * Waits for the SDK to be ready with a timeout.
     */
    public void waitForReady(Duration timeout) {
        if (ready) {
            return;
        }
        try {
            readyLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Evaluates a boolean flag.
     */
    public boolean getBooleanValue(String key, boolean defaultValue) {
        return getBooleanValue(key, defaultValue, null);
    }

    /**
     * Evaluates a boolean flag with context.
     */
    public boolean getBooleanValue(String key, boolean defaultValue, EvaluationContext ctx) {
        EvaluationResult result = evaluate(key, defaultValue, FlagType.BOOLEAN, ctx);
        return result.getBooleanValue();
    }

    /**
     * Evaluates a string flag.
     */
    public String getStringValue(String key, String defaultValue) {
        return getStringValue(key, defaultValue, null);
    }

    /**
     * Evaluates a string flag with context.
     */
    public String getStringValue(String key, String defaultValue, EvaluationContext ctx) {
        EvaluationResult result = evaluate(key, defaultValue, FlagType.STRING, ctx);
        String value = result.getStringValue();
        return value != null ? value : defaultValue;
    }

    /**
     * Evaluates a number flag.
     */
    public double getNumberValue(String key, double defaultValue) {
        return getNumberValue(key, defaultValue, null);
    }

    /**
     * Evaluates a number flag with context.
     */
    public double getNumberValue(String key, double defaultValue, EvaluationContext ctx) {
        EvaluationResult result = evaluate(key, defaultValue, FlagType.NUMBER, ctx);
        return result.getNumberValue();
    }

    /**
     * Evaluates an integer flag.
     */
    public int getIntValue(String key, int defaultValue) {
        return getIntValue(key, defaultValue, null);
    }

    /**
     * Evaluates an integer flag with context.
     */
    public int getIntValue(String key, int defaultValue, EvaluationContext ctx) {
        EvaluationResult result = evaluate(key, (double) defaultValue, FlagType.NUMBER, ctx);
        return result.getIntValue();
    }

    /**
     * Evaluates a JSON flag.
     */
    public Map<String, Object> getJsonValue(String key, Map<String, Object> defaultValue) {
        return getJsonValue(key, defaultValue, null);
    }

    /**
     * Evaluates a JSON flag with context.
     */
    public Map<String, Object> getJsonValue(String key, Map<String, Object> defaultValue, EvaluationContext ctx) {
        EvaluationResult result = evaluate(key, defaultValue, FlagType.JSON, ctx);
        Map<String, Object> value = result.getJsonValue();
        return value != null ? value : defaultValue;
    }

    /**
     * Evaluates a flag and returns the full result.
     */
    public EvaluationResult evaluate(String key) {
        return evaluate(key, null);
    }

    /**
     * Evaluates a flag with context and returns the full result.
     */
    public EvaluationResult evaluate(String key, EvaluationContext ctx) {
        return evaluate(key, null, null, ctx);
    }

    /**
     * Checks if a flag exists.
     */
    public boolean hasFlag(String key) {
        return cache.has(key) || options.getBootstrap().containsKey(key);
    }

    /**
     * Returns all flag keys.
     */
    public Set<String> getAllFlagKeys() {
        Set<String> keys = new HashSet<>(cache.getAllKeys());
        keys.addAll(options.getBootstrap().keySet());
        return keys;
    }

    /**
     * Sets the global evaluation context.
     */
    public void setContext(EvaluationContext ctx) {
        lock.lock();
        try {
            this.context = ctx;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the current global context.
     */
    public EvaluationContext getContext() {
        lock.lock();
        try {
            return context;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears the global context.
     */
    public void clearContext() {
        lock.lock();
        try {
            this.context = null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Identifies a user.
     */
    public void identify(String userId) {
        identify(userId, null);
    }

    /**
     * Identifies a user with attributes.
     */
    public void identify(String userId, Map<String, Object> attributes) {
        EvaluationContext ctx = EvaluationContext.create(userId);
        if (attributes != null) {
            ctx.withCustom(attributes);
        }

        lock.lock();
        try {
            if (this.context != null) {
                this.context = this.context.merge(ctx);
            } else {
                this.context = ctx;
            }
        } finally {
            lock.unlock();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        eventQueue.track("context.identified", data);
    }

    /**
     * Resets to anonymous user.
     */
    public void reset() {
        lock.lock();
        try {
            this.context = EvaluationContext.anonymous();
        } finally {
            lock.unlock();
        }

        eventQueue.track("context.reset", null);
    }

    /**
     * Tracks a custom event.
     */
    public void track(String eventType) {
        track(eventType, null);
    }

    /**
     * Tracks a custom event with data.
     */
    public void track(String eventType, Map<String, Object> data) {
        eventQueue.track(eventType, data);
    }

    /**
     * Flushes pending events.
     */
    public void flush() {
        eventQueue.flush();
    }

    /**
     * Forces a refresh of flags from the server.
     */
    public void refresh() {
        if (options.isOffline() || closed) {
            return;
        }
        doRefresh();
    }

    /**
     * Closes the client and cleans up resources.
     */
    public void close() {
        lock.lock();
        try {
            if (closed) {
                return;
            }
            closed = true;
        } finally {
            lock.unlock();
        }

        logger.debug("Closing SDK");

        if (pollingManager != null) {
            pollingManager.shutdown();
        }

        eventQueue.stop();
        httpClient.close();

        logger.info("SDK closed");
    }

    private EvaluationResult evaluate(String key, Object defaultValue, FlagType expectedType, EvaluationContext ctx) {
        // Validate key
        if (key == null || key.isEmpty()) {
            logger.warn("Invalid flag key: {}", key);
            return EvaluationResult.defaultResult(key, defaultValue, EvaluationReason.ERROR);
        }

        // Try cache first
        FlagState cached = cache.get(key);
        if (cached != null) {
            // Type check
            if (expectedType != null && cached.getFlagType() != expectedType) {
                logger.warn("Flag type mismatch: key={}, expected={}, got={}",
                        key, expectedType, cached.getFlagType());
                return EvaluationResult.defaultResult(key, defaultValue, EvaluationReason.TYPE_MISMATCH);
            }

            return EvaluationResult.builder()
                    .flagKey(key)
                    .value(cached.getValue())
                    .enabled(cached.isEnabled())
                    .reason(EvaluationReason.CACHED)
                    .version(cached.getVersion())
                    .build();
        }

        // Try stale cache
        FlagState stale = cache.getStale(key);
        if (stale != null) {
            logger.debug("Using stale cached value: {}", key);
            return EvaluationResult.builder()
                    .flagKey(key)
                    .value(stale.getValue())
                    .enabled(stale.isEnabled())
                    .reason(EvaluationReason.STALE_CACHE)
                    .version(stale.getVersion())
                    .build();
        }

        // Try bootstrap
        if (options.getBootstrap().containsKey(key)) {
            Object value = options.getBootstrap().get(key);
            logger.debug("Using bootstrap value: {}", key);
            return EvaluationResult.defaultResult(key, value, EvaluationReason.BOOTSTRAP);
        }

        // Return default
        logger.debug("Flag not found, using default: {}", key);
        return EvaluationResult.defaultResult(key, defaultValue, EvaluationReason.FLAG_NOT_FOUND);
    }

    private void applyBootstrap() {
        for (Map.Entry<String, Object> entry : options.getBootstrap().entrySet()) {
            FlagState flag = FlagState.builder()
                    .key(entry.getKey())
                    .value(entry.getValue())
                    .enabled(true)
                    .version(0)
                    .flagType(FlagType.infer(entry.getValue()))
                    .build();
            // Bootstrap values don't expire
            cache.set(entry.getKey(), flag, Duration.ofDays(365));
        }
    }

    private void startPolling(Duration interval) {
        if (pollingManager != null) {
            return;
        }

        pollingManager = new PollingManager(this::doRefresh, interval);
        pollingManager.start();
    }

    private void doRefresh() {
        String since = lastUpdateTime != null ? lastUpdateTime :
                Instant.now().minusSeconds(3600).toString();

        try {
            HttpClient.HttpResponse response = httpClient.get("/sdk/updates?since=" + since);
            JsonObject data = gson.fromJson(response.getBody(), JsonObject.class);

            if (data.has("flags")) {
                List<FlagState> flags = parseFlags(data.getAsJsonArray("flags"));
                if (!flags.isEmpty()) {
                    cache.setMany(flags);
                    logger.debug("Flags refreshed: {}", flags.size());

                    if (options.getOnUpdate() != null) {
                        options.getOnUpdate().accept(flags);
                    }
                }
            }

            if (data.has("checkedAt")) {
                lastUpdateTime = data.get("checkedAt").getAsString();
            }

            if (pollingManager != null) {
                pollingManager.onSuccess();
            }

        } catch (Exception e) {
            logger.warn("Failed to refresh flags: {}", e.getMessage());
            if (pollingManager != null) {
                pollingManager.onError();
            }
        }
    }

    private List<FlagState> parseFlags(JsonArray flagsArray) {
        List<FlagState> flags = new ArrayList<>();
        for (JsonElement element : flagsArray) {
            JsonObject obj = element.getAsJsonObject();
            FlagState flag = FlagState.builder()
                    .key(obj.get("key").getAsString())
                    .value(parseValue(obj.get("value")))
                    .enabled(obj.has("enabled") && obj.get("enabled").getAsBoolean())
                    .version(obj.has("version") ? obj.get("version").getAsInt() : 0)
                    .flagType(obj.has("flagType") ?
                            FlagType.fromValue(obj.get("flagType").getAsString()) : FlagType.BOOLEAN)
                    .lastModified(obj.has("lastModified") ? obj.get("lastModified").getAsString() : null)
                    .build();
            flags.add(flag);
        }
        return flags;
    }

    @SuppressWarnings("unchecked")
    private Object parseValue(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean();
            } else if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsDouble();
            } else {
                return element.getAsString();
            }
        }
        if (element.isJsonObject()) {
            return gson.fromJson(element, Map.class);
        }
        if (element.isJsonArray()) {
            return gson.fromJson(element, List.class);
        }
        return element.toString();
    }

    private void setReady() {
        ready = true;
        readyLatch.countDown();
        if (options.getOnReady() != null) {
            options.getOnReady().run();
        }
    }
}
