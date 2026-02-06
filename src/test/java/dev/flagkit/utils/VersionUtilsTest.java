package dev.flagkit.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class VersionUtilsTest {

    @Nested
    class ParseVersionTest {

        @Test
        void shouldParseStandardSemver() {
            VersionUtils.ParsedVersion version = VersionUtils.parseVersion("1.2.3");

            assertNotNull(version);
            assertEquals(1, version.getMajor());
            assertEquals(2, version.getMinor());
            assertEquals(3, version.getPatch());
        }

        @Test
        void shouldParseVersionWithVPrefix() {
            VersionUtils.ParsedVersion version = VersionUtils.parseVersion("v1.2.3");

            assertNotNull(version);
            assertEquals(1, version.getMajor());
            assertEquals(2, version.getMinor());
            assertEquals(3, version.getPatch());
        }

        @Test
        void shouldParseVersionWithPrereleaseSuffix() {
            VersionUtils.ParsedVersion version = VersionUtils.parseVersion("1.2.3-beta.1");

            assertNotNull(version);
            assertEquals(1, version.getMajor());
            assertEquals(2, version.getMinor());
            assertEquals(3, version.getPatch());
        }

        @Test
        void shouldParseVersionWithBuildMetadata() {
            VersionUtils.ParsedVersion version = VersionUtils.parseVersion("1.2.3+build.456");

            assertNotNull(version);
            assertEquals(1, version.getMajor());
            assertEquals(2, version.getMinor());
            assertEquals(3, version.getPatch());
        }

        @Test
        void shouldReturnNullForNullInput() {
            assertNull(VersionUtils.parseVersion(null));
        }

        @Test
        void shouldReturnNullForEmptyString() {
            assertNull(VersionUtils.parseVersion(""));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "invalid",
                "1.2",
                "1",
                "abc.def.ghi",
                "1.2.x",
                "latest"
        })
        void shouldReturnNullForInvalidVersions(String version) {
            assertNull(VersionUtils.parseVersion(version));
        }

        @Test
        void shouldHandleLargeVersionNumbers() {
            VersionUtils.ParsedVersion version = VersionUtils.parseVersion("100.200.300");

            assertNotNull(version);
            assertEquals(100, version.getMajor());
            assertEquals(200, version.getMinor());
            assertEquals(300, version.getPatch());
        }

        @Test
        void shouldHandleZeroVersionNumbers() {
            VersionUtils.ParsedVersion version = VersionUtils.parseVersion("0.0.0");

            assertNotNull(version);
            assertEquals(0, version.getMajor());
            assertEquals(0, version.getMinor());
            assertEquals(0, version.getPatch());
        }

        @Test
        void shouldParseVersionWithUppercaseVPrefix() {
            VersionUtils.ParsedVersion version = VersionUtils.parseVersion("V1.2.3");

            assertNotNull(version);
            assertEquals(1, version.getMajor());
            assertEquals(2, version.getMinor());
            assertEquals(3, version.getPatch());
        }

        @Test
        void shouldParseVersionWithLeadingWhitespace() {
            VersionUtils.ParsedVersion version = VersionUtils.parseVersion("  1.2.3");

            assertNotNull(version);
            assertEquals(1, version.getMajor());
            assertEquals(2, version.getMinor());
            assertEquals(3, version.getPatch());
        }

        @Test
        void shouldParseVersionWithTrailingWhitespace() {
            VersionUtils.ParsedVersion version = VersionUtils.parseVersion("1.2.3  ");

            assertNotNull(version);
            assertEquals(1, version.getMajor());
            assertEquals(2, version.getMinor());
            assertEquals(3, version.getPatch());
        }

        @Test
        void shouldParseVersionWithSurroundingWhitespace() {
            VersionUtils.ParsedVersion version = VersionUtils.parseVersion("  1.2.3  ");

            assertNotNull(version);
            assertEquals(1, version.getMajor());
            assertEquals(2, version.getMinor());
            assertEquals(3, version.getPatch());
        }

        @Test
        void shouldParseVersionWithVPrefixAndWhitespace() {
            VersionUtils.ParsedVersion version = VersionUtils.parseVersion("  v1.0.0  ");

            assertNotNull(version);
            assertEquals(1, version.getMajor());
        }

        @Test
        void shouldReturnNullForWhitespaceOnly() {
            assertNull(VersionUtils.parseVersion("   "));
        }

        @Test
        void shouldReturnNullForVersionExceedingMax() {
            assertNull(VersionUtils.parseVersion("1000000000.0.0"));
            assertNull(VersionUtils.parseVersion("0.1000000000.0"));
            assertNull(VersionUtils.parseVersion("0.0.1000000000"));
        }

        @Test
        void shouldParseVersionAtMaxBoundary() {
            VersionUtils.ParsedVersion version = VersionUtils.parseVersion("999999999.999999999.999999999");

            assertNotNull(version);
            assertEquals(999999999, version.getMajor());
            assertEquals(999999999, version.getMinor());
            assertEquals(999999999, version.getPatch());
        }
    }

    @Nested
    class CompareVersionsTest {

        @ParameterizedTest
        @CsvSource({
                "1.0.0, 2.0.0",
                "1.0.0, 1.1.0",
                "1.0.0, 1.0.1",
                "0.9.9, 1.0.0",
                "1.0.0, 1.0.1",
                "v1.0.0, 1.0.1"
        })
        void shouldReturnNegativeWhenFirstIsLower(String a, String b) {
            assertTrue(VersionUtils.compareVersions(a, b) < 0,
                    a + " should be less than " + b);
        }

        @ParameterizedTest
        @CsvSource({
                "2.0.0, 1.0.0",
                "1.1.0, 1.0.0",
                "1.0.1, 1.0.0",
                "1.0.0, 0.9.9",
                "1.0.1, 1.0.0",
                "1.0.1, v1.0.0"
        })
        void shouldReturnPositiveWhenFirstIsHigher(String a, String b) {
            assertTrue(VersionUtils.compareVersions(a, b) > 0,
                    a + " should be greater than " + b);
        }

        @ParameterizedTest
        @CsvSource({
                "1.0.0, 1.0.0",
                "v1.0.0, 1.0.0",
                "1.0.0, v1.0.0",
                "0.0.0, 0.0.0",
                "10.20.30, 10.20.30"
        })
        void shouldReturnZeroWhenVersionsAreEqual(String a, String b) {
            assertEquals(0, VersionUtils.compareVersions(a, b),
                    a + " should be equal to " + b);
        }

        @Test
        void shouldReturnZeroWhenFirstIsInvalid() {
            assertEquals(0, VersionUtils.compareVersions("invalid", "1.0.0"));
        }

        @Test
        void shouldReturnZeroWhenSecondIsInvalid() {
            assertEquals(0, VersionUtils.compareVersions("1.0.0", "invalid"));
        }

        @Test
        void shouldReturnZeroWhenBothAreInvalid() {
            assertEquals(0, VersionUtils.compareVersions("invalid", "also-invalid"));
        }

        @Test
        void shouldReturnZeroWhenFirstIsNull() {
            assertEquals(0, VersionUtils.compareVersions(null, "1.0.0"));
        }

        @Test
        void shouldReturnZeroWhenSecondIsNull() {
            assertEquals(0, VersionUtils.compareVersions("1.0.0", null));
        }

        @Test
        void shouldReturnZeroWhenBothAreNull() {
            assertEquals(0, VersionUtils.compareVersions(null, null));
        }
    }

    @Nested
    class IsVersionLessThanTest {

        @ParameterizedTest
        @CsvSource({
                "1.0.0, 2.0.0",
                "1.0.0, 1.1.0",
                "1.0.0, 1.0.1",
                "0.9.9, 1.0.0"
        })
        void shouldReturnTrueWhenFirstIsLower(String a, String b) {
            assertTrue(VersionUtils.isVersionLessThan(a, b),
                    a + " should be less than " + b);
        }

        @ParameterizedTest
        @CsvSource({
                "2.0.0, 1.0.0",
                "1.1.0, 1.0.0",
                "1.0.1, 1.0.0"
        })
        void shouldReturnFalseWhenFirstIsHigher(String a, String b) {
            assertFalse(VersionUtils.isVersionLessThan(a, b),
                    a + " should not be less than " + b);
        }

        @Test
        void shouldReturnFalseWhenVersionsAreEqual() {
            assertFalse(VersionUtils.isVersionLessThan("1.0.0", "1.0.0"));
        }

        @Test
        void shouldReturnFalseWhenEitherIsInvalid() {
            assertFalse(VersionUtils.isVersionLessThan("invalid", "1.0.0"));
            assertFalse(VersionUtils.isVersionLessThan("1.0.0", "invalid"));
        }
    }

    @Nested
    class IsVersionAtLeastTest {

        @ParameterizedTest
        @CsvSource({
                "2.0.0, 1.0.0",
                "1.1.0, 1.0.0",
                "1.0.1, 1.0.0",
                "1.0.0, 0.9.9"
        })
        void shouldReturnTrueWhenFirstIsHigher(String a, String b) {
            assertTrue(VersionUtils.isVersionAtLeast(a, b),
                    a + " should be at least " + b);
        }

        @Test
        void shouldReturnTrueWhenVersionsAreEqual() {
            assertTrue(VersionUtils.isVersionAtLeast("1.0.0", "1.0.0"));
        }

        @ParameterizedTest
        @CsvSource({
                "1.0.0, 2.0.0",
                "1.0.0, 1.1.0",
                "1.0.0, 1.0.1"
        })
        void shouldReturnFalseWhenFirstIsLower(String a, String b) {
            assertFalse(VersionUtils.isVersionAtLeast(a, b),
                    a + " should not be at least " + b);
        }

        @Test
        void shouldReturnTrueWhenEitherIsInvalid() {
            // compareVersions returns 0 for invalid versions, so isVersionAtLeast returns true
            assertTrue(VersionUtils.isVersionAtLeast("invalid", "1.0.0"));
            assertTrue(VersionUtils.isVersionAtLeast("1.0.0", "invalid"));
        }
    }

    @Nested
    class RealWorldScenariosTest {

        @Test
        void shouldCorrectlyCompareSDKVersionScenarios() {
            String currentVersion = "1.0.0";
            String minVersion = "0.9.0";
            String recommendedVersion = "1.1.0";
            String latestVersion = "1.2.0";

            // Current >= min (OK)
            assertTrue(VersionUtils.isVersionAtLeast(currentVersion, minVersion));

            // Current < recommended (should warn)
            assertTrue(VersionUtils.isVersionLessThan(currentVersion, recommendedVersion));

            // Current < latest (newer available)
            assertTrue(VersionUtils.isVersionLessThan(currentVersion, latestVersion));
        }

        @Test
        void shouldHandleUpToDateSDK() {
            String currentVersion = "2.0.0";
            String minVersion = "1.0.0";
            String recommendedVersion = "1.5.0";
            String latestVersion = "2.0.0";

            // Current >= min (OK)
            assertTrue(VersionUtils.isVersionAtLeast(currentVersion, minVersion));

            // Current >= recommended (OK)
            assertFalse(VersionUtils.isVersionLessThan(currentVersion, recommendedVersion));

            // Current == latest (up to date)
            assertFalse(VersionUtils.isVersionLessThan(currentVersion, latestVersion));
        }

        @Test
        void shouldHandleOutdatedSDK() {
            String currentVersion = "0.5.0";
            String minVersion = "1.0.0";

            // Current < min (should error)
            assertTrue(VersionUtils.isVersionLessThan(currentVersion, minVersion));
        }
    }
}
