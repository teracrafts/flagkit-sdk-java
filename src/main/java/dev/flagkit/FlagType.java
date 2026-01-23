package dev.flagkit;

/**
 * Types of feature flags.
 */
public enum FlagType {
    BOOLEAN("boolean"),
    STRING("string"),
    NUMBER("number"),
    JSON("json");

    private final String value;

    FlagType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FlagType fromValue(String value) {
        for (FlagType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return JSON;
    }

    public static FlagType infer(Object value) {
        if (value instanceof Boolean) {
            return BOOLEAN;
        } else if (value instanceof String) {
            return STRING;
        } else if (value instanceof Number) {
            return NUMBER;
        } else {
            return JSON;
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
