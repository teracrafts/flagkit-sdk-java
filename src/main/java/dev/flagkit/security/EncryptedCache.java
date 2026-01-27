package dev.flagkit.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import dev.flagkit.error.ErrorCode;
import dev.flagkit.error.FlagKitException;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * AES-256-GCM encrypted cache storage.
 *
 * <p>Provides authenticated encryption for cached flag data with keys
 * derived from the API key using PBKDF2.</p>
 */
public class EncryptedCache {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_LENGTH = 256; // AES-256
    private static final int IV_LENGTH = 12; // 96 bits for GCM
    private static final int TAG_LENGTH = 128; // GCM authentication tag length
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final String SALT = "FlagKit-v1-cache";
    private static final int ENCRYPTION_VERSION = 1;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;
    private final Gson gson;

    /**
     * Creates a new EncryptedCache with a key derived from the API key.
     *
     * @param apiKey the API key to derive the encryption key from
     * @throws FlagKitException if key derivation fails
     */
    public EncryptedCache(String apiKey) {
        this.secretKey = deriveKey(apiKey);
        this.secureRandom = new SecureRandom();
        this.gson = new GsonBuilder().create();
    }

    /**
     * Encrypts data using AES-256-GCM.
     *
     * @param plaintext the data to encrypt
     * @return the encrypted data as a JSON string containing IV, ciphertext, and version
     * @throws FlagKitException if encryption fails
     */
    public String encrypt(String plaintext) {
        try {
            // Generate random IV
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Create cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Create encrypted data structure
            EncryptedData data = new EncryptedData();
            data.iv = Base64.getEncoder().encodeToString(iv);
            data.data = Base64.getEncoder().encodeToString(ciphertext);
            data.version = ENCRYPTION_VERSION;

            return gson.toJson(data);
        } catch (Exception e) {
            throw FlagKitException.securityError(
                    ErrorCode.SECURITY_ENCRYPTION_FAILED,
                    "Failed to encrypt data: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypts data encrypted with {@link #encrypt(String)}.
     *
     * @param encryptedJson the encrypted data JSON string
     * @return the decrypted plaintext
     * @throws FlagKitException if decryption fails
     */
    public String decrypt(String encryptedJson) {
        try {
            // Parse encrypted data structure
            EncryptedData data = gson.fromJson(encryptedJson, EncryptedData.class);

            // Check version
            if (data.version != ENCRYPTION_VERSION) {
                throw FlagKitException.securityError(
                        ErrorCode.SECURITY_DECRYPTION_FAILED,
                        "Unsupported encryption version: " + data.version);
            }

            // Decode IV and ciphertext
            byte[] iv = Base64.getDecoder().decode(data.iv);
            byte[] ciphertext = Base64.getDecoder().decode(data.data);

            // Create cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (JsonSyntaxException e) {
            throw FlagKitException.securityError(
                    ErrorCode.SECURITY_DECRYPTION_FAILED,
                    "Invalid encrypted data format", e);
        } catch (FlagKitException e) {
            throw e;
        } catch (Exception e) {
            throw FlagKitException.securityError(
                    ErrorCode.SECURITY_DECRYPTION_FAILED,
                    "Failed to decrypt data: " + e.getMessage(), e);
        }
    }

    /**
     * Encrypts a JSON object.
     *
     * @param value the object to encrypt
     * @return the encrypted data as a JSON string
     * @throws FlagKitException if encryption fails
     */
    public String encryptJson(Object value) {
        String json = gson.toJson(value);
        return encrypt(json);
    }

    /**
     * Decrypts and parses a JSON object.
     *
     * @param encryptedJson the encrypted data
     * @param type the class to deserialize to
     * @param <T> the type parameter
     * @return the decrypted and parsed object
     * @throws FlagKitException if decryption or parsing fails
     */
    public <T> T decryptJson(String encryptedJson, Class<T> type) {
        String json = decrypt(encryptedJson);
        return gson.fromJson(json, type);
    }

    /**
     * Checks if a string appears to be encrypted data.
     *
     * @param data the data to check
     * @return true if the data appears to be in encrypted format
     */
    public static boolean isEncrypted(String data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        // Check if it looks like our encrypted JSON format
        return data.startsWith("{\"iv\":") || data.contains("\"version\":");
    }

    /**
     * Derives an AES-256 key from the API key using PBKDF2.
     */
    private SecretKeySpec deriveKey(String apiKey) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
            KeySpec spec = new PBEKeySpec(
                    apiKey.toCharArray(),
                    SALT.getBytes(StandardCharsets.UTF_8),
                    PBKDF2_ITERATIONS,
                    KEY_LENGTH);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw FlagKitException.securityError(
                    ErrorCode.SECURITY_KEY_DERIVATION_FAILED,
                    "Failed to derive encryption key: " + e.getMessage(), e);
        }
    }

    /**
     * Internal class representing encrypted data structure.
     */
    private static class EncryptedData {
        String iv;
        String data;
        int version;
    }
}
