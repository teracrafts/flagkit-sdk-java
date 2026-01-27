package dev.flagkit.security;

import java.time.Duration;

/**
 * Configuration for bootstrap signature verification.
 *
 * <p>Controls how the SDK verifies signed bootstrap data and what action
 * to take when verification fails.</p>
 */
public class BootstrapVerificationConfig {
    /** Default maximum age for bootstrap data (24 hours). */
    public static final Duration DEFAULT_MAX_AGE = Duration.ofHours(24);

    /** Verification failure action: log a warning and continue. */
    public static final String ON_FAILURE_WARN = "warn";

    /** Verification failure action: throw an exception. */
    public static final String ON_FAILURE_ERROR = "error";

    /** Verification failure action: silently ignore. */
    public static final String ON_FAILURE_IGNORE = "ignore";

    private final boolean enabled;
    private final Duration maxAge;
    private final String onFailure;

    /**
     * Creates a new BootstrapVerificationConfig.
     *
     * @param enabled whether verification is enabled
     * @param maxAge maximum age of bootstrap data before considered stale
     * @param onFailure action on failure: "warn", "error", or "ignore"
     */
    public BootstrapVerificationConfig(boolean enabled, Duration maxAge, String onFailure) {
        this.enabled = enabled;
        this.maxAge = maxAge != null ? maxAge : DEFAULT_MAX_AGE;
        this.onFailure = onFailure != null ? onFailure : ON_FAILURE_WARN;
    }

    /**
     * Returns the default verification config (enabled, 24h max age, warn on failure).
     *
     * @return the default config
     */
    public static BootstrapVerificationConfig defaults() {
        return new BootstrapVerificationConfig(true, DEFAULT_MAX_AGE, ON_FAILURE_WARN);
    }

    /**
     * Returns a disabled verification config.
     *
     * @return a config with verification disabled
     */
    public static BootstrapVerificationConfig disabled() {
        return new BootstrapVerificationConfig(false, DEFAULT_MAX_AGE, ON_FAILURE_IGNORE);
    }

    /**
     * Creates a strict verification config that errors on failure.
     *
     * @return a config that throws on verification failure
     */
    public static BootstrapVerificationConfig strict() {
        return new BootstrapVerificationConfig(true, DEFAULT_MAX_AGE, ON_FAILURE_ERROR);
    }

    /**
     * Creates a custom verification config.
     *
     * @param enabled whether verification is enabled
     * @param maxAge maximum age of bootstrap data
     * @param onFailure action on failure
     * @return the custom config
     */
    public static BootstrapVerificationConfig custom(boolean enabled, Duration maxAge, String onFailure) {
        return new BootstrapVerificationConfig(enabled, maxAge, onFailure);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Duration getMaxAge() {
        return maxAge;
    }

    public String getOnFailure() {
        return onFailure;
    }

    /**
     * Checks if verification should fail with an error.
     *
     * @return true if onFailure is "error"
     */
    public boolean shouldError() {
        return ON_FAILURE_ERROR.equals(onFailure);
    }

    /**
     * Checks if verification should log a warning.
     *
     * @return true if onFailure is "warn"
     */
    public boolean shouldWarn() {
        return ON_FAILURE_WARN.equals(onFailure);
    }
}
