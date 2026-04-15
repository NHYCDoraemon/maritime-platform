package com.maritime.platform.common.core.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utility for computing and verifying HMAC-SHA256 request signatures.
 *
 * <p>Used by the gateway to validate service-to-service calls from
 * business systems. Business systems sign requests with their
 * {@code systemSecret}; the gateway verifies before forwarding.
 *
 * <p>Signature headers:
 * <ul>
 *   <li>{@code X-App-Code} — business system code</li>
 *   <li>{@code X-Timestamp} — epoch millis</li>
 *   <li>{@code X-Nonce} — random string (>= 16 bytes)</li>
 *   <li>{@code X-Body-Digest} — SHA-256 hex of request body</li>
 *   <li>{@code X-Signature} — HMAC-SHA256 hex of canonical string</li>
 * </ul>
 *
 * <p>Canonical signing string:
 * <pre>systemCode={X-App-Code}&amp;timestamp={X-Timestamp}&amp;nonce={X-Nonce}&amp;bodyDigest={X-Body-Digest}</pre>
 */
public final class HmacSignatureValidator {

    public static final String HEADER_APP_CODE = "X-App-Code";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_BODY_DIGEST = "X-Body-Digest";
    public static final String HEADER_SIGNATURE = "X-Signature";

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long MAX_TIMESTAMP_SKEW_MS = 300_000L;

    private HmacSignatureValidator() {
    }

    /**
     * Computes HMAC-SHA256 signature, returns hex-encoded result.
     */
    public static String sign(String appSecret,
                              String appCode,
                              String timestamp,
                              String nonce,
                              String bodyDigest) {
        String payload = buildPayload(appCode, timestamp, nonce, bodyDigest);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    appSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(
                    "HMAC-SHA256 computation failed", e);
        }
    }

    /**
     * Verifies signature using constant-time comparison.
     */
    public static boolean verify(String appSecret,
                                 String appCode,
                                 String timestamp,
                                 String nonce,
                                 String bodyDigest,
                                 String signature) {
        String expected = sign(
                appSecret, appCode, timestamp, nonce, bodyDigest);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Builds canonical signing string.
     */
    public static String buildPayload(String appCode,
                                      String timestamp,
                                      String nonce,
                                      String bodyDigest) {
        return "systemCode=" + appCode
                + "&timestamp=" + timestamp
                + "&nonce=" + nonce
                + "&bodyDigest=" + bodyDigest;
    }

    /**
     * Checks whether the timestamp is within the allowed clock skew.
     */
    public static boolean isTimestampFresh(String timestamp,
                                           long maxSkewMs) {
        try {
            long ts = Long.parseLong(timestamp);
            long diff = Math.abs(System.currentTimeMillis() - ts);
            return diff <= maxSkewMs;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Overload using default 5-minute skew.
     */
    public static boolean isTimestampFresh(String timestamp) {
        return isTimestampFresh(timestamp, MAX_TIMESTAMP_SKEW_MS);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
