package dev.flagkit.security;

import dev.flagkit.error.ErrorCode;
import dev.flagkit.error.FlagKitException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Request signing utilities using HMAC-SHA256.
 *
 * <p>Provides methods for signing HTTP requests to ensure message integrity
 * and authenticate requests to the FlagKit API.</p>
 */
public final class RequestSigning {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long DEFAULT_MAX_AGE_MS = 300_000; // 5 minutes

    private RequestSigning() {
        // Prevent instantiation
    }

    /**
     * Generates an HMAC-SHA256 signature for the given message.
     *
     * @param message the message to sign
     * @param key the signing key
     * @return the hex-encoded signature
     * @throws FlagKitException if signing fails
     */
    public static String generateHmacSha256(String message, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw FlagKitException.securityError(
                    ErrorCode.SECURITY_SIGNATURE_INVALID,
                    "Failed to generate HMAC-SHA256 signature: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a request signature for the given body.
     *
     * <p>The signature is computed over: timestamp.body</p>
     *
     * @param body the request body to sign
     * @param apiKey the API key to use for signing
     * @return the request signature
     */
    public static RequestSignature createRequestSignature(String body, String apiKey) {
        long timestamp = System.currentTimeMillis();
        String message = timestamp + "." + body;
        String signature = generateHmacSha256(message, apiKey);
        String keyId = getKeyId(apiKey);
        return new RequestSignature(signature, timestamp, keyId);
    }

    /**
     * Verifies a request signature.
     *
     * @param body the request body
     * @param signature the signature to verify
     * @param timestamp the timestamp from the signature
     * @param apiKey the API key
     * @return true if the signature is valid
     */
    public static boolean verifyRequestSignature(String body, String signature,
            long timestamp, String apiKey) {
        return verifyRequestSignature(body, signature, timestamp, apiKey, DEFAULT_MAX_AGE_MS);
    }

    /**
     * Verifies a request signature with a custom max age.
     *
     * @param body the request body
     * @param signature the signature to verify
     * @param timestamp the timestamp from the signature
     * @param apiKey the API key
     * @param maxAgeMs maximum allowed age in milliseconds
     * @return true if the signature is valid and not expired
     */
    public static boolean verifyRequestSignature(String body, String signature,
            long timestamp, String apiKey, long maxAgeMs) {
        // Check timestamp age
        long now = System.currentTimeMillis();
        long age = now - timestamp;

        // Check if too old
        if (age > maxAgeMs) {
            return false;
        }

        // Check if timestamp is too far in the future (5 minute clock skew tolerance)
        if (age < -300_000) {
            return false;
        }

        // Verify signature with constant-time comparison
        String message = timestamp + "." + body;
        String expectedSignature = generateHmacSha256(message, apiKey);

        return MessageDigest.isEqual(
                signature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extracts the key ID from an API key.
     *
     * @param apiKey the API key
     * @return the first 8 characters of the key, or the full key if shorter
     */
    public static String getKeyId(String apiKey) {
        if (apiKey == null) {
            return "";
        }
        return apiKey.length() >= 8 ? apiKey.substring(0, 8) : apiKey;
    }

    /**
     * Converts a byte array to a hex string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
