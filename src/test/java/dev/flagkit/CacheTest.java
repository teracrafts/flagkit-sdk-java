package dev.flagkit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CacheTest {

    private Cache cache;

    @BeforeEach
    void setUp() {
        cache = new Cache(Duration.ofMinutes(5), 100);
    }

    @Test
    void testSetAndGet() {
        FlagState flag = FlagState.builder()
                .key("test-flag")
                .value(true)
                .enabled(true)
                .version(1)
                .build();

        cache.set("test-flag", flag);

        FlagState result = cache.get("test-flag");
        assertNotNull(result);
        assertEquals("test-flag", result.getKey());
        assertEquals(true, result.getValue());
    }

    @Test
    void testGetNonExistent() {
        FlagState result = cache.get("non-existent");
        assertNull(result);
    }

    @Test
    void testExpiration() throws InterruptedException {
        Cache shortTtlCache = new Cache(Duration.ofMillis(50), 100);

        FlagState flag = FlagState.builder()
                .key("test-flag")
                .value(true)
                .build();

        shortTtlCache.set("test-flag", flag);

        // Should exist immediately
        assertNotNull(shortTtlCache.get("test-flag"));

        // Wait for expiration
        Thread.sleep(100);

        // Should be expired now
        assertNull(shortTtlCache.get("test-flag"));
    }

    @Test
    void testGetStale() throws InterruptedException {
        Cache shortTtlCache = new Cache(Duration.ofMillis(50), 100);

        FlagState flag = FlagState.builder()
                .key("test-flag")
                .value(true)
                .build();

        shortTtlCache.set("test-flag", flag);

        // Wait for expiration
        Thread.sleep(100);

        // GetStale should still return the value
        FlagState result = shortTtlCache.getStale("test-flag");
        assertNotNull(result);
        assertEquals(true, result.getValue());
    }

    @Test
    void testHas() {
        FlagState flag = FlagState.builder().key("test-flag").value(true).build();
        cache.set("test-flag", flag);

        assertTrue(cache.has("test-flag"));
        assertFalse(cache.has("non-existent"));
    }

    @Test
    void testDelete() {
        FlagState flag = FlagState.builder().key("test-flag").value(true).build();
        cache.set("test-flag", flag);

        assertTrue(cache.delete("test-flag"));
        assertNull(cache.get("test-flag"));
        assertFalse(cache.delete("non-existent"));
    }

    @Test
    void testClear() {
        cache.set("flag1", FlagState.builder().key("flag1").value(true).build());
        cache.set("flag2", FlagState.builder().key("flag2").value(true).build());

        cache.clear();

        assertNull(cache.get("flag1"));
        assertNull(cache.get("flag2"));
        assertEquals(0, cache.size());
    }

    @Test
    void testSetMany() {
        cache.setMany(Arrays.asList(
                FlagState.builder().key("flag1").value(true).build(),
                FlagState.builder().key("flag2").value("test").build(),
                FlagState.builder().key("flag3").value(42.0).build()
        ));

        assertNotNull(cache.get("flag1"));
        assertNotNull(cache.get("flag2"));
        assertNotNull(cache.get("flag3"));
    }

    @Test
    void testGetAllKeys() {
        cache.set("flag1", FlagState.builder().key("flag1").value(true).build());
        cache.set("flag2", FlagState.builder().key("flag2").value(true).build());

        var keys = cache.getAllKeys();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("flag1"));
        assertTrue(keys.contains("flag2"));
    }

    @Test
    void testStats() {
        cache.set("flag1", FlagState.builder().key("flag1").value(true).build());
        cache.set("flag2", FlagState.builder().key("flag2").value(true).build());

        // Hit
        cache.get("flag1");
        // Miss
        cache.get("non-existent");

        Map<String, Integer> stats = cache.getStats();
        assertEquals(2, stats.get("size").intValue());
        assertEquals(1, stats.get("hits").intValue());
        assertEquals(1, stats.get("misses").intValue());
    }

    @Test
    void testEviction() {
        Cache smallCache = new Cache(Duration.ofMinutes(5), 2);

        smallCache.set("flag1", FlagState.builder().key("flag1").value(true).build());
        smallCache.set("flag2", FlagState.builder().key("flag2").value(true).build());
        smallCache.set("flag3", FlagState.builder().key("flag3").value(true).build());

        // Should only have 2 entries
        assertEquals(2, smallCache.size());
    }
}
