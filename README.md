# FlagKit Java SDK

Official Java SDK for [FlagKit](https://flagkit.dev) - Feature flag management made simple.

## Requirements

- Java 11+

## Installation

### Maven

```xml
<dependency>
    <groupId>com.teracrafts</groupId>
    <artifactId>flagkit</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.teracrafts:flagkit:1.0.0'
```

## Quick Start

```java
import dev.flagkit.*;

public class Main {
    public static void main(String[] args) {
        // Initialize the SDK
        FlagKitClient client = FlagKit.initialize("sdk_your_api_key");

        // Identify a user
        client.identify("user-123", Map.of("plan", "premium"));

        // Evaluate flags
        boolean darkMode = client.getBooleanValue("dark-mode", false);
        String welcomeMsg = client.getStringValue("welcome-message", "Hello!");
        int maxItems = client.getIntValue("max-items", 10);
        Map<String, Object> config = client.getJsonValue("feature-config", Map.of());

        // Get full evaluation details
        EvaluationResult result = client.evaluate("dark-mode");
        System.out.printf("Value: %s, Reason: %s%n", result.getValue(), result.getReason());

        // Track custom events
        client.track("button_clicked", Map.of("button", "signup"));

        // Shutdown
        FlagKit.shutdown();
    }
}
```

## Features

- **Type-safe evaluation** - Boolean, string, number, and JSON flag types
- **Local caching** - Fast evaluations with configurable TTL and optional encryption
- **Background polling** - Automatic flag updates
- **Event tracking** - Analytics with batching
- **Resilient** - Circuit breaker, retry with backoff, offline support
- **Thread-safe** - Safe for concurrent use
- **Security** - PII detection, request signing, bootstrap verification, cache encryption

## API Reference

### Initialization

```java
// Using the singleton (recommended for most apps)
FlagKitClient client = FlagKit.initialize(
    FlagKitOptions.builder("sdk_...")
        .pollingInterval(Duration.ofSeconds(30))
        .cacheTtl(Duration.ofMinutes(5))
        .debug()
        .build()
);

// Or create a client directly
FlagKitClient client = new FlagKitClient(
    FlagKitOptions.builder("sdk_...")
        .offline()
        .bootstrap(Map.of("feature-flag", true))
        .build()
);
client.initialize();
```

### Flag Evaluation

```java
// Boolean flags
boolean enabled = client.getBooleanValue("feature-flag", false);

// String flags
String variant = client.getStringValue("button-text", "Click");

// Number flags (double)
double limit = client.getNumberValue("rate-limit", 100.0);

// Integer flags
int count = client.getIntValue("max-retries", 3);

// JSON flags
Map<String, Object> config = client.getJsonValue("config", Map.of("enabled", false));

// Full evaluation result
EvaluationResult result = client.evaluate("feature-flag");
// result.getFlagKey(), result.getValue(), result.isEnabled(), result.getReason(), result.getVersion()

// Check flag existence
if (client.hasFlag("my-flag")) {
    // ...
}

// Get all flag keys
Set<String> keys = client.getAllFlagKeys();
```

### Context Management

```java
// Create context
EvaluationContext ctx = EvaluationContext.create("user-123")
    .withEmail("user@example.com")
    .withCountry("US")
    .withCustom("plan", "premium")
    .withPrivateAttribute("email");

// Set global context
client.setContext(ctx);

// Get current context
EvaluationContext current = client.getContext();

// Clear context
client.clearContext();

// Identify user (shorthand)
client.identify("user-123", Map.of("email", "user@example.com"));

// Reset to anonymous
client.reset();

// Pass context to evaluation
boolean enabled = client.getBooleanValue("feature-flag", false, ctx);
```

### Event Tracking

```java
// Track custom event
client.track("purchase", Map.of(
    "amount", 99.99,
    "currency", "USD",
    "product_id", "prod-123"
));

// Force flush pending events
client.flush();
```

### Lifecycle

```java
// Check if SDK is ready
if (client.isReady()) {
    // ...
}

// Wait for ready
client.waitForReady();

// Wait with timeout
client.waitForReady(Duration.ofSeconds(10));

// Force refresh flags from server
client.refresh();

// Close SDK and cleanup
client.close();

// Using singleton
FlagKit.shutdown();
```

## Error Handling

```java
try {
    FlagKitClient client = FlagKit.initialize("sdk_...");
} catch (FlagKitException e) {
    System.err.printf("FlagKit error [%s]: %s%n", e.getErrorCode(), e.getMessage());
    if (e.isRecoverable()) {
        // Retry logic
    }
}
```

## Security

The SDK includes built-in security features that can be enabled through configuration options, including request signing, bootstrap verification, cache encryption, evaluation jitter for timing attack protection, and error message sanitization. These are configurable via `FlagKitOptions.builder()`.

## Local Development


```java
FlagKitClient client = FlagKit.initialize(
    FlagKitOptions.builder("sdk_your_api_key")
        .build()
);
```

## Testing

```java
// Use offline mode with bootstrap values
FlagKitClient client = new FlagKitClient(
    FlagKitOptions.builder("sdk_test")
        .offline()
        .bootstrap(Map.of("feature-flag", true))
        .build()
);
client.initialize();

// Or mock the HTTP responses using MockWebServer
```

## Thread Safety

All SDK methods are safe for concurrent use from multiple threads. The client uses internal synchronization to ensure thread-safe access to:

- Flag cache
- Event queue
- Context management
- Polling state

## License

MIT License - see [LICENSE](LICENSE) for details.
