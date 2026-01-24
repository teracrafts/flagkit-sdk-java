package dev.flagkit.utils;

import org.slf4j.Logger;

import java.util.*;

/**
 * Security utilities for FlagKit SDK.
 *
 * <p>Provides methods for detecting potential PII in data and validating API keys.</p>
 */
public final class Security {

    /**
     * Common PII field patterns (case-insensitive).
     */
    private static final List<String> PII_PATTERNS = Arrays.asList(
            "email",
            "phone",
            "telephone",
            "mobile",
            "ssn",
            "social_security",
            "socialSecurity",
            "credit_card",
            "creditCard",
            "card_number",
            "cardNumber",
            "cvv",
            "password",
            "passwd",
            "secret",
            "token",
            "api_key",
            "apiKey",
            "private_key",
            "privateKey",
            "access_token",
            "accessToken",
            "refresh_token",
            "refreshToken",
            "auth_token",
            "authToken",
            "address",
            "street",
            "zip_code",
            "zipCode",
            "postal_code",
            "postalCode",
            "date_of_birth",
            "dateOfBirth",
            "dob",
            "birth_date",
            "birthDate",
            "passport",
            "driver_license",
            "driverLicense",
            "national_id",
            "nationalId",
            "bank_account",
            "bankAccount",
            "routing_number",
            "routingNumber",
            "iban",
            "swift"
    );

    private Security() {
        // Prevent instantiation
    }

    /**
     * Checks if a field name potentially contains PII based on common patterns.
     *
     * @param fieldName the field name to check
     * @return true if the field name matches a PII pattern
     */
    public static boolean isPotentialPIIField(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return false;
        }

