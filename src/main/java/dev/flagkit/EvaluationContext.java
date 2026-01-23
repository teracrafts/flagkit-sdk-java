package dev.flagkit;

import java.util.*;

/**
 * Context for flag evaluation containing user and environment information.
 */
public class EvaluationContext {
    private String userId;
    private String email;
    private String name;
    private boolean anonymous;
    private String country;
    private String deviceType;
    private String os;
    private String browser;
    private final Map<String, Object> custom;
    private final Set<String> privateAttributes;

    private EvaluationContext() {
        this.custom = new HashMap<>();
        this.privateAttributes = new HashSet<>();
    }

    public static EvaluationContext create(String userId) {
        EvaluationContext ctx = new EvaluationContext();
        ctx.userId = userId;
        ctx.anonymous = false;
        return ctx;
    }

    public static EvaluationContext anonymous() {
        EvaluationContext ctx = new EvaluationContext();
        ctx.anonymous = true;
        ctx.userId = UUID.randomUUID().toString();
        return ctx;
    }

    public EvaluationContext withEmail(String email) {
        this.email = email;
        return this;
    }

    public EvaluationContext withName(String name) {
        this.name = name;
        return this;
    }

    public EvaluationContext withCountry(String country) {
        this.country = country;
        return this;
    }

    public EvaluationContext withDeviceType(String deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public EvaluationContext withOs(String os) {
        this.os = os;
        return this;
    }

    public EvaluationContext withBrowser(String browser) {
        this.browser = browser;
        return this;
    }

    public EvaluationContext withCustom(String key, Object value) {
        this.custom.put(key, value);
        return this;
    }

    public EvaluationContext withCustom(Map<String, Object> custom) {
        this.custom.putAll(custom);
        return this;
    }

    public EvaluationContext withPrivateAttribute(String attribute) {
        this.privateAttributes.add(attribute);
        return this;
    }

    public EvaluationContext withPrivateAttributes(Collection<String> attributes) {
        this.privateAttributes.addAll(attributes);
        return this;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public String getCountry() {
        return country;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getOs() {
        return os;
    }

    public String getBrowser() {
        return browser;
    }

    public Map<String, Object> getCustom() {
        return new HashMap<>(custom);
    }

    public Set<String> getPrivateAttributes() {
        return new HashSet<>(privateAttributes);
    }

    /**
     * Merges another context into this one. Values from the other context take precedence.
     */
    public EvaluationContext merge(EvaluationContext other) {
        if (other == null) {
            return this;
        }

        EvaluationContext merged = new EvaluationContext();
        merged.userId = this.userId;
        merged.email = this.email;
        merged.name = this.name;
        merged.anonymous = this.anonymous;
        merged.country = this.country;
        merged.deviceType = this.deviceType;
        merged.os = this.os;
        merged.browser = this.browser;
        merged.custom.putAll(this.custom);
        merged.privateAttributes.addAll(this.privateAttributes);

        // Override with other's values
        if (other.userId != null) merged.userId = other.userId;
        if (other.email != null) merged.email = other.email;
        if (other.name != null) merged.name = other.name;
        if (other.country != null) merged.country = other.country;
        if (other.deviceType != null) merged.deviceType = other.deviceType;
        if (other.os != null) merged.os = other.os;
        if (other.browser != null) merged.browser = other.browser;
        if (other.anonymous) merged.anonymous = true;

        merged.custom.putAll(other.custom);
        merged.privateAttributes.addAll(other.privateAttributes);

        return merged;
    }

    /**
     * Creates a copy with private attributes stripped.
     */
    public EvaluationContext stripPrivateAttributes() {
        EvaluationContext stripped = new EvaluationContext();
        stripped.userId = this.userId;
        stripped.anonymous = this.anonymous;

        if (!privateAttributes.contains("email")) stripped.email = this.email;
        if (!privateAttributes.contains("name")) stripped.name = this.name;
        if (!privateAttributes.contains("country")) stripped.country = this.country;
        if (!privateAttributes.contains("deviceType")) stripped.deviceType = this.deviceType;
        if (!privateAttributes.contains("os")) stripped.os = this.os;
        if (!privateAttributes.contains("browser")) stripped.browser = this.browser;

        for (Map.Entry<String, Object> entry : this.custom.entrySet()) {
            if (!privateAttributes.contains(entry.getKey())) {
                stripped.custom.put(entry.getKey(), entry.getValue());
            }
        }

        return stripped;
    }

    /**
     * Creates a deep copy of this context.
     */
    public EvaluationContext copy() {
        EvaluationContext copy = new EvaluationContext();
        copy.userId = this.userId;
        copy.email = this.email;
        copy.name = this.name;
        copy.anonymous = this.anonymous;
        copy.country = this.country;
        copy.deviceType = this.deviceType;
        copy.os = this.os;
        copy.browser = this.browser;
        copy.custom.putAll(this.custom);
        copy.privateAttributes.addAll(this.privateAttributes);
        return copy;
    }

    /**
     * Converts context to a map for serialization.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        if (userId != null) map.put("userId", userId);
        if (email != null) map.put("email", email);
        if (name != null) map.put("name", name);
        if (anonymous) map.put("anonymous", anonymous);
        if (country != null) map.put("country", country);
        if (deviceType != null) map.put("deviceType", deviceType);
        if (os != null) map.put("os", os);
        if (browser != null) map.put("browser", browser);
        if (!custom.isEmpty()) map.put("custom", new HashMap<>(custom));

        return map;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvaluationContext that = (EvaluationContext) o;
        return anonymous == that.anonymous &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(email, that.email) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email, name, anonymous);
    }

    @Override
    public String toString() {
        return "EvaluationContext{" +
                "userId='" + userId + '\'' +
                ", anonymous=" + anonymous +
                '}';
    }
}
