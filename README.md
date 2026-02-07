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

## Architecture

The SDK is organized into clean, modular packages:

```
dev.flagkit/
├── FlagKit.java            # Static methods and singleton access
├── FlagKitClient.java      # Main client implementation
├── FlagKitOptions.java     # Configuration options
├── core/                   # Core components
│   ├── Cache.java          # In-memory cache with TTL
│   ├── PollingManager.java
│   └── EventQueue.java     # Event batching
├── http/                   # HTTP client, circuit breaker, retry
│   ├── HttpClient.java
│   └── CircuitBreaker.java
├── error/                  # Error types and codes
│   ├── FlagKitException.java
│   ├── ErrorCode.java
│   └── ErrorSanitizer.java # Error message sanitization
├── types/                  # Type definitions
│   ├── EvaluationContext.java
│   ├── EvaluationResult.java
│   └── FlagState.java
├── security/               # Security utilities
│   ├── RequestSigning.java     # HMAC-SHA256 request signing
│   ├── BootstrapVerification.java  # Bootstrap signature verification
│   ├── EncryptedCache.java     # AES-256-GCM cache encryption
│   ├── EvaluationJitterConfig.java # Timing attack protection
│   ├── ErrorSanitizationConfig.java
│   └── ApiKeyManager.java      # Key rotation support
└── utils/                  # Utilities
    └── Security.java       # PII detection
```

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

## Security Features

### PII Detection

The SDK can detect and warn about potential PII (Personally Identifiable Information) in contexts and events:

```java
import dev.flagkit.utils.Security;
import dev.flagkit.utils.Security.SecurityConfig;

// Enable strict PII mode
SecurityConfig config = SecurityConfig.builder()
    .warnOnPotentialPII(true)
    .build();

// Check for PII in data
Map<String, Object> data = Map.of("email", "user@example.com");
List<String> piiFields = Security.detectPotentialPII(data);
if (!piiFields.isEmpty()) {
    System.out.println("PII detected: " + piiFields);
}
```

### Request Signing

POST requests to the FlagKit API are signed with HMAC-SHA256 for integrity verification:

```java
import dev.flagkit.security.RequestSigning;
import dev.flagkit.security.RequestSignature;

// Create a request signature
String body = "{\"flag_key\": \"my-flag\"}";
RequestSignature signature = RequestSigning.createRequestSignature(body, "sdk_your_api_key");

// Use signature headers in HTTP request:
// X-Signature: signature.getSignature()
// X-Timestamp: signature.getTimestamp()
// X-Key-Id: signature.getKeyId()

// Enable/disable request signing in config (enabled by default)
FlagKitOptions options = FlagKitOptions.builder("sdk_your_api_key")
    .enableRequestSigning(true)  // default
    // .disableRequestSigning()  // to disable
    .build();
```

### Bootstrap Signature Verification

Verify bootstrap data integrity using HMAC signatures:

```java
import dev.flagkit.security.BootstrapConfig;
import dev.flagkit.security.BootstrapVerification;
import dev.flagkit.security.BootstrapVerificationConfig;

// Create signed bootstrap data
Map<String, Object> flags = Map.of(
    "feature-a", true,
    "feature-b", "value"
);
BootstrapConfig bootstrapConfig = BootstrapVerification.createSignedBootstrap(
    flags, "sdk_your_api_key");

// Use signed bootstrap with verification
FlagKitOptions options = FlagKitOptions.builder("sdk_your_api_key")
    .bootstrapConfig(bootstrapConfig)
    .bootstrapVerification(BootstrapVerificationConfig.custom(
        true,                           // enabled
        Duration.ofHours(24),           // max age
        BootstrapVerificationConfig.ON_FAILURE_ERROR  // "warn", "error", or "ignore"
    ))
    .build();

// Or use strict verification (throws on failure)
FlagKitOptions strictOptions = FlagKitOptions.builder("sdk_your_api_key")
    .bootstrapConfig(bootstrapConfig)
    .bootstrapVerification(BootstrapVerificationConfig.strict())
    .build();
```

### Cache Encryption

