/**
 * Security utilities for the FlagKit SDK.
 *
 * <p>This package provides comprehensive security features:</p>
 *
 * <ul>
 *   <li><strong>Request Signing</strong> - HMAC-SHA256 signatures for request integrity
 *       ({@link dev.flagkit.security.RequestSigning})</li>
 *   <li><strong>Bootstrap Verification</strong> - Cryptographic verification of bootstrap data
 *       ({@link dev.flagkit.security.BootstrapVerification})</li>
 *   <li><strong>Cache Encryption</strong> - AES-256-GCM encryption for cached flag data
 *       ({@link dev.flagkit.security.EncryptedCache})</li>
 *   <li><strong>Evaluation Jitter</strong> - Timing attack protection
 *       ({@link dev.flagkit.security.EvaluationJitterConfig})</li>
 *   <li><strong>API Key Rotation</strong> - Seamless key rotation with failover
 *       ({@link dev.flagkit.security.ApiKeyManager})</li>
 * </ul>
 *
 * <p>Error sanitization is provided in the error package via
 * {@link dev.flagkit.error.ErrorSanitizer}.</p>
 *
 * @see dev.flagkit.FlagKitOptions.Builder for configuration options
 */
package dev.flagkit.security;
