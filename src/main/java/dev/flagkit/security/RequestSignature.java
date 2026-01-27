package dev.flagkit.security;

/**
 * Container for request signature data.
 *
 * <p>Used for HMAC-SHA256 request signing to ensure message integrity.</p>
 */
public class RequestSignature {
    private final String signature;
    private final long timestamp;
    private final String keyId;

    /**
     * Creates a new RequestSignature.
     *
     * @param signature the HMAC-SHA256 signature (hex-encoded)
     * @param timestamp the timestamp when signature was created (milliseconds since epoch)
     * @param keyId the key identifier (first 8 characters of API key)
     */
    public RequestSignature(String signature, long timestamp, String keyId) {
        this.signature = signature;
        this.timestamp = timestamp;
        this.keyId = keyId;
    }

    /**
     * Returns the HMAC-SHA256 signature.
     *
     * @return the hex-encoded signature
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Returns the timestamp when the signature was created.
     *
     * @return timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the key identifier.
     *
     * @return the first 8 characters of the API key used for signing
     */
    public String getKeyId() {
        return keyId;
    }
}
