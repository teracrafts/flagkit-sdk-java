package dev.flagkit.http;

import com.google.gson.Gson;
import dev.flagkit.FlagKitOptions;
import dev.flagkit.error.ErrorCode;
import dev.flagkit.error.FlagKitException;
import dev.flagkit.security.RequestSignature;
import dev.flagkit.security.RequestSigning;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client with retry and circuit breaker support.
 */
public class HttpClient {
    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final List<String> VALID_SUBSCRIPTION_STATUSES = Arrays.asList(
            "active", "trial", "past_due", "suspended", "cancelled"
    );

    /** Default base URL for the FlagKit API */
    public static final String DEFAULT_BASE_URL = "https://api.flagkit.dev/api/v1";

    /**
     * Returns the appropriate base URL based on the localPort.
     *
     * @param localPort the port for local development, or null for production
     * @return the base URL to use
     */
    public static String getBaseUrl(Integer localPort) {
        return localPort != null ? "http://localhost:" + localPort + "/api/v1" : DEFAULT_BASE_URL;
    }

    private final OkHttpClient client;
    private final String baseUrl;
    private final String apiKey;
    private final CircuitBreaker circuitBreaker;
    private final Gson gson;
    private final int maxRetries;
    private final Duration baseDelay;
    private final Duration maxDelay;
    private final double backoffMultiplier;
    private final Random random;
    private final boolean enableRequestSigning;
    private volatile UsageUpdateCallback usageUpdateCallback;

    public HttpClient(String baseUrl, String apiKey, Duration timeout, int maxRetries) {
        this(baseUrl, apiKey, timeout, maxRetries, true);
    }

