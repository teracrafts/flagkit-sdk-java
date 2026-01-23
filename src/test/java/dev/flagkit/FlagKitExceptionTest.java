package dev.flagkit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlagKitExceptionTest {

    @Test
    void testConfigError() {
        FlagKitException exception = FlagKitException.configError(
                ErrorCode.CONFIG_INVALID_API_KEY,
                "Invalid API key"
        );

        assertEquals(ErrorCode.CONFIG_INVALID_API_KEY, exception.getCode());
        assertTrue(exception.getMessage().contains("Invalid API key"));
        assertTrue(exception.isConfigError());
        assertFalse(exception.isNetworkError());
        assertFalse(exception.isEvaluationError());
    }

    @Test
    void testNetworkError() {
        FlagKitException exception = FlagKitException.networkError(
                ErrorCode.HTTP_TIMEOUT,
                "Request timed out"
        );

        assertEquals(ErrorCode.HTTP_TIMEOUT, exception.getCode());
        assertTrue(exception.getMessage().contains("Request timed out"));
        assertTrue(exception.isNetworkError());
        assertFalse(exception.isConfigError());
        assertFalse(exception.isEvaluationError());
    }

    @Test
    void testNetworkErrorWithCause() {
        Exception cause = new RuntimeException("Connection failed");
        FlagKitException exception = FlagKitException.networkError(
                ErrorCode.HTTP_NETWORK_ERROR,
                "Network error",
                cause
        );

        assertEquals(ErrorCode.HTTP_NETWORK_ERROR, exception.getCode());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testEvaluationError() {
        FlagKitException exception = FlagKitException.evaluationError(
                ErrorCode.EVAL_FLAG_NOT_FOUND,
                "Flag not found"
        );

        assertEquals(ErrorCode.EVAL_FLAG_NOT_FOUND, exception.getCode());
        assertTrue(exception.getMessage().contains("Flag not found"));
        assertTrue(exception.isEvaluationError());
        assertFalse(exception.isConfigError());
        assertFalse(exception.isNetworkError());
    }

    @Test
    void testSdkError() {
        FlagKitException exception = FlagKitException.sdkError(
                ErrorCode.SDK_NOT_INITIALIZED,
                "SDK not initialized"
        );

        assertEquals(ErrorCode.SDK_NOT_INITIALIZED, exception.getCode());
        assertTrue(exception.getMessage().contains("SDK not initialized"));
        assertTrue(exception.isSdkError());
    }

    @Test
    void testNotInitialized() {
        FlagKitException exception = FlagKitException.notInitialized();

        assertEquals(ErrorCode.SDK_NOT_INITIALIZED, exception.getCode());
        assertTrue(exception.isSdkError());
    }

    @Test
    void testAlreadyInitialized() {
        FlagKitException exception = FlagKitException.alreadyInitialized();

        assertEquals(ErrorCode.SDK_ALREADY_INITIALIZED, exception.getCode());
        assertTrue(exception.isSdkError());
    }

    @Test
    void testIsRecoverable() {
        FlagKitException recoverableException = FlagKitException.networkError(
                ErrorCode.HTTP_TIMEOUT,
                "Timeout"
        );
        assertTrue(recoverableException.isRecoverable());

        FlagKitException nonRecoverableException = FlagKitException.configError(
                ErrorCode.CONFIG_INVALID_API_KEY,
                "Invalid key"
        );
        assertFalse(nonRecoverableException.isRecoverable());
    }

    @Test
    void testToString() {
        FlagKitException exception = FlagKitException.configError(
                ErrorCode.CONFIG_INVALID_API_KEY,
                "Test message"
        );

        String str = exception.toString();
        assertTrue(str.contains("CONFIG_INVALID_API_KEY"));
        assertTrue(str.contains("Test message"));
    }
}
