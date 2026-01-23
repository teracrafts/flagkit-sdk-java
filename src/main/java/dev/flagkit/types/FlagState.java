package dev.flagkit.types;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the state of a feature flag.
 */
public class FlagState {
    private final String key;
    private final Object value;
    private final boolean enabled;
    private final int version;
    private final FlagType flagType;
    private final String lastModified;
    private final Map<String, Object> metadata;

    private FlagState(Builder builder) {
        this.key = builder.key;
        this.value = builder.value;
        this.enabled = builder.enabled;
        this.version = builder.version;
        this.flagType = builder.flagType;
        this.lastModified = builder.lastModified;
        this.metadata = builder.metadata;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getVersion() {
        return version;
    }

    public FlagType getFlagType() {
        return flagType;
    }

    public String getLastModified() {
        return lastModified;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
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

    public static class Builder {
        private String key;
        private Object value;
        private boolean enabled = true;
        private int version = 0;
        private FlagType flagType = FlagType.BOOLEAN;
        private String lastModified;
        private Map<String, Object> metadata;

        public Builder key(String key) {
            this.key = key;
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

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder flagType(FlagType flagType) {
            this.flagType = flagType;
            return this;
        }

        public Builder lastModified(String lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public FlagState build() {
            Objects.requireNonNull(key, "key is required");
            if (flagType == null) {
                flagType = FlagType.infer(value);
            }
            if (lastModified == null) {
                lastModified = Instant.now().toString();
            }
            return new FlagState(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlagState flagState = (FlagState) o;
        return enabled == flagState.enabled &&
                version == flagState.version &&
                Objects.equals(key, flagState.key) &&
                Objects.equals(value, flagState.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, enabled, version);
    }

    @Override
    public String toString() {
        return "FlagState{" +
                "key='" + key + '\'' +
                ", value=" + value +
                ", enabled=" + enabled +
                ", version=" + version +
                ", flagType=" + flagType +
                '}';
    }
}
