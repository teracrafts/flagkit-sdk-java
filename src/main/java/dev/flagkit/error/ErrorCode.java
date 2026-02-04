package dev.flagkit.error;

/**
 * Error codes for FlagKit SDK errors.
 *
 * Error code ranges:
 * - Initialization: 1000-1099
 * - Authentication: 1100-1199
 * - Evaluation: 1200-1299
 * - Network: 1300-1399
 * - Cache: 1400-1499
 * - Event: 1500-1599
 * - Configuration: 1600-1699
 * - Security: 1700-1799
 * - Streaming: 1800-1899
 * - Internal: 1900-1999
 */
public enum ErrorCode {
    // Initialization errors (1000-1099)
    INIT_FAILED(1000, "SDK initialization failed", false),
    INIT_TIMEOUT(1001, "Initialization timed out", true),
    INIT_ALREADY_INITIALIZED(1002, "SDK already initialized", false),
    INIT_NOT_INITIALIZED(1003, "SDK not initialized", false),

    // Authentication errors (1100-1199)
    AUTH_INVALID_KEY(1100, "Invalid API key", false),
    AUTH_EXPIRED_KEY(1101, "API key has expired", false),
    AUTH_MISSING_KEY(1102, "API key is missing", false),
    AUTH_UNAUTHORIZED(1103, "Unauthorized access", false),
    AUTH_REVOKED_KEY(1104, "API key has been revoked", false),
    AUTH_INSUFFICIENT_PERMISSIONS(1105, "API key lacks required permissions", false),
    AUTH_ENVIRONMENT_MISMATCH(1106, "API key not valid for this environment", false),
    AUTH_IP_RESTRICTED(1107, "IP address not allowed for this API key", false),
    AUTH_ORGANIZATION_REQUIRED(1108, "Organization context missing from token", false),
    AUTH_SUBSCRIPTION_SUSPENDED(1109, "Subscription is suspended", false),

    // Network errors (1300-1399)
    NETWORK_ERROR(1300, "Network request failed", true),
    NETWORK_TIMEOUT(1301, "Request timed out", true),
    NETWORK_RETRY_LIMIT(1302, "Retry limit exceeded", true),
    NETWORK_DNS_ERROR(1303, "DNS resolution failed", true),
    NETWORK_CONNECTION_REFUSED(1304, "Connection refused", true),
    NETWORK_SSL_ERROR(1305, "SSL/TLS error", false),
    NETWORK_OFFLINE(1306, "Device is offline", true),
    NETWORK_INVALID_RESPONSE(1307, "Invalid server response", true),
    NETWORK_SERVICE_UNAVAILABLE(1308, "Service unavailable", true),

    // Evaluation errors (1200-1299)
    EVAL_FLAG_NOT_FOUND(1200, "Flag does not exist", false),
    EVAL_TYPE_MISMATCH(1201, "Flag value type mismatch", false),
    EVAL_INVALID_KEY(1202, "Invalid flag key", false),
    EVAL_INVALID_VALUE(1203, "Invalid flag value", false),
    EVAL_DISABLED(1204, "Flag is disabled", false),
    EVAL_ERROR(1205, "Evaluation error", false),
    EVAL_CONTEXT_ERROR(1206, "Invalid evaluation context", false),
    EVAL_DEFAULT_USED(1207, "Using default value", false),
    EVAL_STALE_VALUE(1208, "Using stale cached value", true),
    EVAL_CACHE_MISS(1209, "Cache miss", true),
    EVAL_NETWORK_ERROR(1210, "Network error during evaluation", true),
    EVAL_PARSE_ERROR(1211, "Error parsing flag value", false),
    EVAL_TIMEOUT_ERROR(1212, "Evaluation timed out", true),

    // Cache errors (1400-1499)
    CACHE_READ_ERROR(1400, "Failed to read from cache", false),
    CACHE_WRITE_ERROR(1401, "Failed to write to cache", false),
    CACHE_INVALID_DATA(1402, "Cache data is invalid", false),
    CACHE_EXPIRED(1403, "Cache has expired", true),
    CACHE_STORAGE_ERROR(1404, "Cache storage error", false),

    // Event errors (1500-1599)
    EVENT_QUEUE_FULL(1500, "Event queue is full", false),
    EVENT_INVALID_TYPE(1501, "Invalid event type", false),
    EVENT_INVALID_DATA(1502, "Invalid event data", false),
    EVENT_SEND_FAILED(1503, "Failed to send event", true),
    EVENT_FLUSH_FAILED(1504, "Failed to flush events", true),
    EVENT_FLUSH_TIMEOUT(1505, "Event flush timed out", true),

