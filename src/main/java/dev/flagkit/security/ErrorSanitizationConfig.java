package dev.flagkit.security;

/**
 * Configuration for error message sanitization.
 *
 * <p>Controls automatic redaction of sensitive information from error messages
 * to prevent information leakage through logs or error responses.</p>
 */
public class ErrorSanitizationConfig {
    private final boolean enabled;
    private final boolean preserveOriginal;

    /**
     * Creates a new ErrorSanitizationConfig.
     *
     * @param enabled whether sanitization is enabled
     * @param preserveOriginal whether to preserve the original message in error details (for debugging)
     */
    public ErrorSanitizationConfig(boolean enabled, boolean preserveOriginal) {
        this.enabled = enabled;
        this.preserveOriginal = preserveOriginal;
    }

    /**
     * Returns the default sanitization config (disabled, no preservation).
     *
     * <p>Sanitization is disabled by default for developer convenience.
     * Enable in production for security.</p>
     *
     * @return the default config
     */
    public static ErrorSanitizationConfig defaults() {
        return new ErrorSanitizationConfig(false, false);
    }

    /**
     * Returns a config with sanitization enabled.
     *
     * @return a config with sanitization enabled
     */
    public static ErrorSanitizationConfig enabled() {
        return new ErrorSanitizationConfig(true, false);
    }

    /**
     * Returns a disabled sanitization config.
     *
     * @return a config with sanitization disabled
     */
    public static ErrorSanitizationConfig disabled() {
        return new ErrorSanitizationConfig(false, false);
    }

    /**
     * Creates a config with sanitization enabled and original preservation.
     *
     * <p>Useful for debugging where you need sanitized output but also
     * access to the original message.</p>
     *
     * @return a config with preservation enabled
     */
    public static ErrorSanitizationConfig withPreservation() {
        return new ErrorSanitizationConfig(true, true);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPreserveOriginal() {
        return preserveOriginal;
    }
}
