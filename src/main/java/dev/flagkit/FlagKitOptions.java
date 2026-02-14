package dev.flagkit;

import dev.flagkit.error.ErrorCode;
import dev.flagkit.error.FlagKitException;
import dev.flagkit.types.FlagState;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import dev.flagkit.security.BootstrapConfig;
import dev.flagkit.security.BootstrapVerificationConfig;
import dev.flagkit.security.EvaluationJitterConfig;
import dev.flagkit.security.ErrorSanitizationConfig;

/**
 * Configuration options for the FlagKit client.
 */
public class FlagKitOptions {
    public static final Duration DEFAULT_POLLING_INTERVAL = Duration.ofSeconds(30);
    public static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(5);
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    public static final int DEFAULT_RETRIES = 3;
    public static final String SDK_VERSION = "1.0.8";

    private final String apiKey;
    private final Duration pollingInterval;
    private final boolean enablePolling;
    private final boolean cacheEnabled;
    private final Duration cacheTtl;
    private final boolean offline;
    private final Duration timeout;
    private final int retries;
    private final Map<String, Object> bootstrap;
    private final boolean debug;
    private final Runnable onReady;
    private final Consumer<Throwable> onError;
    private final Consumer<java.util.List<FlagState>> onUpdate;

    // Security configuration
    private final boolean enableRequestSigning;
    private final boolean enableCacheEncryption;
    private final BootstrapConfig bootstrapConfig;
    private final BootstrapVerificationConfig bootstrapVerification;
    private final EvaluationJitterConfig evaluationJitter;
    private final ErrorSanitizationConfig errorSanitization;

    private FlagKitOptions(Builder builder) {
        this.apiKey = builder.apiKey;
        this.pollingInterval = builder.pollingInterval;
        this.enablePolling = builder.enablePolling;
        this.cacheEnabled = builder.cacheEnabled;
        this.cacheTtl = builder.cacheTtl;
        this.offline = builder.offline;
        this.timeout = builder.timeout;
        this.retries = builder.retries;
        this.bootstrap = builder.bootstrap;
        this.debug = builder.debug;
        this.onReady = builder.onReady;
        this.onError = builder.onError;
        this.onUpdate = builder.onUpdate;
        // Security configuration
        this.enableRequestSigning = builder.enableRequestSigning;
        this.enableCacheEncryption = builder.enableCacheEncryption;
        this.bootstrapConfig = builder.bootstrapConfig;
        this.bootstrapVerification = builder.bootstrapVerification;
        this.evaluationJitter = builder.evaluationJitter;
        this.errorSanitization = builder.errorSanitization;
    }

    public String getApiKey() {
        return apiKey;
    }

    public Duration getPollingInterval() {
        return pollingInterval;
    }

    public boolean isEnablePolling() {
        return enablePolling;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public boolean isOffline() {
        return offline;
    }
    public Duration getTimeout() {
        return timeout;
    }

    public int getRetries() {
        return retries;
    }

    public Map<String, Object> getBootstrap() {
        return new HashMap<>(bootstrap);
    }

    public boolean isDebug() {
        return debug;
    }

    public Runnable getOnReady() {
        return onReady;
    }

    public Consumer<Throwable> getOnError() {
        return onError;
    }

    public Consumer<java.util.List<FlagState>> getOnUpdate() {
        return onUpdate;
    }

    public boolean isEnableRequestSigning() {
        return enableRequestSigning;
    }

    public boolean isEnableCacheEncryption() {
        return enableCacheEncryption;
    }

    public BootstrapConfig getBootstrapConfig() {
        return bootstrapConfig;
    }

    public BootstrapVerificationConfig getBootstrapVerification() {
        return bootstrapVerification;
    }

    public EvaluationJitterConfig getEvaluationJitter() {
        return evaluationJitter;
    }

    public ErrorSanitizationConfig getErrorSanitization() {
        return errorSanitization;
    }

    public void validate() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw FlagKitException.configError(ErrorCode.CONFIG_MISSING_REQUIRED, "API key is required");
        }

        if (apiKey.length() < 10) {
            throw FlagKitException.authError(ErrorCode.AUTH_INVALID_KEY, "API key is too short");
        }

        if (!apiKey.startsWith("sdk_") && !apiKey.startsWith("srv_") && !apiKey.startsWith("cli_")) {
            throw FlagKitException.authError(ErrorCode.AUTH_INVALID_KEY, "Invalid API key prefix");
        }

