package dev.flagkit;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * FlagKit SDK entry point with singleton client management.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Initialize the SDK
 * FlagKitClient client = FlagKit.initialize("sdk_your_api_key");
 *
 * // Evaluate flags
 * boolean enabled = FlagKit.getBooleanValue("my-feature", false);
 * String variant = FlagKit.getStringValue("button-text", "Click");
 *
 * // Identify user
 * FlagKit.identify("user-123", Map.of("plan", "premium"));
 *
 * // Track events
 * FlagKit.track("button_clicked", Map.of("button", "signup"));
 *
 * // Shutdown
 * FlagKit.shutdown();
 * }</pre>
 */
public final class FlagKit {
    private static FlagKitClient instance;
    private static final ReentrantLock lock = new ReentrantLock();

    private FlagKit() {
        // Prevent instantiation
    }

    /**
     * Initializes the FlagKit SDK with the given API key.
     *
     * @param apiKey the API key for authentication
     * @return the initialized client
     * @throws FlagKitException if initialization fails or SDK is already initialized
     */
    public static FlagKitClient initialize(String apiKey) {
        return initialize(FlagKitOptions.builder(apiKey).build());
    }

    /**
     * Initializes the FlagKit SDK with the given options.
     *
     * @param options the configuration options
     * @return the initialized client
     * @throws FlagKitException if initialization fails or SDK is already initialized
     */
    public static FlagKitClient initialize(FlagKitOptions options) {
        lock.lock();
        try {
            if (instance != null) {
                throw new FlagKitException(ErrorCode.INIT_ALREADY_INITIALIZED,
                        "FlagKit is already initialized");
            }

            options.validate();
            instance = new FlagKitClient(options);
            instance.initialize();
            return instance;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the singleton client instance.
     *
     * @return the client instance, or null if not initialized
     */
    public static FlagKitClient getClient() {
        lock.lock();
        try {
            return instance;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Checks if the SDK has been initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        lock.lock();
        try {
            return instance != null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Shuts down the SDK and releases resources.
     */
    public static void shutdown() {
        lock.lock();
        try {
            if (instance != null) {
                instance.close();
                instance = null;
            }
        } finally {
            lock.unlock();
        }
    }

    // Convenience methods that operate on the singleton instance

    /**
     * Evaluates a boolean flag using the singleton client.
     *
     * @throws IllegalStateException if SDK is not initialized
     */
    public static boolean getBooleanValue(String key, boolean defaultValue) {
        return requireClient().getBooleanValue(key, defaultValue);
    }

    /**
     * Evaluates a string flag using the singleton client.
     *
     * @throws IllegalStateException if SDK is not initialized
     */
    public static String getStringValue(String key, String defaultValue) {
        return requireClient().getStringValue(key, defaultValue);
    }

    /**
     * Evaluates a number flag using the singleton client.
     *
     * @throws IllegalStateException if SDK is not initialized
     */
    public static double getNumberValue(String key, double defaultValue) {
        return requireClient().getNumberValue(key, defaultValue);
    }

    /**
     * Evaluates an integer flag using the singleton client.
     *
     * @throws IllegalStateException if SDK is not initialized
     */
    public static int getIntValue(String key, int defaultValue) {
        return requireClient().getIntValue(key, defaultValue);
    }

    /**
     * Evaluates a JSON flag using the singleton client.
     *
     * @throws IllegalStateException if SDK is not initialized
     */
    public static Map<String, Object> getJsonValue(String key, Map<String, Object> defaultValue) {
        return requireClient().getJsonValue(key, defaultValue);
    }

    /**
     * Evaluates a flag and returns the full result using the singleton client.
     *
     * @throws IllegalStateException if SDK is not initialized
     */
    public static EvaluationResult evaluate(String key) {
        return requireClient().evaluate(key);
    }

    /**
     * Checks if a flag exists using the singleton client.
     *
     * @throws IllegalStateException if SDK is not initialized
     */
    public static boolean hasFlag(String key) {
        return requireClient().hasFlag(key);
    }

    /**
     * Identifies a user using the singleton client.
     *
     * @throws IllegalStateException if SDK is not initialized
     */
    public static void identify(String userId) {
        requireClient().identify(userId);
    }

    /**
     * Identifies a user with attributes using the singleton client.
     *
     * @throws IllegalStateException if SDK is not initialized
     */
    public static void identify(String userId, Map<String, Object> attributes) {
        requireClient().identify(userId, attributes);
    }

    /**
     * Resets to anonymous user using the singleton client.
     *
     * @throws IllegalStateException if SDK is not initialized
     */
    public static void reset() {
        requireClient().reset();
    }

    /**
     * Tracks a custom event using the singleton client.
     *
     * @throws IllegalStateException if SDK is not initialized
     */
    public static void track(String eventType) {
        requireClient().track(eventType);
    }

    /**
     * Tracks a custom event with data using the singleton client.
     *
     * @throws IllegalStateException if SDK is not initialized
     */
    public static void track(String eventType, Map<String, Object> data) {
        requireClient().track(eventType, data);
    }

    /**
     * Flushes pending events using the singleton client.
     *
     * @throws IllegalStateException if SDK is not initialized
     */
    public static void flush() {
        requireClient().flush();
    }

    private static FlagKitClient requireClient() {
        FlagKitClient client = getClient();
        if (client == null) {
            throw new IllegalStateException("FlagKit is not initialized. Call FlagKit.initialize() first.");
        }
        return client;
    }
}
