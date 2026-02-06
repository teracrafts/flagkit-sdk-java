package dev.flagkit.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Semantic version comparison utilities for SDK version metadata handling.
 *
 * These utilities are used to compare the current SDK version against
 * server-provided version requirements (min, recommended, latest).
 */
public final class VersionUtils {

    private static final Pattern SEMVER_PATTERN = Pattern.compile("^[vV]?(\\d+)\\.(\\d+)\\.(\\d+)");

    /** Maximum allowed value for version components (defensive limit). */
    private static final int MAX_VERSION_COMPONENT = 999_999_999;

    private VersionUtils() {
        // Utility class, no instantiation
    }

    /**
     * Parsed semantic version representation.
     */
    public static class ParsedVersion {
        private final int major;
        private final int minor;
        private final int patch;

        public ParsedVersion(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        public int getPatch() {
            return patch;
        }
    }

    /**
     * Parse a semantic version string into numeric components.
     * Returns null if the version is not a valid semver.
     *
     * @param version the version string to parse (e.g., "1.2.3", "v1.2.3", "1.2.3-beta")
     * @return the parsed version or null if invalid
     */
    public static ParsedVersion parseVersion(String version) {
        if (version == null || version.isBlank()) {
            return null;
        }

        // Trim whitespace
        String trimmed = version.trim();

        Matcher matcher = SEMVER_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            return null;
        }

        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = Integer.parseInt(matcher.group(3));

            // Validate components are within reasonable bounds
            if (major < 0 || major > MAX_VERSION_COMPONENT ||
                minor < 0 || minor > MAX_VERSION_COMPONENT ||
                patch < 0 || patch > MAX_VERSION_COMPONENT) {
                return null;
            }

            return new ParsedVersion(major, minor, patch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Compare two semantic versions.
     *
     * @param a first version string
     * @param b second version string
     * @return negative number if a < b, 0 if a == b, positive number if a > b.
     *         Returns 0 if either version is invalid.
     */
    public static int compareVersions(String a, String b) {
        ParsedVersion parsedA = parseVersion(a);
        ParsedVersion parsedB = parseVersion(b);

        if (parsedA == null || parsedB == null) {
            return 0;
        }

        // Compare major
        if (parsedA.major != parsedB.major) {
            return parsedA.major - parsedB.major;
        }

        // Compare minor
        if (parsedA.minor != parsedB.minor) {
            return parsedA.minor - parsedB.minor;
        }

        // Compare patch
        return parsedA.patch - parsedB.patch;
    }

    /**
     * Check if version a is less than version b.
     *
     * @param a first version string
     * @param b second version string
     * @return true if a < b
     */
    public static boolean isVersionLessThan(String a, String b) {
        return compareVersions(a, b) < 0;
    }

    /**
     * Check if version a is greater than or equal to version b.
     *
     * @param a first version string
     * @param b second version string
     * @return true if a >= b
     */
    public static boolean isVersionAtLeast(String a, String b) {
        return compareVersions(a, b) >= 0;
    }
}
