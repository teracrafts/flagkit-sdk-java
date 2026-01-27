package dev.flagkit.security;

/**
 * Configuration for evaluation timing jitter.
 *
 * <p>Adds random delays to flag evaluations to prevent cache timing attacks.
 * When enabled, a random delay between minMs and maxMs milliseconds is added
 * at the start of each flag evaluation.</p>
 */
public class EvaluationJitterConfig {
    /** Default minimum jitter in milliseconds. */
    public static final int DEFAULT_MIN_MS = 5;

    /** Default maximum jitter in milliseconds. */
    public static final int DEFAULT_MAX_MS = 15;

    private final boolean enabled;
    private final int minMs;
    private final int maxMs;

    /**
     * Creates a new EvaluationJitterConfig.
     *
     * @param enabled whether jitter is enabled
     * @param minMs minimum delay in milliseconds
     * @param maxMs maximum delay in milliseconds
     */
    public EvaluationJitterConfig(boolean enabled, int minMs, int maxMs) {
        this.enabled = enabled;
        this.minMs = Math.max(0, minMs);
        this.maxMs = Math.max(this.minMs, maxMs);
    }

    /**
     * Returns the default jitter config (enabled, 5-15ms).
     *
     * @return the default config
     */
    public static EvaluationJitterConfig defaults() {
        return new EvaluationJitterConfig(true, DEFAULT_MIN_MS, DEFAULT_MAX_MS);
    }

    /**
     * Returns a disabled jitter config.
     *
     * @return a config with jitter disabled
     */
    public static EvaluationJitterConfig disabled() {
        return new EvaluationJitterConfig(false, DEFAULT_MIN_MS, DEFAULT_MAX_MS);
    }

    /**
     * Creates a custom jitter config.
     *
     * @param minMs minimum delay in milliseconds
     * @param maxMs maximum delay in milliseconds
     * @return the custom config
     */
    public static EvaluationJitterConfig custom(int minMs, int maxMs) {
        return new EvaluationJitterConfig(true, minMs, maxMs);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMinMs() {
        return minMs;
    }

    public int getMaxMs() {
        return maxMs;
    }

    /**
     * Calculates a random jitter value within the configured range.
     *
     * @return jitter in milliseconds, or 0 if disabled
     */
    public int calculateJitter() {
        if (!enabled) {
            return 0;
        }
        if (maxMs <= minMs) {
            return minMs;
        }
        return minMs + (int) (Math.random() * (maxMs - minMs + 1));
    }
}
