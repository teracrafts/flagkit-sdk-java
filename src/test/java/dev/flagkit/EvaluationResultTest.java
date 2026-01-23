package dev.flagkit;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationResultTest {

    @Test
    void testBuilderCreatesResult() {
        EvaluationResult result = EvaluationResult.builder()
                .flagKey("test-flag")
                .value(true)
                .enabled(true)
                .reason(EvaluationReason.CACHED)
                .version(5)
                .build();

        assertEquals("test-flag", result.getFlagKey());
        assertEquals(true, result.getValue());
        assertTrue(result.isEnabled());
        assertEquals(EvaluationReason.CACHED, result.getReason());
        assertEquals(5, result.getVersion());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testDefaultResult() {
        EvaluationResult result = EvaluationResult.defaultResult(
                "missing-flag",
                false,
                EvaluationReason.FLAG_NOT_FOUND
        );

        assertEquals("missing-flag", result.getFlagKey());
        assertEquals(false, result.getValue());
        assertFalse(result.isEnabled());
        assertEquals(EvaluationReason.FLAG_NOT_FOUND, result.getReason());
    }

    @Test
    void testGetBooleanValue() {
        EvaluationResult trueResult = EvaluationResult.builder()
                .flagKey("bool-flag")
                .value(true)
                .build();
        assertTrue(trueResult.getBooleanValue());

        EvaluationResult falseResult = EvaluationResult.builder()
                .flagKey("bool-flag")
                .value(false)
                .build();
        assertFalse(falseResult.getBooleanValue());

        EvaluationResult nonBoolResult = EvaluationResult.builder()
                .flagKey("string-flag")
                .value("not a bool")
                .build();
        assertFalse(nonBoolResult.getBooleanValue());
    }

    @Test
    void testGetStringValue() {
        EvaluationResult stringResult = EvaluationResult.builder()
                .flagKey("string-flag")
                .value("hello")
                .build();
        assertEquals("hello", stringResult.getStringValue());

        EvaluationResult numberResult = EvaluationResult.builder()
                .flagKey("number-flag")
                .value(42)
                .build();
        assertEquals("42", numberResult.getStringValue());

        EvaluationResult nullResult = EvaluationResult.builder()
                .flagKey("null-flag")
                .value(null)
                .build();
        assertNull(nullResult.getStringValue());
    }

    @Test
    void testGetNumberValue() {
        EvaluationResult intResult = EvaluationResult.builder()
                .flagKey("int-flag")
                .value(42)
                .build();
        assertEquals(42.0, intResult.getNumberValue(), 0.001);

        EvaluationResult doubleResult = EvaluationResult.builder()
                .flagKey("double-flag")
                .value(3.14)
                .build();
        assertEquals(3.14, doubleResult.getNumberValue(), 0.001);

        EvaluationResult nonNumberResult = EvaluationResult.builder()
                .flagKey("string-flag")
                .value("not a number")
                .build();
        assertEquals(0.0, nonNumberResult.getNumberValue(), 0.001);
    }

    @Test
    void testGetIntValue() {
        EvaluationResult intResult = EvaluationResult.builder()
                .flagKey("int-flag")
                .value(42)
                .build();
        assertEquals(42, intResult.getIntValue());

        EvaluationResult doubleResult = EvaluationResult.builder()
                .flagKey("double-flag")
                .value(3.9)
                .build();
        assertEquals(3, doubleResult.getIntValue());

        EvaluationResult nonNumberResult = EvaluationResult.builder()
                .flagKey("string-flag")
                .value("not a number")
                .build();
        assertEquals(0, nonNumberResult.getIntValue());
    }

    @Test
    void testGetJsonValue() {
        Map<String, Object> json = new HashMap<>();
        json.put("key", "value");
        json.put("count", 10);

        EvaluationResult jsonResult = EvaluationResult.builder()
                .flagKey("json-flag")
                .value(json)
                .build();
        assertEquals(json, jsonResult.getJsonValue());

        EvaluationResult nonJsonResult = EvaluationResult.builder()
                .flagKey("string-flag")
                .value("not json")
                .build();
        assertNull(nonJsonResult.getJsonValue());
    }

    @Test
    void testEquality() {
        EvaluationResult result1 = EvaluationResult.builder()
                .flagKey("test-flag")
                .value(true)
                .enabled(true)
                .reason(EvaluationReason.CACHED)
                .version(1)
                .build();

        EvaluationResult result2 = EvaluationResult.builder()
                .flagKey("test-flag")
                .value(true)
                .enabled(true)
                .reason(EvaluationReason.CACHED)
                .version(1)
                .build();

        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void testToString() {
        EvaluationResult result = EvaluationResult.builder()
                .flagKey("test-flag")
                .value(true)
                .enabled(true)
                .reason(EvaluationReason.CACHED)
                .version(1)
                .build();

        String str = result.toString();
        assertTrue(str.contains("test-flag"));
        assertTrue(str.contains("CACHED"));
    }
}
