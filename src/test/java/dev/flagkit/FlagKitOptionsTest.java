package dev.flagkit;

import dev.flagkit.error.ErrorCode;
import dev.flagkit.error.FlagKitException;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FlagKitOptionsTest {

    @Test
    void testBuilderWithApiKey() {
        FlagKitOptions options = FlagKitOptions.builder("sdk_test_key").build();
        assertEquals("sdk_test_key", options.getApiKey());
    }

    @Test
    void testDefaultValues() {
        FlagKitOptions options = FlagKitOptions.builder("sdk_test_key").build();

        assertEquals(FlagKitOptions.DEFAULT_POLLING_INTERVAL, options.getPollingInterval());
        assertEquals(FlagKitOptions.DEFAULT_CACHE_TTL, options.getCacheTtl());
        assertTrue(options.isCacheEnabled());
        assertEquals(FlagKitOptions.DEFAULT_TIMEOUT, options.getTimeout());
        assertEquals(FlagKitOptions.DEFAULT_RETRIES, options.getRetries());
    }

    @Test
    void testCustomValues() {
        FlagKitOptions options = FlagKitOptions.builder("sdk_test_key")
                .pollingInterval(Duration.ofSeconds(60))
                .cacheTtl(Duration.ofMinutes(10))
                .cacheEnabled(false)
                .timeout(Duration.ofSeconds(30))
                .retries(5)
                .build();

        assertEquals(Duration.ofSeconds(60), options.getPollingInterval());
        assertEquals(Duration.ofMinutes(10), options.getCacheTtl());
        assertFalse(options.isCacheEnabled());
        assertEquals(Duration.ofSeconds(30), options.getTimeout());
        assertEquals(5, options.getRetries());
    }

    @Test
    void testBootstrapData() {
        Map<String, Object> bootstrap = new HashMap<>();
        bootstrap.put("dark-mode", true);
        bootstrap.put("max-items", 100);

        FlagKitOptions options = FlagKitOptions.builder("sdk_test_key")
                .bootstrap(bootstrap)
                .build();

        assertNotNull(options.getBootstrap());
        assertEquals(true, options.getBootstrap().get("dark-mode"));
        assertEquals(100, options.getBootstrap().get("max-items"));
    }

    @Test
    void testValidateEmptyApiKey() {
        FlagKitOptions options = FlagKitOptions.builder("").build();

        FlagKitException exception = assertThrows(FlagKitException.class, options::validate);
        assertEquals(ErrorCode.CONFIG_MISSING_REQUIRED, exception.getCode());
    }

    @Test
    void testValidateValidSdkPrefix() {
        FlagKitOptions options = FlagKitOptions.builder("sdk_valid_key_12345").build();
        assertDoesNotThrow(options::validate);
    }

    @Test
    void testValidateValidSrvPrefix() {
        FlagKitOptions options = FlagKitOptions.builder("srv_server_key_12345").build();
        assertDoesNotThrow(options::validate);
    }

    @Test
    void testValidateValidCliPrefix() {
        FlagKitOptions options = FlagKitOptions.builder("cli_client_key_12345").build();
        assertDoesNotThrow(options::validate);
    }

    @Test
    void testValidateInvalidApiKeyPrefix() {
        FlagKitOptions options = FlagKitOptions.builder("invalid_key_12345").build();

        FlagKitException exception = assertThrows(FlagKitException.class, options::validate);
        assertEquals(ErrorCode.AUTH_INVALID_KEY, exception.getCode());
    }

    @Test
    void testValidateShortApiKey() {
        FlagKitOptions options = FlagKitOptions.builder("sdk_short").build();

        FlagKitException exception = assertThrows(FlagKitException.class, options::validate);
        assertEquals(ErrorCode.AUTH_INVALID_KEY, exception.getCode());
    }

    @Test
    void testValidateTooShortPollingInterval() {
        FlagKitOptions options = FlagKitOptions.builder("sdk_test_key_12345")
                .pollingInterval(Duration.ofMillis(500))
                .build();

        FlagKitException exception = assertThrows(FlagKitException.class, options::validate);
        assertEquals(ErrorCode.CONFIG_INVALID_INTERVAL, exception.getCode());
    }

    @Test
    void testBuilderIsFluent() {
        FlagKitOptions.Builder builder = FlagKitOptions.builder("sdk_test_key");

        assertSame(builder, builder.pollingInterval(Duration.ofSeconds(30)));
        assertSame(builder, builder.cacheTtl(Duration.ofMinutes(5)));
    }

    @Test
    void testOfflineMode() {
        FlagKitOptions options = FlagKitOptions.builder("sdk_test_key")
                .offline()
                .build();

        assertTrue(options.isOffline());
    }

    @Test
    void testDebugMode() {
        FlagKitOptions options = FlagKitOptions.builder("sdk_test_key")
                .debug()
                .build();

        assertTrue(options.isDebug());
    }

    @Test
    void testDisablePolling() {
        FlagKitOptions options = FlagKitOptions.builder("sdk_test_key")
                .disablePolling()
                .build();

        assertFalse(options.isEnablePolling());
    }

    @Test
    void testDisableCache() {
        FlagKitOptions options = FlagKitOptions.builder("sdk_test_key")
                .disableCache()
                .build();

        assertFalse(options.isCacheEnabled());
    }

    @Test
    void testLocalPortMode() {
        FlagKitOptions options = FlagKitOptions.builder("sdk_test_key")
                .localPort(8200)
                .build();

        assertEquals(8200, options.getLocalPort());
    }

    @Test
    void testLocalPortCustomValue() {
        FlagKitOptions options = FlagKitOptions.builder("sdk_test_key")
                .localPort(3000)
                .build();

        assertEquals(3000, options.getLocalPort());
    }

    @Test
    void testLocalPortDefaultsNull() {
        FlagKitOptions options = FlagKitOptions.builder("sdk_test_key").build();

        assertNull(options.getLocalPort());
    }
}
