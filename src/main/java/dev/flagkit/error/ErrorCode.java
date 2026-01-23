package dev.flagkit.error;

/**
 * Error codes for FlagKit SDK errors.
 */
public enum ErrorCode {
    // Initialization errors
    INIT_FAILED("INIT_FAILED", false),
    INIT_TIMEOUT("INIT_TIMEOUT", true),
    INIT_ALREADY_INITIALIZED("INIT_ALREADY_INITIALIZED", false),
    INIT_NOT_INITIALIZED("INIT_NOT_INITIALIZED", false),

    // Authentication errors
    AUTH_INVALID_KEY("AUTH_INVALID_KEY", false),
    AUTH_EXPIRED_KEY("AUTH_EXPIRED_KEY", false),
    AUTH_MISSING_KEY("AUTH_MISSING_KEY", false),
    AUTH_UNAUTHORIZED("AUTH_UNAUTHORIZED", false),

    // Network errors
    NETWORK_ERROR("NETWORK_ERROR", true),
    NETWORK_TIMEOUT("NETWORK_TIMEOUT", true),
    NETWORK_RETRY_LIMIT("NETWORK_RETRY_LIMIT", true),

    // Evaluation errors
    EVAL_FLAG_NOT_FOUND("EVAL_FLAG_NOT_FOUND", false),
    EVAL_TYPE_MISMATCH("EVAL_TYPE_MISMATCH", false),
    EVAL_INVALID_KEY("EVAL_INVALID_KEY", false),
    EVAL_INVALID_VALUE("EVAL_INVALID_VALUE", false),
    EVAL_DISABLED("EVAL_DISABLED", false),
    EVAL_ERROR("EVAL_ERROR", false),
    EVAL_CONTEXT_ERROR("EVAL_CONTEXT_ERROR", false),
    EVAL_DEFAULT_USED("EVAL_DEFAULT_USED", false),
    EVAL_STALE_VALUE("EVAL_STALE_VALUE", true),
    EVAL_CACHE_MISS("EVAL_CACHE_MISS", true),
    EVAL_NETWORK_ERROR("EVAL_NETWORK_ERROR", true),
    EVAL_PARSE_ERROR("EVAL_PARSE_ERROR", false),
    EVAL_TIMEOUT_ERROR("EVAL_TIMEOUT_ERROR", true),

    // Cache errors
    CACHE_READ_ERROR("CACHE_READ_ERROR", false),
    CACHE_WRITE_ERROR("CACHE_WRITE_ERROR", false),
    CACHE_INVALID_DATA("CACHE_INVALID_DATA", false),
    CACHE_EXPIRED("CACHE_EXPIRED", true),
    CACHE_STORAGE_ERROR("CACHE_STORAGE_ERROR", false),

    // Event errors
    EVENT_QUEUE_FULL("EVENT_QUEUE_FULL", false),
    EVENT_INVALID_TYPE("EVENT_INVALID_TYPE", false),
    EVENT_INVALID_DATA("EVENT_INVALID_DATA", false),
    EVENT_SEND_FAILED("EVENT_SEND_FAILED", true),
    EVENT_FLUSH_FAILED("EVENT_FLUSH_FAILED", true),
    EVENT_FLUSH_TIMEOUT("EVENT_FLUSH_TIMEOUT", true),

    // Circuit breaker errors
    CIRCUIT_OPEN("CIRCUIT_OPEN", true),

    // HTTP errors
    HTTP_BAD_REQUEST("HTTP_BAD_REQUEST", false),
    HTTP_UNAUTHORIZED("HTTP_UNAUTHORIZED", false),
    HTTP_FORBIDDEN("HTTP_FORBIDDEN", false),
    HTTP_NOT_FOUND("HTTP_NOT_FOUND", false),
    HTTP_RATE_LIMITED("HTTP_RATE_LIMITED", true),
    HTTP_SERVER_ERROR("HTTP_SERVER_ERROR", true),
    HTTP_TIMEOUT("HTTP_TIMEOUT", true),
    HTTP_NETWORK_ERROR("HTTP_NETWORK_ERROR", true),
    HTTP_INVALID_RESPONSE("HTTP_INVALID_RESPONSE", false),
    HTTP_CIRCUIT_OPEN("HTTP_CIRCUIT_OPEN", true),

    // SDK lifecycle errors
    SDK_NOT_INITIALIZED("SDK_NOT_INITIALIZED", false),
    SDK_ALREADY_INITIALIZED("SDK_ALREADY_INITIALIZED", false),
    SDK_NOT_READY("SDK_NOT_READY", false),

    // Configuration errors
    CONFIG_INVALID_URL("CONFIG_INVALID_URL", false),
    CONFIG_INVALID_INTERVAL("CONFIG_INVALID_INTERVAL", false),
    CONFIG_MISSING_REQUIRED("CONFIG_MISSING_REQUIRED", false),
    CONFIG_INVALID_API_KEY("CONFIG_INVALID_API_KEY", false),
    CONFIG_INVALID_BASE_URL("CONFIG_INVALID_BASE_URL", false),
    CONFIG_INVALID_POLLING_INTERVAL("CONFIG_INVALID_POLLING_INTERVAL", false),
    CONFIG_INVALID_CACHE_TTL("CONFIG_INVALID_CACHE_TTL", false);

    private final String code;
    private final boolean recoverable;

    ErrorCode(String code, boolean recoverable) {
        this.code = code;
        this.recoverable = recoverable;
    }

    public String getCode() {
        return code;
    }

    public boolean isRecoverable() {
        return recoverable;
    }

    @Override
    public String toString() {
        return code;
    }
}
