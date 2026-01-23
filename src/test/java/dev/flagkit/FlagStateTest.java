package dev.flagkit;

import dev.flagkit.types.FlagState;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FlagStateTest {

    @Test
    void testBuilderCreatesState() {
        FlagState state = FlagState.builder()
                .key("test-flag")
                .value(true)
                .enabled(true)
                .version(10)
                .build();

        assertEquals("test-flag", state.getKey());
        assertEquals(true, state.getValue());
        assertTrue(state.isEnabled());
        assertEquals(10, state.getVersion());
    }

    @Test
    void testDefaultValues() {
        FlagState state = FlagState.builder()
                .key("test-flag")
                .value("default")
                .build();

        assertTrue(state.isEnabled()); // default is true
        assertEquals(0, state.getVersion());
    }

    @Test
    void testWithBooleanValue() {
        FlagState state = FlagState.builder()
                .key("bool-flag")
                .value(true)
                .enabled(true)
                .build();

        assertTrue(state.getBooleanValue());
    }

    @Test
    void testWithStringValue() {
        FlagState state = FlagState.builder()
                .key("string-flag")
                .value("hello")
                .enabled(true)
                .build();

        assertEquals("hello", state.getStringValue());
    }

    @Test
    void testWithNumberValue() {
        FlagState state = FlagState.builder()
                .key("number-flag")
                .value(3.14)
                .enabled(true)
                .build();

        assertEquals(3.14, state.getNumberValue(), 0.001);
    }

    @Test
    void testWithIntValue() {
        FlagState state = FlagState.builder()
                .key("int-flag")
                .value(42)
                .enabled(true)
                .build();

        assertEquals(42, state.getIntValue());
    }

    @Test
    void testWithJsonValue() {
        Map<String, Object> json = new HashMap<>();
        json.put("key", "value");
        json.put("count", 10);

        FlagState state = FlagState.builder()
                .key("json-flag")
                .value(json)
                .enabled(true)
                .build();

        assertEquals(json, state.getJsonValue());
    }

    @Test
    void testWithMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("owner", "team-a");
        metadata.put("environment", "production");

        FlagState state = FlagState.builder()
                .key("test-flag")
                .value(true)
                .metadata(metadata)
                .build();

        assertNotNull(state.getMetadata());
        assertEquals("team-a", state.getMetadata().get("owner"));
        assertEquals("production", state.getMetadata().get("environment"));
    }

    @Test
    void testEquality() {
        FlagState state1 = FlagState.builder()
                .key("test-flag")
                .value(true)
                .enabled(true)
                .version(1)
                .build();

        FlagState state2 = FlagState.builder()
                .key("test-flag")
                .value(true)
                .enabled(true)
                .version(1)
                .build();

        assertEquals(state1, state2);
        assertEquals(state1.hashCode(), state2.hashCode());
    }

    @Test
    void testToString() {
        FlagState state = FlagState.builder()
                .key("test-flag")
                .value(true)
                .enabled(true)
                .version(5)
                .build();

        String str = state.toString();
        assertTrue(str.contains("test-flag"));
        assertTrue(str.contains("enabled=true"));
    }
}
