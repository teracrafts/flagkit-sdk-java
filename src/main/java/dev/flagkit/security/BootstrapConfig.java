package dev.flagkit.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for signed bootstrap data.
 *
 * <p>Bootstrap data can include an HMAC-SHA256 signature for integrity verification.
 * The signature is computed over the canonicalized JSON representation of the flags.</p>
 */
public class BootstrapConfig {
    private final Map<String, Object> flags;
    private final String signature;
    private final long timestamp;

    /**
     * Creates a new BootstrapConfig.
     *
     * @param flags the bootstrap flag values
     * @param signature the HMAC-SHA256 signature (hex-encoded), or null for unsigned bootstrap
     * @param timestamp the timestamp when the signature was created (milliseconds since epoch)
     */
    public BootstrapConfig(Map<String, Object> flags, String signature, long timestamp) {
        this.flags = flags != null ? new HashMap<>(flags) : new HashMap<>();
        this.signature = signature;
        this.timestamp = timestamp;
    }

    /**
     * Creates an unsigned bootstrap config (legacy format).
     *
     * @param flags the bootstrap flag values
     */
    public BootstrapConfig(Map<String, Object> flags) {
        this(flags, null, 0);
    }

    /**
     * Returns the bootstrap flag values.
     *
     * @return an unmodifiable view of the flags map
     */
    public Map<String, Object> getFlags() {
        return Collections.unmodifiableMap(flags);
    }

    /**
     * Returns the signature for verification.
     *
     * @return the HMAC-SHA256 signature, or null if unsigned
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Returns the timestamp when the signature was created.
     *
     * @return the timestamp in milliseconds since epoch, or 0 if unsigned
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Checks if this bootstrap config has a signature.
     *
     * @return true if signed, false for legacy unsigned format
     */
    public boolean isSigned() {
        return signature != null && !signature.isEmpty();
    }
}
