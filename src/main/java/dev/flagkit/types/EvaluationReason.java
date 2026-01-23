package dev.flagkit.types;

/**
 * Reasons for flag evaluation results.
 */
public enum EvaluationReason {
    /** Value from cache */
    CACHED("CACHED"),

    /** Default value used */
    DEFAULT("DEFAULT"),

    /** Flag not found */
    FLAG_NOT_FOUND("FLAG_NOT_FOUND"),

    /** Value from bootstrap */
    BOOTSTRAP("BOOTSTRAP"),

    /** Value from server */
    SERVER("SERVER"),

    /** Stale cached value */
    STALE_CACHE("STALE_CACHE"),

    /** Error during evaluation */
    ERROR("ERROR"),

    /** Flag is disabled */
    DISABLED("DISABLED"),

    /** Type mismatch */
    TYPE_MISMATCH("TYPE_MISMATCH"),

    /** Offline mode */
    OFFLINE("OFFLINE");

    private final String value;

    EvaluationReason(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Parses a string to an EvaluationReason.
     *
     * @param value the string value
     * @return the matching EvaluationReason, or DEFAULT if not found
     */
    public static EvaluationReason fromString(String value) {
        if (value == null || value.isEmpty()) {
            return DEFAULT;
        }
        String upperValue = value.toUpperCase();
        for (EvaluationReason reason : values()) {
            if (reason.value.equals(upperValue)) {
                return reason;
            }
        }
        return DEFAULT;
    }
}
