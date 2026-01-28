package sdklab;

import dev.flagkit.FlagKit;
import dev.flagkit.FlagKitClient;
import dev.flagkit.FlagKitOptions;
import dev.flagkit.types.EvaluationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * FlagKit Java SDK Lab
 *
 * Internal verification script for SDK functionality.
 * Run with: mvn exec:java -Plab
 */
public class Runner {
    private static final String PASS = "\u001B[32m[PASS]\u001B[0m";
    private static final String FAIL = "\u001B[31m[FAIL]\u001B[0m";

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== FlagKit Java SDK Lab ===\n");

        FlagKitClient client = null;
        try {
            // Test 1: Initialization with offline mode + bootstrap
            System.out.println("Testing initialization...");
            Map<String, Object> bootstrap = new HashMap<>();
            bootstrap.put("lab-bool", true);
            bootstrap.put("lab-string", "Hello Lab");
            bootstrap.put("lab-number", 42.0);
            Map<String, Object> jsonValue = new HashMap<>();
            jsonValue.put("nested", true);
            jsonValue.put("count", 100.0);
            bootstrap.put("lab-json", jsonValue);

            FlagKitOptions options = FlagKitOptions.builder("sdk_lab_test_key")
                    .offline(true)
                    .bootstrap(bootstrap)
                    .build();

            client = FlagKit.initialize(options);
            client.waitForReady();

            if (client.isReady()) {
                pass("Initialization");
            } else {
                fail("Initialization - client not ready");
            }

            // Test 2: Boolean flag evaluation
            System.out.println("\nTesting flag evaluation...");
            boolean boolValue = client.getBooleanValue("lab-bool", false);
            if (boolValue) {
                pass("Boolean flag evaluation");
            } else {
                fail("Boolean flag - expected true, got " + boolValue);
            }

            // Test 3: String flag evaluation
            String stringValue = client.getStringValue("lab-string", "");
            if ("Hello Lab".equals(stringValue)) {
                pass("String flag evaluation");
            } else {
                fail("String flag - expected 'Hello Lab', got '" + stringValue + "'");
            }

            // Test 4: Number flag evaluation
            double numberValue = client.getNumberValue("lab-number", 0);
            if (numberValue == 42.0) {
                pass("Number flag evaluation");
            } else {
                fail("Number flag - expected 42, got " + numberValue);
            }

            // Test 5: JSON flag evaluation
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonResult = client.getJsonValue("lab-json", new HashMap<>());
            Object nested = jsonResult.get("nested");
            Object count = jsonResult.get("count");
            if (Boolean.TRUE.equals(nested) && count instanceof Number && ((Number) count).doubleValue() == 100.0) {
                pass("JSON flag evaluation");
            } else {
                fail("JSON flag - unexpected value: " + jsonResult);
            }

            // Test 6: Default value for missing flag
            boolean missingValue = client.getBooleanValue("non-existent", true);
            if (missingValue) {
                pass("Default value for missing flag");
            } else {
                fail("Missing flag - expected default true, got " + missingValue);
            }

            // Test 7: Context management - identify
            System.out.println("\nTesting context management...");
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("plan", "premium");
            attributes.put("country", "US");
            client.identify("lab-user-123", attributes);
            EvaluationContext context = client.getContext();
            if (context != null && "lab-user-123".equals(context.getUserId())) {
                pass("identify()");
            } else {
                fail("identify() - context not set correctly");
            }

            // Test 8: Context management - getContext
            if (context != null && context.getCustom() != null && "premium".equals(context.getCustom().get("plan"))) {
                pass("getContext()");
            } else {
                fail("getContext() - custom attributes missing");
            }

            // Test 9: Context management - clearContext (Java SDK uses clearContext to clear, reset creates anonymous)
            client.clearContext();
            EvaluationContext resetContext = client.getContext();
            if (resetContext == null) {
                pass("clearContext()");
            } else {
                fail("clearContext() - context not cleared");
            }

            // Test 10: Event tracking
            System.out.println("\nTesting event tracking...");
            try {
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("sdk", "java");
                eventData.put("version", "1.0.0");
                client.track("lab_verification", eventData);
                pass("track()");
            } catch (Exception e) {
                fail("track() - " + e.getMessage());
            }

            // Test 11: Flush (offline mode - no-op but should not throw)
            try {
                client.flush();
                pass("flush()");
            } catch (Exception e) {
                fail("flush() - " + e.getMessage());
            }

            // Test 12: Cleanup
            System.out.println("\nTesting cleanup...");
            try {
                client.close();
                pass("close()");
            } catch (Exception e) {
                fail("close() - " + e.getMessage());
            }

        } catch (Exception e) {
            fail("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }

        // Summary
        System.out.println("\n" + "=".repeat(40));
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        System.out.println("=".repeat(40));

        if (failed > 0) {
            System.out.println("\n\u001B[31mSome verifications failed!\u001B[0m");
            System.exit(1);
        } else {
            System.out.println("\n\u001B[32mAll verifications passed!\u001B[0m");
            System.exit(0);
        }
    }

    private static void pass(String test) {
        System.out.println(PASS + " " + test);
        passed++;
    }

    private static void fail(String test) {
        System.out.println(FAIL + " " + test);
        failed++;
    }
}