    // Circuit breaker errors
    CIRCUIT_OPEN(1350, "Circuit breaker is open", true),

    // HTTP errors
    HTTP_BAD_REQUEST(1310, "Bad request", false),
    HTTP_UNAUTHORIZED(1311, "Unauthorized", false),
    HTTP_FORBIDDEN(1312, "Forbidden", false),
    HTTP_NOT_FOUND(1313, "Not found", false),
    HTTP_RATE_LIMITED(1314, "Rate limit exceeded", true),
    HTTP_SERVER_ERROR(1315, "Server error", true),
    HTTP_TIMEOUT(1316, "HTTP timeout", true),
    HTTP_NETWORK_ERROR(1317, "HTTP network error", true),
    HTTP_INVALID_RESPONSE(1318, "Invalid HTTP response", false),
    HTTP_CIRCUIT_OPEN(1319, "HTTP circuit breaker open", true),

    // SDK lifecycle errors
    SDK_NOT_INITIALIZED(1004, "SDK not initialized", false),
    SDK_ALREADY_INITIALIZED(1005, "SDK already initialized", false),
    SDK_NOT_READY(1006, "SDK not ready", false),

    // Configuration errors (1600-1699)
    CONFIG_INVALID_URL(1600, "Invalid URL configuration", false),
    CONFIG_INVALID_INTERVAL(1601, "Invalid interval configuration", false),
    CONFIG_MISSING_REQUIRED(1602, "Missing required configuration", false),
    CONFIG_INVALID_API_KEY(1603, "Invalid API key configuration", false),
    CONFIG_INVALID_BASE_URL(1604, "Invalid base URL configuration", false),
    CONFIG_INVALID_POLLING_INTERVAL(1605, "Invalid polling interval configuration", false),
    CONFIG_INVALID_CACHE_TTL(1606, "Invalid cache TTL configuration", false),

    // Security errors (1700-1799)
    SECURITY_PII_DETECTED(1700, "PII detected in data", false),
    SECURITY_SIGNATURE_INVALID(1701, "Invalid signature", false),
    SECURITY_SIGNATURE_EXPIRED(1702, "Signature has expired", false),
    SECURITY_ENCRYPTION_FAILED(1703, "Encryption failed", false),
    SECURITY_DECRYPTION_FAILED(1704, "Decryption failed", false),
    SECURITY_BOOTSTRAP_INVALID(1705, "Invalid bootstrap data", false),
    SECURITY_BOOTSTRAP_EXPIRED(1706, "Bootstrap data has expired", false),
    SECURITY_KEY_DERIVATION_FAILED(1707, "Key derivation failed", false),

    // Streaming errors (1800-1899)
    STREAMING_TOKEN_INVALID(1800, "Stream token is invalid", true),
    STREAMING_TOKEN_EXPIRED(1801, "Stream token has expired", true),
    STREAMING_SUBSCRIPTION_SUSPENDED(1802, "Organization subscription suspended", false),
    STREAMING_CONNECTION_LIMIT(1803, "Too many concurrent streaming connections", true),
    STREAMING_UNAVAILABLE(1804, "Streaming service not available", true);

    private final int numericCode;
    private final String message;
    private final boolean recoverable;

    ErrorCode(int numericCode, String message, boolean recoverable) {
        this.numericCode = numericCode;
        this.message = message;
        this.recoverable = recoverable;
    }

    /**
     * Returns the numeric error code.
     * @return the numeric code
     */
    public int getNumericCode() {
        return numericCode;
    }

    /**
     * Returns the string error code (enum name).
     * @return the string code
     */
    public String getCode() {
        return name();
    }

    /**
     * Returns the default error message.
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns whether this error is recoverable (can be retried).
     * @return true if recoverable, false otherwise
     */
    public boolean isRecoverable() {
        return recoverable;
    }

    @Override
    public String toString() {
        return name();
    }

    /**
     * Finds an ErrorCode by its numeric code.
     * @param numericCode the numeric code to search for
     * @return the matching ErrorCode, or null if not found
     */
    public static ErrorCode fromNumericCode(int numericCode) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.numericCode == numericCode) {
                return errorCode;
            }
        }
        return null;
    }
}
