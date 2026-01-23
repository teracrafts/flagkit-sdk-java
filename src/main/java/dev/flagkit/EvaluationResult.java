package dev.flagkit;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Result of evaluating a feature flag.
 */
public class EvaluationResult {
    private final String flagKey;
    private final Object value;
    private final boolean enabled;
    private final EvaluationReason reason;
    private final int version;
    private final Instant timestamp;

    private EvaluationResult(Builder builder) {
        this.flagKey = builder.flagKey;
        this.value = builder.value;
        this.enabled = builder.enabled;
        this.reason = builder.reason;
        this.version = builder.version;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
    }

    public String getFlagKey() {
        return flagKey;
    }

    public Object getValue() {
        return value;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public EvaluationReason getReason() {
        return reason;
    }

    public int getVersion() {
        return version;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean getBooleanValue() {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    public String getStringValue() {
        if (value instanceof String) {
            return (String) value;
        }
        return value != null ? value.toString() : null;
    }

    public double getNumberValue() {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    public int getIntValue() {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getJsonValue() {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static EvaluationResult defaultResult(String key, Object defaultValue, EvaluationReason reason) {
        return builder()
                .flagKey(key)
                .value(defaultValue)
                .enabled(false)
                .reason(reason)
                .build();
    }

    public static class Builder {
        private String flagKey;
        private Object value;
        private boolean enabled;
        private EvaluationReason reason = EvaluationReason.DEFAULT;
        private int version;
        private Instant timestamp;

        public Builder flagKey(String flagKey) {
            this.flagKey = flagKey;
            return this;
        }

        public Builder value(Object value) {
            this.value = value;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder reason(EvaluationReason reason) {
            this.reason = reason;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public EvaluationResult build() {
            return new EvaluationResult(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvaluationResult that = (EvaluationResult) o;
        return enabled == that.enabled &&
                version == that.version &&
                Objects.equals(flagKey, that.flagKey) &&
                Objects.equals(value, that.value) &&
                reason == that.reason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(flagKey, value, enabled, reason, version);
    }

    @Override
    public String toString() {
        return "EvaluationResult{" +
                "flagKey='" + flagKey + '\'' +
                ", value=" + value +
                ", enabled=" + enabled +
                ", reason=" + reason +
                ", version=" + version +
                '}';
    }
}
