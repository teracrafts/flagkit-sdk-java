package dev.flagkit.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all FlagKit SDK errors.
 */
public class FlagKitException extends RuntimeException {
    private final ErrorCode errorCode;
    private final boolean recoverable;
    private final Map<String, Object> details;

    public FlagKitException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.recoverable = errorCode.isRecoverable();
        this.details = new HashMap<>();
    }

    public FlagKitException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.recoverable = errorCode.isRecoverable();
        this.details = new HashMap<>();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public boolean isRecoverable() {
        return recoverable;
    }

    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }

    public FlagKitException withDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }

    public FlagKitException withDetails(Map<String, Object> details) {
        this.details.putAll(details);
        return this;
    }

    @Override
    public String getMessage() {
        return String.format("[%s] %s", errorCode.getCode(), super.getMessage());
    }

    /**
     * Creates an initialization error.
     */
    public static FlagKitException initError(String message) {
        return new FlagKitException(ErrorCode.INIT_FAILED, message);
    }

    /**
     * Creates an authentication error.
     */
    public static FlagKitException authError(ErrorCode code, String message) {
        return new FlagKitException(code, message);
    }

    /**
     * Creates a network error.
     */
    public static FlagKitException networkError(String message, Throwable cause) {
        return new FlagKitException(ErrorCode.NETWORK_ERROR, message, cause);
    }

    /**
     * Creates an evaluation error.
     */
    public static FlagKitException evalError(ErrorCode code, String message) {
        return new FlagKitException(code, message);
    }

    /**
     * Creates a configuration error.
     */
    public static FlagKitException configError(ErrorCode code, String message) {
        return new FlagKitException(code, message);
    }

    /**
     * Creates a network error with error code.
     */
    public static FlagKitException networkError(ErrorCode code, String message) {
        return new FlagKitException(code, message);
    }

    /**
     * Creates a network error with error code and cause.
     */
    public static FlagKitException networkError(ErrorCode code, String message, Throwable cause) {
        return new FlagKitException(code, message, cause);
    }

    /**
     * Creates an evaluation error.
     */
    public static FlagKitException evaluationError(ErrorCode code, String message) {
        return new FlagKitException(code, message);
    }

    /**
     * Creates an SDK lifecycle error.
     */
    public static FlagKitException sdkError(ErrorCode code, String message) {
        return new FlagKitException(code, message);
    }

    /**
     * Creates a not initialized error.
     */
    public static FlagKitException notInitialized() {
        return new FlagKitException(ErrorCode.SDK_NOT_INITIALIZED, "SDK not initialized. Call FlagKit.initialize() first.");
    }

    /**
     * Creates an already initialized error.
     */
    public static FlagKitException alreadyInitialized() {
        return new FlagKitException(ErrorCode.SDK_ALREADY_INITIALIZED, "SDK already initialized.");
    }

    /**
     * Alias for errorCode getter.
     */
    public ErrorCode getCode() {
        return errorCode;
    }

    /**
     * Checks if this is a config error.
     */
    public boolean isConfigError() {
        return errorCode.getCode().startsWith("CONFIG_");
    }

    /**
     * Checks if this is a network error.
     */
    public boolean isNetworkError() {
        return errorCode.getCode().startsWith("HTTP_") || errorCode.getCode().startsWith("NETWORK_");
    }

    /**
     * Checks if this is an evaluation error.
     */
    public boolean isEvaluationError() {
        return errorCode.getCode().startsWith("EVAL_");
    }

    /**
     * Checks if this is an SDK lifecycle error.
     */
    public boolean isSdkError() {
        return errorCode.getCode().startsWith("SDK_") || errorCode.getCode().startsWith("INIT_");
    }

    /**
     * Checks if this is a security error.
     */
    public boolean isSecurityError() {
        return errorCode.getCode().startsWith("SECURITY_");
    }

    /**
     * Creates a security error.
     */
    public static FlagKitException securityError(ErrorCode code, String message) {
        return new FlagKitException(code, message);
    }

    /**
     * Creates a security error with cause.
     */
    public static FlagKitException securityError(ErrorCode code, String message, Throwable cause) {
        return new FlagKitException(code, message, cause);
    }
}