        if (pollingInterval.toMillis() < 1000) {
            throw FlagKitException.configError(ErrorCode.CONFIG_INVALID_INTERVAL,
                    "Polling interval must be at least 1 second");
        }
    }

    public static Builder builder(String apiKey) {
        return new Builder(apiKey);
    }

    public static class Builder {
        private final String apiKey;
        private Duration pollingInterval = DEFAULT_POLLING_INTERVAL;
        private boolean enablePolling = true;
        private boolean cacheEnabled = true;
        private Duration cacheTtl = DEFAULT_CACHE_TTL;
        private boolean offline = false;
        private Duration timeout = DEFAULT_TIMEOUT;
        private int retries = DEFAULT_RETRIES;
        private Map<String, Object> bootstrap = new HashMap<>();
        private boolean debug = false;
        private Runnable onReady;
        private Consumer<Throwable> onError;
        private Consumer<java.util.List<FlagState>> onUpdate;
        // Security configuration defaults
        private boolean enableRequestSigning = true;
        private boolean enableCacheEncryption = false;
        private BootstrapConfig bootstrapConfig = null;
        private BootstrapVerificationConfig bootstrapVerification = BootstrapVerificationConfig.defaults();
        private EvaluationJitterConfig evaluationJitter = EvaluationJitterConfig.disabled();
        private ErrorSanitizationConfig errorSanitization = ErrorSanitizationConfig.defaults();

        public Builder(String apiKey) {
            this.apiKey = Objects.requireNonNull(apiKey, "apiKey is required");
        }

        public Builder pollingInterval(Duration pollingInterval) {
            this.pollingInterval = pollingInterval;
            return this;
        }

        public Builder enablePolling(boolean enablePolling) {
            this.enablePolling = enablePolling;
            return this;
        }

        public Builder disablePolling() {
            this.enablePolling = false;
            return this;
        }

        public Builder cacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
            return this;
        }

        public Builder disableCache() {
            this.cacheEnabled = false;
            return this;
        }

        public Builder cacheTtl(Duration cacheTtl) {
            this.cacheTtl = cacheTtl;
            return this;
        }

        public Builder offline(boolean offline) {
            this.offline = offline;
            return this;
        }

        public Builder offline() {
            this.offline = true;
            return this;
        }
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder retries(int retries) {
            this.retries = retries;
            return this;
        }

        public Builder bootstrap(Map<String, Object> bootstrap) {
            this.bootstrap = new HashMap<>(bootstrap);
            return this;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Builder debug() {
            this.debug = true;
            return this;
        }

        public Builder onReady(Runnable onReady) {
            this.onReady = onReady;
            return this;
        }

        public Builder onError(Consumer<Throwable> onError) {
            this.onError = onError;
            return this;
        }

        public Builder onUpdate(Consumer<java.util.List<FlagState>> onUpdate) {
            this.onUpdate = onUpdate;
            return this;
        }

        /**
         * Enables or disables request signing (HMAC-SHA256).
         * Default: enabled
         */
        public Builder enableRequestSigning(boolean enable) {
            this.enableRequestSigning = enable;
            return this;
        }

        /**
         * Disables request signing.
         */
        public Builder disableRequestSigning() {
            this.enableRequestSigning = false;
            return this;
        }

        /**
         * Enables or disables cache encryption (AES-256-GCM).
         * Default: disabled
         */
        public Builder enableCacheEncryption(boolean enable) {
            this.enableCacheEncryption = enable;
            return this;
        }

        /**
         * Enables cache encryption.
         */
        public Builder enableCacheEncryption() {
            this.enableCacheEncryption = true;
            return this;
        }

        /**
         * Sets signed bootstrap configuration.
         * Use this instead of bootstrap() when using signed bootstrap data.
         */
        public Builder bootstrapConfig(BootstrapConfig config) {
            this.bootstrapConfig = config;
            if (config != null && config.getFlags() != null) {
                this.bootstrap = new HashMap<>(config.getFlags());
            }
            return this;
        }

        /**
         * Sets bootstrap with signature for verification.
         */
        public Builder bootstrapWithSignature(Map<String, Object> flags, String signature, long timestamp) {
            this.bootstrapConfig = new BootstrapConfig(flags, signature, timestamp);
            this.bootstrap = new HashMap<>(flags);
            return this;
        }

        /**
         * Configures bootstrap verification settings.
         */
        public Builder bootstrapVerification(BootstrapVerificationConfig config) {
            this.bootstrapVerification = config != null ? config : BootstrapVerificationConfig.defaults();
            return this;
        }

        /**
         * Disables bootstrap verification.
         */
        public Builder disableBootstrapVerification() {
            this.bootstrapVerification = BootstrapVerificationConfig.disabled();
            return this;
        }

        /**
         * Configures evaluation jitter settings for timing attack protection.
         */
        public Builder evaluationJitter(EvaluationJitterConfig config) {
            this.evaluationJitter = config != null ? config : EvaluationJitterConfig.disabled();
            return this;
        }

        /**
         * Enables evaluation jitter with default settings (5-15ms).
         */
        public Builder enableEvaluationJitter() {
            this.evaluationJitter = EvaluationJitterConfig.defaults();
            return this;
        }

        /**
         * Enables evaluation jitter with custom min/max milliseconds.
         */
        public Builder enableEvaluationJitter(int minMs, int maxMs) {
            this.evaluationJitter = new EvaluationJitterConfig(true, minMs, maxMs);
            return this;
        }

        /**
         * Configures error sanitization settings.
         */
        public Builder errorSanitization(ErrorSanitizationConfig config) {
            this.errorSanitization = config != null ? config : ErrorSanitizationConfig.defaults();
            return this;
        }

        /**
         * Enables error sanitization with debug preservation.
         */
        public Builder errorSanitizationWithPreservation() {
            this.errorSanitization = new ErrorSanitizationConfig(true, true);
            return this;
        }

        /**
         * Disables error sanitization.
         */
        public Builder disableErrorSanitization() {
            this.errorSanitization = ErrorSanitizationConfig.disabled();
            return this;
        }

        public FlagKitOptions build() {
            return new FlagKitOptions(this);
        }
    }
}
