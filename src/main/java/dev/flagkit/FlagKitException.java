package dev.flagkit;

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
}
