package dev.flagkit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorCodeTest {

    @Test
    void testRecoverableErrors() {
        assertTrue(ErrorCode.NETWORK_ERROR.isRecoverable());
        assertTrue(ErrorCode.NETWORK_TIMEOUT.isRecoverable());
        assertTrue(ErrorCode.NETWORK_RETRY_LIMIT.isRecoverable());
        assertTrue(ErrorCode.CIRCUIT_OPEN.isRecoverable());
        assertTrue(ErrorCode.CACHE_EXPIRED.isRecoverable());
    }

    @Test
    void testNonRecoverableErrors() {
        assertFalse(ErrorCode.AUTH_INVALID_KEY.isRecoverable());
        assertFalse(ErrorCode.AUTH_EXPIRED_KEY.isRecoverable());
        assertFalse(ErrorCode.INIT_FAILED.isRecoverable());
        assertFalse(ErrorCode.EVAL_INVALID_KEY.isRecoverable());
        assertFalse(ErrorCode.CONFIG_MISSING_REQUIRED.isRecoverable());
    }

    @Test
    void testErrorCodeValues() {
        assertEquals("AUTH_INVALID_KEY", ErrorCode.AUTH_INVALID_KEY.getCode());
        assertEquals("NETWORK_ERROR", ErrorCode.NETWORK_ERROR.getCode());
        assertEquals("INIT_FAILED", ErrorCode.INIT_FAILED.getCode());
    }

    @Test
    void testToString() {
        assertEquals("AUTH_INVALID_KEY", ErrorCode.AUTH_INVALID_KEY.toString());
    }
}
