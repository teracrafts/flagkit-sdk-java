package dev.flagkit.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.flagkit.error.ErrorCode;
import dev.flagkit.error.FlagKitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Bootstrap signature verification utilities.
 *
 * <p>Provides methods for signing and verifying bootstrap flag data using HMAC-SHA256.</p>
 */
public final class BootstrapVerification {
    private static final Logger logger = LoggerFactory.getLogger(BootstrapVerification.class);
    private static final Gson gson = new GsonBuilder().create();
    private static final long CLOCK_SKEW_TOLERANCE_MS = 300_000; // 5 minutes

    private BootstrapVerification() {
        // Prevent instantiation
    }

    /**
     * Creates a signed bootstrap configuration.
     *
     * @param flags the flag values to sign
     * @param apiKey the API key for signing
     * @return a signed BootstrapConfig
     * @throws FlagKitException if signing fails
     */
    public static BootstrapConfig createSignedBootstrap(Map<String, Object> flags, String apiKey) {
        long timestamp = System.currentTimeMillis();
        String signature = signBootstrap(flags, apiKey, timestamp);
        return new BootstrapConfig(flags, signature, timestamp);
    }

    /**
     * Signs bootstrap flags with HMAC-SHA256.
     *
     * @param flags the flag values to sign
     * @param apiKey the API key for signing
     * @param timestamp the signature timestamp
     * @return the hex-encoded signature
     * @throws FlagKitException if signing fails
     */
    public static String signBootstrap(Map<String, Object> flags, String apiKey, long timestamp) {
        String canonical = canonicalizeObject(flags);
        String message = timestamp + "." + canonical;
        return RequestSigning.generateHmacSha256(message, apiKey);
    }

    /**
     * Verifies a bootstrap configuration's signature.
     *
     * @param bootstrap the bootstrap config to verify
     * @param apiKey the API key for verification
     * @param config the verification configuration
     * @return true if valid, false if verification was skipped or ignored
     * @throws FlagKitException if verification fails and config is set to error
     */
    public static boolean verifyBootstrapSignature(BootstrapConfig bootstrap,
            String apiKey, BootstrapVerificationConfig config) {

        if (!config.isEnabled()) {
            return true;
        }

        // Skip verification for unsigned bootstrap (legacy format)
        if (!bootstrap.isSigned()) {
            logger.debug("Bootstrap data is unsigned, skipping verification");
            return true;
        }

        try {
            // Check timestamp age
            if (config.getMaxAge() != null && bootstrap.getTimestamp() > 0) {
                long now = System.currentTimeMillis();
                long age = now - bootstrap.getTimestamp();
                long maxAgeMs = config.getMaxAge().toMillis();

                if (age > maxAgeMs) {
                    throw FlagKitException.securityError(
                            ErrorCode.SECURITY_BOOTSTRAP_EXPIRED,
                            String.format("Bootstrap data is expired: age %dms exceeds max age %dms",
                                    age, maxAgeMs));
                }

                // Check for future timestamp (clock skew tolerance)
                if (age < -CLOCK_SKEW_TOLERANCE_MS) {
                    throw FlagKitException.securityError(
                            ErrorCode.SECURITY_BOOTSTRAP_INVALID,
                            "Bootstrap timestamp is in the future");
                }
            }

            // Canonicalize flags for deterministic signature verification
            String canonical = canonicalizeObject(bootstrap.getFlags());

            // Build message: timestamp.canonical_json
            String message = bootstrap.getTimestamp() + "." + canonical;

            // Generate expected signature
            String expectedSignature = RequestSigning.generateHmacSha256(message, apiKey);

            // Use constant-time comparison
            if (!java.security.MessageDigest.isEqual(
                    bootstrap.getSignature().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    expectedSignature.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                throw FlagKitException.securityError(
                        ErrorCode.SECURITY_SIGNATURE_INVALID,
                        "Bootstrap signature verification failed: signature mismatch");
            }

            logger.debug("Bootstrap signature verified successfully");
            return true;

        } catch (FlagKitException e) {
            return handleVerificationFailure(e, config);
        }
    }

    /**
     * Canonicalizes an object to a deterministic JSON string.
     *
     * <p>Keys are sorted alphabetically at all levels to ensure consistent output.</p>
     *
     * @param obj the object to canonicalize
     * @return the canonical JSON string
     */
    public static String canonicalizeObject(Map<String, Object> obj) {
        if (obj == null) {
            return "null";
        }
        return gson.toJson(sortMapRecursively(obj));
    }

    /**
     * Recursively sorts map keys alphabetically.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> sortMapRecursively(Map<String, Object> map) {
        TreeMap<String, Object> sorted = new TreeMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                value = sortMapRecursively((Map<String, Object>) value);
            } else if (value instanceof List) {
                value = sortListRecursively((List<Object>) value);
            }
            sorted.put(entry.getKey(), value);
        }
        return sorted;
    }

    /**
     * Recursively processes lists, sorting any nested maps.
     */
    @SuppressWarnings("unchecked")
    private static List<Object> sortListRecursively(List<Object> list) {
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map) {
                result.add(sortMapRecursively((Map<String, Object>) item));
            } else if (item instanceof List) {
                result.add(sortListRecursively((List<Object>) item));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Handles verification failure based on configuration.
     */
    private static boolean handleVerificationFailure(FlagKitException e,
            BootstrapVerificationConfig config) {
        if (config.shouldError()) {
            throw e;
        } else if (config.shouldWarn()) {
            logger.warn("[FlagKit Security] Bootstrap verification failed: {}", e.getMessage());
            return false;
        }
        // ON_FAILURE_IGNORE
        return false;
    }
}