    public HttpClient(String baseUrl, String apiKey, Duration timeout, int maxRetries, boolean enableRequestSigning) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .readTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .writeTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .build();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.circuitBreaker = new CircuitBreaker();
        this.gson = new Gson();
        this.maxRetries = maxRetries;
        this.baseDelay = Duration.ofSeconds(1);
        this.maxDelay = Duration.ofSeconds(30);
        this.backoffMultiplier = 2.0;
        this.random = new Random();
        this.enableRequestSigning = enableRequestSigning;
    }

    /**
     * Sets the callback for usage metrics updates.
     *
     * @param callback the callback to invoke when usage metrics are received
     */
    public void setUsageUpdateCallback(UsageUpdateCallback callback) {
        this.usageUpdateCallback = callback;
    }

    /**
     * Performs a GET request.
     */
    public HttpResponse get(String path) throws FlagKitException {
        return executeWithRetry(() -> doGet(path));
    }

    /**
     * Performs a POST request.
     */
    public HttpResponse post(String path, Object body) throws FlagKitException {
        return executeWithRetry(() -> doPost(path, body));
    }

    /**
     * Closes the HTTP client.
     */
    public void close() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }

    private HttpResponse doGet(String path) throws IOException, FlagKitException {
        if (!circuitBreaker.allow()) {
            throw new FlagKitException(ErrorCode.CIRCUIT_OPEN, "Circuit breaker is open");
        }

        String url = baseUrl + path;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-API-Key", apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "FlagKit-Java/" + FlagKitOptions.SDK_VERSION)
                .addHeader("X-FlagKit-SDK-Version", FlagKitOptions.SDK_VERSION)
                .addHeader("X-FlagKit-SDK-Language", "java")
                .get()
                .build();

        logger.debug("GET {}", url);

        try (Response response = client.newCall(request).execute()) {
            return handleResponse(response);
        }
    }

    private HttpResponse doPost(String path, Object body) throws IOException, FlagKitException {
        if (!circuitBreaker.allow()) {
            throw new FlagKitException(ErrorCode.CIRCUIT_OPEN, "Circuit breaker is open");
        }

        String url = baseUrl + path;
        String jsonBody = gson.toJson(body);
        RequestBody requestBody = RequestBody.create(jsonBody, JSON);

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("X-API-Key", apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "FlagKit-Java/" + FlagKitOptions.SDK_VERSION)
                .addHeader("X-FlagKit-SDK-Version", FlagKitOptions.SDK_VERSION)
                .addHeader("X-FlagKit-SDK-Language", "java");

        // Add request signing headers if enabled
        if (enableRequestSigning && jsonBody != null && !jsonBody.isEmpty()) {
            RequestSignature signature = RequestSigning.createRequestSignature(jsonBody, apiKey);
            requestBuilder.addHeader("X-Signature", signature.getSignature());
            requestBuilder.addHeader("X-Timestamp", String.valueOf(signature.getTimestamp()));
            requestBuilder.addHeader("X-Key-Id", signature.getKeyId());
        }

        Request request = requestBuilder.post(requestBody).build();

        logger.debug("POST {} - {}", url, jsonBody);

        try (Response response = client.newCall(request).execute()) {
            return handleResponse(response);
        }
    }

    private HttpResponse handleResponse(Response response) throws IOException, FlagKitException {
        int statusCode = response.code();
        String body = response.body() != null ? response.body().string() : "";

        logger.debug("Response: {} - {}", statusCode, body);

        // Extract and process usage metrics from headers
        UsageMetrics usageMetrics = extractUsageMetrics(response);
        if (usageMetrics != null && usageUpdateCallback != null) {
            usageUpdateCallback.onUsageUpdate(usageMetrics);
        }

        if (statusCode >= 200 && statusCode < 300) {
            circuitBreaker.recordSuccess();
            return new HttpResponse(statusCode, body, usageMetrics);
        }

        circuitBreaker.recordFailure();

        // Handle specific error codes
        if (statusCode == 401) {
            throw new FlagKitException(ErrorCode.AUTH_UNAUTHORIZED, "Unauthorized: Invalid API key");
        } else if (statusCode == 403) {
            throw new FlagKitException(ErrorCode.AUTH_INVALID_KEY, "Forbidden: API key does not have access");
        } else if (statusCode == 404) {
            throw new FlagKitException(ErrorCode.EVAL_FLAG_NOT_FOUND, "Resource not found");
        } else if (statusCode == 429) {
            throw new FlagKitException(ErrorCode.NETWORK_ERROR, "Rate limited");
        } else if (statusCode >= 500) {
            throw new FlagKitException(ErrorCode.NETWORK_ERROR, "Server error: " + statusCode);
        }

        throw new FlagKitException(ErrorCode.NETWORK_ERROR, "HTTP error: " + statusCode);
    }

    /**
     * Extracts usage metrics from response headers.
     *
     * @param response the HTTP response
     * @return the extracted usage metrics, or null if no usage headers are present
     */
    private UsageMetrics extractUsageMetrics(Response response) {
        String apiUsage = response.header("X-API-Usage-Percent");
        String evalUsage = response.header("X-Evaluation-Usage-Percent");
        String rateLimitWarning = response.header("X-Rate-Limit-Warning");
        String subscriptionStatus = response.header("X-Subscription-Status");

        // Return null if no usage headers are present
        if (apiUsage == null && evalUsage == null && rateLimitWarning == null && subscriptionStatus == null) {
            return null;
        }

        UsageMetrics.Builder builder = new UsageMetrics.Builder();
        builder.rateLimitWarning("true".equalsIgnoreCase(rateLimitWarning));

        if (apiUsage != null) {
            try {
                double parsed = Double.parseDouble(apiUsage);
                builder.apiUsagePercent(parsed);
                // Log warning for high usage
                if (parsed >= 80) {
                    logger.warn("[FlagKit] API usage at {}%", parsed);
                }
            } catch (NumberFormatException e) {
                logger.debug("Failed to parse X-API-Usage-Percent header: {}", apiUsage);
            }
        }

        if (evalUsage != null) {
            try {
                double parsed = Double.parseDouble(evalUsage);
                builder.evaluationUsagePercent(parsed);
                // Log warning for high usage
                if (parsed >= 80) {
                    logger.warn("[FlagKit] Evaluation usage at {}%", parsed);
                }
            } catch (NumberFormatException e) {
                logger.debug("Failed to parse X-Evaluation-Usage-Percent header: {}", evalUsage);
            }
        }

        if (subscriptionStatus != null && VALID_SUBSCRIPTION_STATUSES.contains(subscriptionStatus.toLowerCase())) {
            builder.subscriptionStatus(subscriptionStatus.toLowerCase());
            // Log error for suspended subscription
            if ("suspended".equalsIgnoreCase(subscriptionStatus)) {
                logger.error("[FlagKit] Subscription suspended - service degraded");
            }
        }

        return builder.build();
    }

    private HttpResponse executeWithRetry(HttpOperation operation) throws FlagKitException {
        FlagKitException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.execute();
            } catch (FlagKitException e) {
                lastException = e;
                if (!e.isRecoverable() || attempt >= maxRetries) {
                    throw e;
                }
                logger.debug("Retry attempt {} after error: {}", attempt, e.getMessage());
                sleep(calculateBackoff(attempt));
            } catch (IOException e) {
                lastException = FlagKitException.networkError("Network error", e);
                if (attempt >= maxRetries) {
                    throw lastException;
                }
                logger.debug("Retry attempt {} after IO error: {}", attempt, e.getMessage());
                sleep(calculateBackoff(attempt));
            }
        }

        throw lastException != null ? lastException :
                new FlagKitException(ErrorCode.NETWORK_RETRY_LIMIT, "Retry limit exceeded");
    }

    private Duration calculateBackoff(int attempt) {
        double exponentialDelay = baseDelay.toMillis() * Math.pow(backoffMultiplier, attempt - 1);
        long delay = Math.min((long) exponentialDelay, maxDelay.toMillis());
        // Add jitter (up to 10%)
        long jitter = (long) (delay * 0.1 * random.nextDouble());
        return Duration.ofMillis(delay + jitter);
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface HttpOperation {
        HttpResponse execute() throws IOException, FlagKitException;
    }

    /**
     * HTTP response container.
     */
    public static class HttpResponse {
        private final int statusCode;
        private final String body;
        private final UsageMetrics usageMetrics;

        public HttpResponse(int statusCode, String body) {
            this(statusCode, body, null);
        }

        public HttpResponse(int statusCode, String body, UsageMetrics usageMetrics) {
            this.statusCode = statusCode;
            this.body = body;
            this.usageMetrics = usageMetrics;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }

        public UsageMetrics getUsageMetrics() {
            return usageMetrics;
        }
    }

    /**
     * Usage metrics extracted from response headers.
     */
    public static class UsageMetrics {
        private final Double apiUsagePercent;
        private final Double evaluationUsagePercent;
        private final boolean rateLimitWarning;
        private final String subscriptionStatus;

        private UsageMetrics(Builder builder) {
            this.apiUsagePercent = builder.apiUsagePercent;
            this.evaluationUsagePercent = builder.evaluationUsagePercent;
            this.rateLimitWarning = builder.rateLimitWarning;
            this.subscriptionStatus = builder.subscriptionStatus;
        }

        /**
         * Returns the percentage of API call limit used this period (0-150+).
         *
         * @return the API usage percentage, or null if not provided
         */
        public Double getApiUsagePercent() {
            return apiUsagePercent;
        }

        /**
         * Returns the percentage of evaluation limit used (0-150+).
         *
         * @return the evaluation usage percentage, or null if not provided
         */
        public Double getEvaluationUsagePercent() {
            return evaluationUsagePercent;
        }

        /**
         * Returns whether approaching rate limit threshold.
         *
         * @return true if rate limit warning is active
         */
        public boolean isRateLimitWarning() {
            return rateLimitWarning;
        }

        /**
         * Returns the current subscription status.
         * Valid values: active, trial, past_due, suspended, cancelled
         *
         * @return the subscription status, or null if not provided
         */
        public String getSubscriptionStatus() {
            return subscriptionStatus;
        }

        @Override
        public String toString() {
            return "UsageMetrics{" +
                    "apiUsagePercent=" + apiUsagePercent +
                    ", evaluationUsagePercent=" + evaluationUsagePercent +
                    ", rateLimitWarning=" + rateLimitWarning +
                    ", subscriptionStatus='" + subscriptionStatus + '\'' +
                    '}';
        }

        /**
         * Builder for UsageMetrics.
         */
        public static class Builder {
            private Double apiUsagePercent;
            private Double evaluationUsagePercent;
            private boolean rateLimitWarning;
            private String subscriptionStatus;

            public Builder apiUsagePercent(Double apiUsagePercent) {
                this.apiUsagePercent = apiUsagePercent;
                return this;
            }

            public Builder evaluationUsagePercent(Double evaluationUsagePercent) {
                this.evaluationUsagePercent = evaluationUsagePercent;
                return this;
            }

            public Builder rateLimitWarning(boolean rateLimitWarning) {
                this.rateLimitWarning = rateLimitWarning;
                return this;
            }

            public Builder subscriptionStatus(String subscriptionStatus) {
                this.subscriptionStatus = subscriptionStatus;
                return this;
            }

            public UsageMetrics build() {
                return new UsageMetrics(this);
            }
        }
    }

    /**
     * Functional interface for receiving usage metrics updates.
     */
    @FunctionalInterface
    public interface UsageUpdateCallback {
        /**
         * Called when usage metrics are received from the API.
         *
         * @param metrics the usage metrics
         */
        void onUsageUpdate(UsageMetrics metrics);
    }
}
