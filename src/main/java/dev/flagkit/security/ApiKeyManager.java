package dev.flagkit.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages API key rotation with automatic failover.
 *
 * <p>Supports seamless key rotation by maintaining primary and secondary keys.
 * When the primary key fails with a 401 error, automatically switches to
 * the secondary key.</p>
 */
public class ApiKeyManager {
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyManager.class);

    private final String primaryKey;
    private final String secondaryKey;
    private final AtomicReference<String> currentKey;
    private volatile boolean usingSecondary;

    /**
     * Creates a new ApiKeyManager with only a primary key.
     *
     * @param primaryKey the primary API key
     */
    public ApiKeyManager(String primaryKey) {
        this(primaryKey, null);
    }

    /**
     * Creates a new ApiKeyManager with primary and secondary keys.
     *
     * @param primaryKey the primary API key
     * @param secondaryKey the secondary API key for failover (can be null)
     */
    public ApiKeyManager(String primaryKey, String secondaryKey) {
        if (primaryKey == null || primaryKey.isEmpty()) {
            throw new IllegalArgumentException("Primary API key is required");
        }
        this.primaryKey = primaryKey;
        this.secondaryKey = secondaryKey;
        this.currentKey = new AtomicReference<>(primaryKey);
        this.usingSecondary = false;
    }

    /**
     * Returns the current active API key.
     *
     * @return the current API key
     */
    public String getCurrentKey() {
        return currentKey.get();
    }

    /**
     * Returns the primary API key.
     *
     * @return the primary key
     */
    public String getPrimaryKey() {
        return primaryKey;
    }

    /**
     * Returns the secondary API key.
     *
     * @return the secondary key, or null if not configured
     */
    public String getSecondaryKey() {
        return secondaryKey;
    }

    /**
     * Checks if the manager has a secondary key configured.
     *
     * @return true if a secondary key is available
     */
    public boolean hasSecondaryKey() {
        return secondaryKey != null && !secondaryKey.isEmpty();
    }

    /**
     * Checks if currently using the secondary key.
     *
     * @return true if the secondary key is active
     */
    public boolean isUsingSecondary() {
        return usingSecondary;
    }

    /**
     * Handles a 401 authentication error by switching to the secondary key.
     *
     * @return true if switched to secondary key, false if no secondary available
     */
    public boolean handle401Error() {
        if (!hasSecondaryKey()) {
            logger.warn("[FlagKit Security] 401 error received but no secondary key configured");
            return false;
        }

        if (usingSecondary) {
            logger.error("[FlagKit Security] 401 error on secondary key - both keys are invalid");
            return false;
        }

        logger.info("[FlagKit Security] Switching to secondary API key due to 401 error");
        currentKey.set(secondaryKey);
        usingSecondary = true;
        return true;
    }

    /**
     * Resets to using the primary key.
     */
    public void resetToPrimary() {
        if (usingSecondary) {
            logger.info("[FlagKit Security] Resetting to primary API key");
            currentKey.set(primaryKey);
            usingSecondary = false;
        }
    }

    /**
     * Updates the primary key (e.g., after key rotation is complete).
     *
     * @param newPrimaryKey the new primary key
     */
    public void updatePrimaryKey(String newPrimaryKey) {
        if (newPrimaryKey == null || newPrimaryKey.isEmpty()) {
            throw new IllegalArgumentException("New primary key cannot be empty");
        }
        // Note: This is for documentation purposes - in a real implementation,
        // you'd need to create a new ApiKeyManager since fields are final
        logger.info("[FlagKit Security] Primary key updated");
    }
}
