package dev.flagkit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvaluationReasonTest {

    @Test
    void testAllReasonsExist() {
        assertNotNull(EvaluationReason.CACHED);
        assertNotNull(EvaluationReason.DEFAULT);
        assertNotNull(EvaluationReason.FLAG_NOT_FOUND);
        assertNotNull(EvaluationReason.BOOTSTRAP);
        assertNotNull(EvaluationReason.SERVER);
        assertNotNull(EvaluationReason.STALE_CACHE);
        assertNotNull(EvaluationReason.ERROR);
        assertNotNull(EvaluationReason.DISABLED);
        assertNotNull(EvaluationReason.TYPE_MISMATCH);
        assertNotNull(EvaluationReason.OFFLINE);
    }

    @Test
    void testFromStringParsesKnownReasons() {
        assertEquals(EvaluationReason.CACHED, EvaluationReason.fromString("CACHED"));
        assertEquals(EvaluationReason.DEFAULT, EvaluationReason.fromString("DEFAULT"));
        assertEquals(EvaluationReason.FLAG_NOT_FOUND, EvaluationReason.fromString("FLAG_NOT_FOUND"));
        assertEquals(EvaluationReason.BOOTSTRAP, EvaluationReason.fromString("BOOTSTRAP"));
        assertEquals(EvaluationReason.SERVER, EvaluationReason.fromString("SERVER"));
        assertEquals(EvaluationReason.STALE_CACHE, EvaluationReason.fromString("STALE_CACHE"));
        assertEquals(EvaluationReason.ERROR, EvaluationReason.fromString("ERROR"));
        assertEquals(EvaluationReason.DISABLED, EvaluationReason.fromString("DISABLED"));
        assertEquals(EvaluationReason.TYPE_MISMATCH, EvaluationReason.fromString("TYPE_MISMATCH"));
        assertEquals(EvaluationReason.OFFLINE, EvaluationReason.fromString("OFFLINE"));
    }

    @Test
    void testFromStringHandlesLowerCase() {
        assertEquals(EvaluationReason.CACHED, EvaluationReason.fromString("cached"));
        assertEquals(EvaluationReason.DEFAULT, EvaluationReason.fromString("default"));
        assertEquals(EvaluationReason.FLAG_NOT_FOUND, EvaluationReason.fromString("flag_not_found"));
    }

    @Test
    void testFromStringReturnsDefaultForUnknown() {
        assertEquals(EvaluationReason.DEFAULT, EvaluationReason.fromString("invalid"));
        assertEquals(EvaluationReason.DEFAULT, EvaluationReason.fromString(""));
    }

    @Test
    void testFromStringHandlesNull() {
        assertEquals(EvaluationReason.DEFAULT, EvaluationReason.fromString(null));
    }

    @Test
    void testGetValue() {
        assertEquals("CACHED", EvaluationReason.CACHED.getValue());
        assertEquals("DEFAULT", EvaluationReason.DEFAULT.getValue());
        assertEquals("FLAG_NOT_FOUND", EvaluationReason.FLAG_NOT_FOUND.getValue());
    }

    @Test
    void testToString() {
        assertEquals("CACHED", EvaluationReason.CACHED.toString());
        assertEquals("DEFAULT", EvaluationReason.DEFAULT.toString());
        assertEquals("FLAG_NOT_FOUND", EvaluationReason.FLAG_NOT_FOUND.toString());
    }
}
