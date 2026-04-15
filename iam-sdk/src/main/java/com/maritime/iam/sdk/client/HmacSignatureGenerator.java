package com.maritime.iam.sdk.client;

import com.maritime.platform.common.core.security.HmacSignatureValidator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Generates HMAC signature headers for outgoing requests
 * to iam-query-service via the gateway.
 */
public final class HmacSignatureGenerator {

    private final String appCode;
    private final String appSecret;

    public HmacSignatureGenerator(String appCode,
                                  String appSecret) {
        this.appCode = appCode;
        this.appSecret = appSecret;
    }

    public HmacHeaders generate(String body) {
        String timestamp = String.valueOf(
                System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString()
                .replace("-", "");
        String bodyDigest = sha256Hex(
                body != null ? body : "");
        String signature = HmacSignatureValidator.sign(
                appSecret, appCode, timestamp,
                nonce, bodyDigest);
        return new HmacHeaders(
                appCode, timestamp, nonce,
                bodyDigest, signature);
    }

    public record HmacHeaders(
            String appCode,
            String timestamp,
            String nonce,
            String bodyDigest,
            String signature
    ) {
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md =
                    MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(
                    input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