Enable AES-256-GCM encryption with PBKDF2 key derivation for cached flag data:

```java
import dev.flagkit.security.EncryptedCache;

// Create encrypted cache
EncryptedCache cache = new EncryptedCache("sdk_your_api_key");

// Encrypt/decrypt data
String encrypted = cache.encrypt("sensitive data");
String decrypted = cache.decrypt(encrypted);

// Encrypt/decrypt JSON values
Map<String, Object> value = Map.of("flags", Map.of("feature", true));
String encryptedJson = cache.encryptJson(value);
Map<String, Object> decryptedJson = cache.decryptJson(encryptedJson, Map.class);

// Enable in config
FlagKitOptions options = FlagKitOptions.builder("sdk_your_api_key")
    .enableCacheEncryption()
    .build();
```

### Evaluation Jitter (Timing Attack Protection)

Add random delays to flag evaluations to prevent cache timing attacks:

```java
import dev.flagkit.security.EvaluationJitterConfig;

FlagKitOptions options = FlagKitOptions.builder("sdk_your_api_key")
    .evaluationJitter(new EvaluationJitterConfig(
        true,   // enabled
        5,      // min_ms
        15      // max_ms
    ))
    .build();

// Or use convenience method
FlagKitOptions optionsSimple = FlagKitOptions.builder("sdk_your_api_key")
    .enableEvaluationJitter()           // default 5-15ms
    // .enableEvaluationJitter(10, 20)  // custom range
    .build();
```

### Error Sanitization

Automatically redact sensitive information from error messages:

```java
import dev.flagkit.security.ErrorSanitizationConfig;

FlagKitOptions options = FlagKitOptions.builder("sdk_your_api_key")
    .errorSanitization(new ErrorSanitizationConfig(
        true,   // enabled
        false   // preserve_original (set true for debugging)
    ))
    .build();

// Or use convenience methods
FlagKitOptions optionsDebug = FlagKitOptions.builder("sdk_your_api_key")
    .errorSanitizationWithPreservation()  // enable with preservation
    // .disableErrorSanitization()         // disable entirely
    .build();
```

### Key Rotation

Support seamless API key rotation:

```java
import dev.flagkit.security.ApiKeyManager;

// Using ApiKeyManager directly
ApiKeyManager manager = new ApiKeyManager(
    "sdk_primary_key",
    "sdk_secondary_key"
);

// SDK will automatically failover on 401 errors
if (manager.handle401Error()) {
    System.out.println("Switched to secondary key");
}

// Reset to primary key
manager.resetToPrimary();
```

## All Configuration Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `apiKey` | String | Required | API key for authentication |
| `pollingInterval` | Duration | 30s | Polling interval |
| `enablePolling` | boolean | true | Enable background polling |
| `cacheEnabled` | boolean | true | Enable local caching |
| `cacheTtl` | Duration | 5m | Cache TTL |
| `offline` | boolean | false | Offline mode |
| `localPort` | Integer | null | Local development port |
| `timeout` | Duration | 5s | HTTP request timeout |
| `retries` | int | 3 | Number of retry attempts |
| `bootstrap` | Map | {} | Initial flag values |
| `debug` | boolean | false | Enable debug logging |
| `onReady` | Runnable | null | Ready callback |
| `onError` | Consumer | null | Error callback |
| `onUpdate` | Consumer | null | Update callback |
| `enableRequestSigning` | boolean | true | Enable HMAC-SHA256 request signing |
| `enableCacheEncryption` | boolean | false | Enable AES-256-GCM cache encryption |
| `bootstrapConfig` | BootstrapConfig | null | Signed bootstrap configuration |
| `bootstrapVerification` | Config | enabled | Bootstrap verification settings |
| `evaluationJitter` | Config | disabled | Timing attack protection |
| `errorSanitization` | Config | disabled | Error message sanitization |

## Local Development

Use the `localPort()` option to connect to a local FlagKit server:

```java
FlagKitClient client = FlagKit.initialize(
    FlagKitOptions.builder("sdk_your_api_key")
        .localPort(8200)
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
