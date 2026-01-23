package dev.flagkit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In-memory cache for flag states with TTL support.
 */
public class Cache {
    private static final Logger logger = LoggerFactory.getLogger(Cache.class);

    private final Map<String, CacheEntry> entries;
    private final Duration defaultTtl;
    private final int maxSize;
    private final ReadWriteLock lock;

    private int hits;
    private int misses;

    public Cache(Duration defaultTtl, int maxSize) {
        this.entries = new ConcurrentHashMap<>();
        this.defaultTtl = defaultTtl;
        this.maxSize = maxSize;
        this.lock = new ReentrantReadWriteLock();
        this.hits = 0;
        this.misses = 0;
    }

    public Cache(Duration defaultTtl) {
        this(defaultTtl, 1000);
    }

    /**
     * Gets a flag from the cache. Returns null if not found or expired.
     */
    public FlagState get(String key) {
        lock.readLock().lock();
        try {
            CacheEntry entry = entries.get(key);
            if (entry == null) {
                misses++;
                return null;
            }

            if (entry.isExpired()) {
                misses++;
                logger.debug("Cache miss (expired): {}", key);
                return null;
            }

            hits++;
            entry.updateAccessTime();
            logger.debug("Cache hit: {}", key);
            return entry.getFlag();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets a flag from the cache even if expired.
     */
    public FlagState getStale(String key) {
        lock.readLock().lock();
        try {
            CacheEntry entry = entries.get(key);
            if (entry == null) {
                return null;
            }
            return entry.getFlag();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Sets a flag in the cache with the default TTL.
     */
    public void set(String key, FlagState flag) {
        set(key, flag, defaultTtl);
    }

    /**
     * Sets a flag in the cache with a specific TTL.
     */
    public void set(String key, FlagState flag, Duration ttl) {
        lock.writeLock().lock();
        try {
            // Enforce max size
            if (entries.size() >= maxSize && !entries.containsKey(key)) {
                evictOldest();
            }

            entries.put(key, new CacheEntry(flag, ttl));
            logger.debug("Cache set: {} (ttl: {})", key, ttl);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Sets multiple flags in the cache.
     */
    public void setMany(List<FlagState> flags) {
        setMany(flags, defaultTtl);
    }

    /**
     * Sets multiple flags in the cache with a specific TTL.
     */
    public void setMany(List<FlagState> flags, Duration ttl) {
        for (FlagState flag : flags) {
            set(flag.getKey(), flag, ttl);
        }
    }

    /**
     * Checks if a key exists in the cache (including stale entries).
     */
    public boolean has(String key) {
        lock.readLock().lock();
        try {
            return entries.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Checks if a cached entry is stale.
     */
    public boolean isStale(String key) {
        lock.readLock().lock();
        try {
            CacheEntry entry = entries.get(key);
            return entry != null && entry.isExpired();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Deletes a flag from the cache.
     */
    public boolean delete(String key) {
        lock.writeLock().lock();
        try {
            CacheEntry removed = entries.remove(key);
            if (removed != null) {
                logger.debug("Cache delete: {}", key);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears all entries from the cache.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            int size = entries.size();
            entries.clear();
            logger.debug("Cache cleared: {} entries", size);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns all cached flag keys.
     */
    public Set<String> getAllKeys() {
        lock.readLock().lock();
        try {
            return new HashSet<>(entries.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns all cached flags (including stale).
     */
    public List<FlagState> getAll() {
        lock.readLock().lock();
        try {
            List<FlagState> flags = new ArrayList<>();
            for (CacheEntry entry : entries.values()) {
                flags.add(entry.getFlag());
            }
            return flags;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the number of cached entries.
     */
    public int size() {
        lock.readLock().lock();
        try {
            return entries.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns cache statistics.
     */
    public Map<String, Integer> getStats() {
        lock.readLock().lock();
        try {
            int validCount = 0;
            int staleCount = 0;

            for (CacheEntry entry : entries.values()) {
                if (entry.isExpired()) {
                    staleCount++;
                } else {
                    validCount++;
                }
            }

            Map<String, Integer> stats = new HashMap<>();
            stats.put("size", entries.size());
            stats.put("valid_count", validCount);
            stats.put("stale_count", staleCount);
            stats.put("max_size", maxSize);
            stats.put("hits", hits);
            stats.put("misses", misses);
            return stats;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Evicts the oldest entry from the cache.
     */
    private void evictOldest() {
        String oldestKey = null;
        Instant oldestTime = null;

        for (Map.Entry<String, CacheEntry> entry : entries.entrySet()) {
            Instant fetchedAt = entry.getValue().getFetchedAt();
            if (oldestTime == null || fetchedAt.isBefore(oldestTime)) {
                oldestKey = entry.getKey();
                oldestTime = fetchedAt;
            }
        }

        if (oldestKey != null) {
            entries.remove(oldestKey);
            logger.debug("Cache evicted oldest: {}", oldestKey);
        }
    }

    /**
     * Internal cache entry with TTL tracking.
     */
    private static class CacheEntry {
        private final FlagState flag;
        private final Instant fetchedAt;
        private final Instant expiresAt;
        private Instant lastAccessedAt;

        CacheEntry(FlagState flag, Duration ttl) {
            this.flag = flag;
            this.fetchedAt = Instant.now();
            this.expiresAt = fetchedAt.plus(ttl);
            this.lastAccessedAt = fetchedAt;
        }

        FlagState getFlag() {
            return flag;
        }

        Instant getFetchedAt() {
            return fetchedAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        void updateAccessTime() {
            this.lastAccessedAt = Instant.now();
        }
    }
}