        String lowerName = fieldName.toLowerCase();
        return PII_PATTERNS.stream()
                .anyMatch(pattern -> lowerName.contains(pattern.toLowerCase()));
    }

    /**
     * Detects potential PII fields in a map and returns the field paths.
     *
     * @param data the data map to scan
     * @param prefix the prefix for nested field paths (empty string for root)
     * @return a list of field paths that potentially contain PII
     */
    public static List<String> detectPotentialPII(Map<String, Object> data, String prefix) {
        List<String> piiFields = new ArrayList<>();

        if (data == null) {
            return piiFields;
        }

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String fullPath = (prefix == null || prefix.isEmpty()) ? key : prefix + "." + key;

            if (isPotentialPIIField(key)) {
                piiFields.add(fullPath);
            }

            // Recursively check nested objects
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                List<String> nestedPII = detectPotentialPII(nestedMap, fullPath);
                piiFields.addAll(nestedPII);
            }
        }

        return piiFields;
    }

    /**
     * Detects potential PII fields in a map starting from the root.
     *
     * @param data the data map to scan
     * @return a list of field paths that potentially contain PII
     */
    public static List<String> detectPotentialPII(Map<String, Object> data) {
        return detectPotentialPII(data, "");
    }

    /**
     * Logs a warning if potential PII is detected in the data.
     *
     * @param data the data map to scan
     * @param dataType the type of data being scanned (e.g., "context", "event")
     * @param logger the logger to use for warnings (can be null)
     */
    public static void warnIfPotentialPII(Map<String, Object> data, String dataType, Logger logger) {
        if (data == null || logger == null) {
            return;
        }

        List<String> piiFields = detectPotentialPII(data);

        if (!piiFields.isEmpty()) {
            String fieldList = String.join(", ", piiFields);
            String advice = "context".equals(dataType)
                    ? "Consider adding these to privateAttributes."
                    : "Consider removing sensitive data from events.";

            logger.warn("[FlagKit Security] Potential PII detected in {} data: {}. {}",
                    dataType, fieldList, advice);
        }
    }

    /**
     * Checks if an API key is a server key.
     *
     * @param apiKey the API key to check
     * @return true if the key starts with "srv_"
     */
    public static boolean isServerKey(String apiKey) {
        return apiKey != null && apiKey.startsWith("srv_");
    }

    /**
     * Checks if an API key is a client/SDK key.
     *
     * @param apiKey the API key to check
     * @return true if the key starts with "sdk_" or "cli_"
     */
    public static boolean isClientKey(String apiKey) {
        return apiKey != null && (apiKey.startsWith("sdk_") || apiKey.startsWith("cli_"));
    }

    /**
     * Logs a warning if a server key is used in a browser-like environment.
     *
     * <p>In Java, "browser-like" environments are detected by checking for
     * common applet or Android WebView indicators. This is primarily useful
     * when the SDK might be used in environments where the key could be exposed.</p>
     *
     * @param apiKey the API key to check
     * @param logger the logger to use for warnings (can be null)
     */
    public static void warnIfServerKeyInBrowser(String apiKey, Logger logger) {
        if (isBrowserLikeEnvironment() && isServerKey(apiKey)) {
            String message = "[FlagKit Security] WARNING: Server keys (srv_) should not be used in " +
                    "browser-like environments. This exposes your server key in client-side code, " +
                    "which is a security risk. Use SDK keys (sdk_) for client-side applications instead. " +
                    "See: https://docs.flagkit.dev/sdk/security#api-keys";

            // Always log to stderr for visibility
            System.err.println(message);

            // Also log through the SDK logger if available
            if (logger != null) {
                logger.warn(message);
            }
        }
    }

    /**
     * Detects if the current environment is browser-like.
     *
     * <p>This checks for indicators that suggest the code might be running
     * in an environment where API keys could be exposed, such as:</p>
     * <ul>
     *   <li>Android WebView (android.webkit.WebView class present)</li>
     *   <li>Java applets (deprecated but still checked)</li>
     *   <li>JavaFX WebView environments</li>
     * </ul>
     *
     * @return true if the environment appears to be browser-like
     */
    public static boolean isBrowserLikeEnvironment() {
        // Check for Android WebView
        try {
            Class.forName("android.webkit.WebView");
            return true;
        } catch (ClassNotFoundException ignored) {
            // Not Android
        }

        // Check for JavaFX WebView
        try {
            Class.forName("javafx.scene.web.WebView");
            // If we're in a JavaFX WebView context, it might be browser-like
            String javaFxRuntime = System.getProperty("javafx.runtime.version");
            if (javaFxRuntime != null) {
                return true;
            }
        } catch (ClassNotFoundException ignored) {
            // Not JavaFX
        }

        // Check for Java Applet context (deprecated but still checked)
        String javaApplet = System.getProperty("java.applet.host");
        if (javaApplet != null) {
            return true;
        }

        return false;
    }

    /**
     * Configuration options for security features.
     */
    public static class SecurityConfig {
        private boolean warnOnPotentialPII;
        private boolean warnOnServerKeyInBrowser;
        private List<String> additionalPIIPatterns;

        private SecurityConfig(Builder builder) {
            this.warnOnPotentialPII = builder.warnOnPotentialPII;
            this.warnOnServerKeyInBrowser = builder.warnOnServerKeyInBrowser;
            this.additionalPIIPatterns = builder.additionalPIIPatterns != null
                    ? new ArrayList<>(builder.additionalPIIPatterns)
                    : new ArrayList<>();
        }

        /**
         * Returns whether PII warnings are enabled.
         *
         * @return true if PII warnings are enabled
         */
        public boolean isWarnOnPotentialPII() {
            return warnOnPotentialPII;
        }

        /**
         * Returns whether server key in browser warnings are enabled.
         *
         * @return true if server key warnings are enabled
         */
        public boolean isWarnOnServerKeyInBrowser() {
            return warnOnServerKeyInBrowser;
        }

        /**
         * Returns the additional PII patterns to detect.
         *
         * @return a copy of the additional PII patterns list
         */
        public List<String> getAdditionalPIIPatterns() {
            return new ArrayList<>(additionalPIIPatterns);
        }

        /**
         * Creates a new builder for SecurityConfig.
         *
         * @return a new builder instance
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Creates a default SecurityConfig.
         *
         * @return a SecurityConfig with default settings
         */
        public static SecurityConfig defaults() {
            return builder().build();
        }

        /**
         * Builder for SecurityConfig.
         */
        public static class Builder {
            private boolean warnOnPotentialPII = !isProduction();
            private boolean warnOnServerKeyInBrowser = true;
            private List<String> additionalPIIPatterns = new ArrayList<>();

            private Builder() {
            }

            /**
             * Sets whether to warn about potential PII in context/events.
             *
             * @param warn true to enable warnings
             * @return this builder
             */
            public Builder warnOnPotentialPII(boolean warn) {
                this.warnOnPotentialPII = warn;
                return this;
            }

            /**
             * Sets whether to warn when server keys are used in browser-like environments.
             *
             * @param warn true to enable warnings
             * @return this builder
             */
            public Builder warnOnServerKeyInBrowser(boolean warn) {
                this.warnOnServerKeyInBrowser = warn;
                return this;
            }

            /**
             * Sets additional PII patterns to detect.
             *
             * @param patterns the additional patterns
             * @return this builder
             */
            public Builder additionalPIIPatterns(List<String> patterns) {
                this.additionalPIIPatterns = patterns != null ? new ArrayList<>(patterns) : new ArrayList<>();
                return this;
            }

            /**
             * Adds an additional PII pattern to detect.
             *
             * @param pattern the pattern to add
             * @return this builder
             */
            public Builder addPIIPattern(String pattern) {
                if (pattern != null) {
                    this.additionalPIIPatterns.add(pattern);
                }
                return this;
            }

            /**
             * Builds the SecurityConfig.
             *
             * @return the built SecurityConfig
             */
            public SecurityConfig build() {
                return new SecurityConfig(this);
            }

            private static boolean isProduction() {
                String env = System.getProperty("flagkit.environment",
                        System.getenv("FLAGKIT_ENVIRONMENT"));
                if (env != null) {
                    return "production".equalsIgnoreCase(env) || "prod".equalsIgnoreCase(env);
                }

                // Also check common Java production indicators
                String profile = System.getProperty("spring.profiles.active",
                        System.getenv("SPRING_PROFILES_ACTIVE"));
                if (profile != null) {
                    return profile.toLowerCase().contains("prod");
                }

                return false;
            }
        }
    }
}
