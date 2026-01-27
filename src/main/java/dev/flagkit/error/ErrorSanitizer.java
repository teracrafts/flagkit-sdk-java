package dev.flagkit.error;

import dev.flagkit.security.ErrorSanitizationConfig;

import java.util.regex.Pattern;

/**
 * Sanitizes error messages to prevent sensitive information leakage.
 *
 * <p>Automatically redacts file paths, IP addresses, API keys, email addresses,
 * and database connection strings from error messages.</p>
 */
public final class ErrorSanitizer {

    /**
     * Sanitization patterns and their replacement tokens.
     */
    private static final SanitizationPattern[] PATTERNS = {
            // Unix-style file paths: /path/to/file → [PATH]
            new SanitizationPattern(
                    Pattern.compile("/(?:[\\w.-]+/)+[\\w.-]+"),
                    "[PATH]"),

            // Windows-style file paths: C:\path\to\file → [PATH]
            new SanitizationPattern(
                    Pattern.compile("[A-Za-z]:\\\\(?:[^\\\\]+\\\\)+[^\\\\]*"),
                    "[PATH]"),

            // IPv4 addresses: 192.168.1.1 → [IP]
            new SanitizationPattern(
                    Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"),
                    "[IP]"),

            // IPv6 addresses (simplified) → [IP]
            new SanitizationPattern(
                    Pattern.compile("\\b(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\\b"),
                    "[IP]"),

            // FlagKit SDK API keys: sdk_xxxxx → sdk_[REDACTED]
            new SanitizationPattern(
                    Pattern.compile("sdk_[a-zA-Z0-9_-]{8,}"),
                    "sdk_[REDACTED]"),

            // FlagKit server API keys: srv_xxxxx → srv_[REDACTED]
            new SanitizationPattern(
                    Pattern.compile("srv_[a-zA-Z0-9_-]{8,}"),
                    "srv_[REDACTED]"),

            // FlagKit CLI API keys: cli_xxxxx → cli_[REDACTED]
            new SanitizationPattern(
                    Pattern.compile("cli_[a-zA-Z0-9_-]{8,}"),
                    "cli_[REDACTED]"),

            // Generic API keys (common patterns)
            new SanitizationPattern(
                    Pattern.compile("(?i)(?:api[_-]?key|apikey)[\"':\\s=]+[a-zA-Z0-9_-]{16,}"),
                    "[API_KEY]"),

            // Bearer tokens
            new SanitizationPattern(
                    Pattern.compile("(?i)bearer\\s+[a-zA-Z0-9_.-]+"),
                    "Bearer [TOKEN]"),

            // Email addresses: user@example.com → [EMAIL]
            new SanitizationPattern(
                    Pattern.compile("[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}"),
                    "[EMAIL]"),

            // Database connection strings: postgres://user:pass@host → [CONNECTION_STRING]
            new SanitizationPattern(
                    Pattern.compile("(?i)(?:postgres|mysql|mongodb|redis|jdbc)://[^\\s]+"),
                    "[CONNECTION_STRING]"),

            // HTTP Basic Auth in URLs: http://user:pass@host → [AUTH_URL]
            new SanitizationPattern(
                    Pattern.compile("https?://[^:]+:[^@]+@[^\\s]+"),
                    "[AUTH_URL]"),

            // JWT tokens (simplified pattern)
            new SanitizationPattern(
                    Pattern.compile("eyJ[a-zA-Z0-9_-]+\\.eyJ[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+"),
                    "[JWT]"),
    };

    /** Default configuration used for the global sanitizer. */
    private static ErrorSanitizationConfig defaultConfig = ErrorSanitizationConfig.defaults();

    private ErrorSanitizer() {
        // Prevent instantiation
    }

    /**
     * Sets the default sanitization configuration.
     *
     * @param config the configuration to use as default
     */
    public static void setDefaultConfig(ErrorSanitizationConfig config) {
        defaultConfig = config != null ? config : ErrorSanitizationConfig.defaults();
    }

    /**
     * Gets the current default configuration.
     *
     * @return the default configuration
     */
    public static ErrorSanitizationConfig getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * Sanitizes an error message using the default configuration.
     *
     * @param message the message to sanitize
     * @return the sanitized message, or the original if sanitization is disabled
     */
    public static String sanitize(String message) {
        return sanitize(message, defaultConfig);
    }

    /**
     * Sanitizes an error message using the provided configuration.
     *
     * @param message the message to sanitize
     * @param config the sanitization configuration
     * @return the sanitized message, or the original if sanitization is disabled
     */
    public static String sanitize(String message, ErrorSanitizationConfig config) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        if (config == null || !config.isEnabled()) {
            return message;
        }

        String result = message;
        for (SanitizationPattern sp : PATTERNS) {
            result = sp.pattern.matcher(result).replaceAll(sp.replacement);
        }

        return result;
    }

    /**
     * Creates a sanitized exception wrapping the original.
     *
     * @param exception the exception to wrap
     * @param config the sanitization configuration
     * @return a new exception with sanitized message, or the original if disabled
     */
    public static FlagKitException sanitizeException(FlagKitException exception,
            ErrorSanitizationConfig config) {
        if (exception == null || config == null || !config.isEnabled()) {
            return exception;
        }

        // Get the raw message (without error code prefix)
        String originalMessage = exception.getMessage();
        String sanitizedMessage = sanitize(originalMessage, config);

        if (originalMessage.equals(sanitizedMessage)) {
            return exception;
        }

        FlagKitException sanitized = new FlagKitException(
                exception.getErrorCode(), sanitizedMessage, exception.getCause());

        // Copy details
        sanitized.withDetails(exception.getDetails());

        // Optionally preserve original message
        if (config.isPreserveOriginal()) {
            sanitized.withDetail("originalMessage", originalMessage);
        }

        return sanitized;
    }

    /**
     * Checks if a message contains potentially sensitive information.
     *
     * @param message the message to check
     * @return true if the message appears to contain sensitive data
     */
    public static boolean containsSensitiveData(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        for (SanitizationPattern sp : PATTERNS) {
            if (sp.pattern.matcher(message).find()) {
                return true;
            }
        }

        return false;
    }

    /**
     * Pattern container for sanitization rules.
     */
    private static class SanitizationPattern {
        final Pattern pattern;
        final String replacement;

        SanitizationPattern(Pattern pattern, String replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }
    }
}
