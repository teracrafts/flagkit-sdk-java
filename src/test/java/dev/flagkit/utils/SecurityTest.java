package dev.flagkit.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityTest {

    @Mock
    private Logger mockLogger;

    @Nested
    class IsPotentialPIIFieldTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "email",
                "userEmail",
                "email_address",
                "EMAIL",
                "user_email"
        })
        void shouldDetectEmailPatterns(String fieldName) {
            assertTrue(Security.isPotentialPIIField(fieldName),
                    "Should detect email pattern in: " + fieldName);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "phone",
                "phoneNumber",
                "phone_number",
                "telephone",
                "mobile",
                "mobilePhone"
        })
        void shouldDetectPhonePatterns(String fieldName) {
            assertTrue(Security.isPotentialPIIField(fieldName),
                    "Should detect phone pattern in: " + fieldName);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "ssn",
                "social_security",
                "socialSecurity",
                "socialSecurityNumber",
                "SSN"
        })
        void shouldDetectSSNPatterns(String fieldName) {
            assertTrue(Security.isPotentialPIIField(fieldName),
                    "Should detect SSN pattern in: " + fieldName);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "credit_card",
                "creditCard",
                "card_number",
                "cardNumber",
                "cvv",
                "CVV"
        })
        void shouldDetectCreditCardPatterns(String fieldName) {
            assertTrue(Security.isPotentialPIIField(fieldName),
                    "Should detect credit card pattern in: " + fieldName);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "password",
                "passwd",
                "userPassword",
                "PASSWORD"
        })
        void shouldDetectPasswordPatterns(String fieldName) {
            assertTrue(Security.isPotentialPIIField(fieldName),
                    "Should detect password pattern in: " + fieldName);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "token",
                "secret",
                "api_key",
                "apiKey",
                "private_key",
                "privateKey",
                "access_token",
                "accessToken",
                "refresh_token",
                "refreshToken",
                "auth_token",
                "authToken"
        })
        void shouldDetectSecretPatterns(String fieldName) {
            assertTrue(Security.isPotentialPIIField(fieldName),
                    "Should detect secret pattern in: " + fieldName);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "address",
                "street",
                "streetAddress",
                "zip_code",
                "zipCode",
                "postal_code",
                "postalCode"
        })
        void shouldDetectAddressPatterns(String fieldName) {
            assertTrue(Security.isPotentialPIIField(fieldName),
                    "Should detect address pattern in: " + fieldName);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "date_of_birth",
                "dateOfBirth",
                "dob",
                "birth_date",
                "birthDate"
        })
        void shouldDetectBirthDatePatterns(String fieldName) {
            assertTrue(Security.isPotentialPIIField(fieldName),
                    "Should detect birth date pattern in: " + fieldName);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "passport",
                "passportNumber",
                "driver_license",
                "driverLicense",
                "national_id",
                "nationalId"
        })
        void shouldDetectIdDocumentPatterns(String fieldName) {
            assertTrue(Security.isPotentialPIIField(fieldName),
                    "Should detect ID document pattern in: " + fieldName);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "bank_account",
                "bankAccount",
                "routing_number",
                "routingNumber",
                "iban",
                "swift",
                "IBAN",
                "SWIFT"
        })
        void shouldDetectBankingPatterns(String fieldName) {
            assertTrue(Security.isPotentialPIIField(fieldName),
                    "Should detect banking pattern in: " + fieldName);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "username",
                "userId",
                "user_id",
                "name",
                "firstName",
                "lastName",
                "plan",
                "subscription",
                "count",
                "total",
                "enabled",
                "flag"
        })
        void shouldNotDetectNonPIIFields(String fieldName) {
            assertFalse(Security.isPotentialPIIField(fieldName),
                    "Should not detect PII in: " + fieldName);
        }

        @Test
        void shouldReturnFalseForNullFieldName() {
            assertFalse(Security.isPotentialPIIField(null));
        }

        @Test
        void shouldReturnFalseForEmptyFieldName() {
            assertFalse(Security.isPotentialPIIField(""));
        }
    }

    @Nested
    class DetectPotentialPIITest {

        @Test
        void shouldDetectPIIInFlatMap() {
            Map<String, Object> data = new HashMap<>();
            data.put("email", "test@example.com");
            data.put("name", "John");
            data.put("phone", "123-456-7890");

            List<String> piiFields = Security.detectPotentialPII(data);

            assertEquals(2, piiFields.size());
            assertTrue(piiFields.contains("email"));
            assertTrue(piiFields.contains("phone"));
        }

        @Test
        void shouldDetectPIIInNestedMap() {
            Map<String, Object> nested = new HashMap<>();
            nested.put("email", "test@example.com");
            nested.put("street", "123 Main St");

            Map<String, Object> data = new HashMap<>();
            data.put("user", nested);
            data.put("plan", "premium");

            List<String> piiFields = Security.detectPotentialPII(data);

            assertEquals(2, piiFields.size());
            assertTrue(piiFields.contains("user.email"));
            assertTrue(piiFields.contains("user.street"));
        }

        @Test
        void shouldDetectPIIInDeeplyNestedMap() {
            Map<String, Object> level3 = new HashMap<>();
            level3.put("ssn", "123-45-6789");

            Map<String, Object> level2 = new HashMap<>();
            level2.put("personal", level3);

            Map<String, Object> level1 = new HashMap<>();
            level1.put("info", level2);

            Map<String, Object> data = new HashMap<>();
            data.put("user", level1);

            List<String> piiFields = Security.detectPotentialPII(data);

            assertEquals(1, piiFields.size());
            assertTrue(piiFields.contains("user.info.personal.ssn"));
        }

        @Test
        void shouldReturnEmptyListForMapWithoutPII() {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", "user-123");
            data.put("plan", "premium");
            data.put("count", 42);

            List<String> piiFields = Security.detectPotentialPII(data);

            assertTrue(piiFields.isEmpty());
        }

        @Test
        void shouldReturnEmptyListForNullMap() {
            List<String> piiFields = Security.detectPotentialPII(null);
            assertTrue(piiFields.isEmpty());
        }

        @Test
        void shouldReturnEmptyListForEmptyMap() {
            List<String> piiFields = Security.detectPotentialPII(new HashMap<>());
            assertTrue(piiFields.isEmpty());
        }

        @Test
        void shouldUseCustomPrefixForNestedPaths() {
            Map<String, Object> data = new HashMap<>();
            data.put("email", "test@example.com");

            List<String> piiFields = Security.detectPotentialPII(data, "context.attributes");

            assertEquals(1, piiFields.size());
            assertTrue(piiFields.contains("context.attributes.email"));
        }

        @Test
        void shouldHandleEmptyPrefix() {
            Map<String, Object> data = new HashMap<>();
            data.put("password", "secret123");

            List<String> piiFields = Security.detectPotentialPII(data, "");

            assertEquals(1, piiFields.size());
            assertEquals("password", piiFields.get(0));
        }

        @Test
        void shouldHandleNullPrefix() {
            Map<String, Object> data = new HashMap<>();
            data.put("token", "abc123");

            List<String> piiFields = Security.detectPotentialPII(data, null);

            assertEquals(1, piiFields.size());
            assertEquals("token", piiFields.get(0));
        }

        @Test
        void shouldHandleMixedNestedContent() {
            Map<String, Object> data = new HashMap<>();
            data.put("creditCard", "4111-1111-1111-1111");
            data.put("items", Arrays.asList("item1", "item2")); // Arrays are skipped
            data.put("count", 5);

            List<String> piiFields = Security.detectPotentialPII(data);

            assertEquals(1, piiFields.size());
            assertTrue(piiFields.contains("creditCard"));
        }
    }

    @Nested
    class WarnIfPotentialPIITest {

        @Test
        void shouldLogWarningWhenPIIDetectedInContext() {
            Map<String, Object> data = new HashMap<>();
            data.put("email", "test@example.com");
            data.put("phone", "123-456-7890");

            Security.warnIfPotentialPII(data, "context", mockLogger);

            verify(mockLogger).warn(
                    eq("[FlagKit Security] Potential PII detected in {} data: {}. {}"),
                    eq("context"),
                    contains("email"),
                    eq("Consider adding these to privateAttributes.")
            );
        }

        @Test
        void shouldLogWarningWhenPIIDetectedInEvent() {
            Map<String, Object> data = new HashMap<>();
            data.put("password", "secret123");

            Security.warnIfPotentialPII(data, "event", mockLogger);

            verify(mockLogger).warn(
                    eq("[FlagKit Security] Potential PII detected in {} data: {}. {}"),
                    eq("event"),
                    eq("password"),
                    eq("Consider removing sensitive data from events.")
            );
        }

        @Test
        void shouldNotLogWhenNoPIIDetected() {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", "user-123");
            data.put("plan", "premium");

            Security.warnIfPotentialPII(data, "context", mockLogger);

            verify(mockLogger, never()).warn(anyString());
            verify(mockLogger, never()).warn(anyString(), any(), any(), any());
        }

        @Test
        void shouldNotLogWhenDataIsNull() {
            Security.warnIfPotentialPII(null, "context", mockLogger);

            verify(mockLogger, never()).warn(anyString());
            verify(mockLogger, never()).warn(anyString(), any(), any(), any());
        }

        @Test
        void shouldNotLogWhenLoggerIsNull() {
            Map<String, Object> data = new HashMap<>();
            data.put("email", "test@example.com");

            // Should not throw
            assertDoesNotThrow(() -> Security.warnIfPotentialPII(data, "context", null));
        }
    }

    @Nested
    class IsServerKeyTest {

        @Test
        void shouldReturnTrueForServerKey() {
            assertTrue(Security.isServerKey("srv_abc123"));
            assertTrue(Security.isServerKey("srv_"));
            assertTrue(Security.isServerKey("srv_longer_key_value"));
        }

        @Test
        void shouldReturnFalseForNonServerKeys() {
            assertFalse(Security.isServerKey("sdk_abc123"));
            assertFalse(Security.isServerKey("cli_abc123"));
            assertFalse(Security.isServerKey("abc_srv_123"));
            assertFalse(Security.isServerKey("SRV_abc123")); // Case sensitive
        }

        @Test
        void shouldReturnFalseForNullKey() {
            assertFalse(Security.isServerKey(null));
        }

        @Test
        void shouldReturnFalseForEmptyKey() {
            assertFalse(Security.isServerKey(""));
        }
    }

    @Nested
    class IsClientKeyTest {

        @Test
        void shouldReturnTrueForSDKKey() {
            assertTrue(Security.isClientKey("sdk_abc123"));
            assertTrue(Security.isClientKey("sdk_"));
            assertTrue(Security.isClientKey("sdk_longer_key_value"));
        }

        @Test
        void shouldReturnTrueForCLIKey() {
            assertTrue(Security.isClientKey("cli_abc123"));
            assertTrue(Security.isClientKey("cli_"));
            assertTrue(Security.isClientKey("cli_longer_key_value"));
        }

        @Test
        void shouldReturnFalseForServerKey() {
            assertFalse(Security.isClientKey("srv_abc123"));
        }

        @Test
        void shouldReturnFalseForInvalidKeys() {
            assertFalse(Security.isClientKey("abc_sdk_123"));
            assertFalse(Security.isClientKey("SDK_abc123")); // Case sensitive
            assertFalse(Security.isClientKey("CLI_abc123")); // Case sensitive
        }

        @Test
        void shouldReturnFalseForNullKey() {
            assertFalse(Security.isClientKey(null));
        }

        @Test
        void shouldReturnFalseForEmptyKey() {
            assertFalse(Security.isClientKey(""));
        }
    }

    @Nested
    class IsBrowserLikeEnvironmentTest {

        @Test
        void shouldReturnFalseInStandardJavaEnvironment() {
            // In a standard Java test environment, this should return false
            // since we don't have Android or JavaFX WebView
            assertFalse(Security.isBrowserLikeEnvironment());
        }
    }

    @Nested
    class WarnIfServerKeyInBrowserTest {

        @Test
        void shouldNotWarnForClientKeyEvenInBrowserEnvironment() {
            // Since we're in standard Java environment (not browser-like),
            // no warning should be issued regardless of key type
            Security.warnIfServerKeyInBrowser("sdk_abc123", mockLogger);
            Security.warnIfServerKeyInBrowser("cli_abc123", mockLogger);

            verify(mockLogger, never()).warn(anyString());
        }

        @Test
        void shouldNotWarnForServerKeyInNonBrowserEnvironment() {
            // In standard Java environment (not browser-like),
            // no warning should be issued even for server keys
            Security.warnIfServerKeyInBrowser("srv_abc123", mockLogger);

            verify(mockLogger, never()).warn(anyString());
        }

        @Test
        void shouldHandleNullLogger() {
            // Should not throw even with null logger
            assertDoesNotThrow(() -> Security.warnIfServerKeyInBrowser("srv_abc123", null));
        }

        @Test
        void shouldHandleNullApiKey() {
            // Should not throw or warn with null API key
            assertDoesNotThrow(() -> Security.warnIfServerKeyInBrowser(null, mockLogger));
            verify(mockLogger, never()).warn(anyString());
        }
    }

    @Nested
    class SecurityConfigTest {

        @Test
        void shouldCreateDefaultConfig() {
            Security.SecurityConfig config = Security.SecurityConfig.defaults();

            assertNotNull(config);
            assertTrue(config.isWarnOnServerKeyInBrowser());
            assertNotNull(config.getAdditionalPIIPatterns());
            assertTrue(config.getAdditionalPIIPatterns().isEmpty());
        }

        @Test
        void shouldBuildConfigWithCustomSettings() {
            Security.SecurityConfig config = Security.SecurityConfig.builder()
                    .warnOnPotentialPII(true)
                    .warnOnServerKeyInBrowser(false)
                    .additionalPIIPatterns(Arrays.asList("custom_field", "another_field"))
                    .build();

            assertTrue(config.isWarnOnPotentialPII());
            assertFalse(config.isWarnOnServerKeyInBrowser());
            assertEquals(2, config.getAdditionalPIIPatterns().size());
            assertTrue(config.getAdditionalPIIPatterns().contains("custom_field"));
            assertTrue(config.getAdditionalPIIPatterns().contains("another_field"));
        }

        @Test
        void shouldAddIndividualPIIPatterns() {
            Security.SecurityConfig config = Security.SecurityConfig.builder()
                    .addPIIPattern("custom1")
                    .addPIIPattern("custom2")
                    .addPIIPattern(null) // Should be ignored
                    .build();

            assertEquals(2, config.getAdditionalPIIPatterns().size());
            assertTrue(config.getAdditionalPIIPatterns().contains("custom1"));
            assertTrue(config.getAdditionalPIIPatterns().contains("custom2"));
        }

        @Test
        void shouldReturnDefensiveCopyOfAdditionalPatterns() {
            List<String> patterns = new ArrayList<>(Arrays.asList("pattern1", "pattern2"));
            Security.SecurityConfig config = Security.SecurityConfig.builder()
                    .additionalPIIPatterns(patterns)
                    .build();

            // Modify the original list
            patterns.add("pattern3");

            // Config should not be affected
            assertEquals(2, config.getAdditionalPIIPatterns().size());

            // Modify the returned list
            List<String> returnedPatterns = config.getAdditionalPIIPatterns();
            returnedPatterns.add("pattern4");

            // Config should still not be affected
            assertEquals(2, config.getAdditionalPIIPatterns().size());
        }

        @Test
        void shouldHandleNullAdditionalPatterns() {
            Security.SecurityConfig config = Security.SecurityConfig.builder()
                    .additionalPIIPatterns(null)
                    .build();

            assertNotNull(config.getAdditionalPIIPatterns());
            assertTrue(config.getAdditionalPIIPatterns().isEmpty());
        }

        @Test
        void shouldDisablePIIWarnings() {
            Security.SecurityConfig config = Security.SecurityConfig.builder()
                    .warnOnPotentialPII(false)
                    .build();

            assertFalse(config.isWarnOnPotentialPII());
        }

        @Test
        void shouldDisableServerKeyWarnings() {
            Security.SecurityConfig config = Security.SecurityConfig.builder()
                    .warnOnServerKeyInBrowser(false)
                    .build();

            assertFalse(config.isWarnOnServerKeyInBrowser());
        }
    }
}
