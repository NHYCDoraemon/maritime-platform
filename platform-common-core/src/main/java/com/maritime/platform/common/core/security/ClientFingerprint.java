package com.maritime.platform.common.core.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Client fingerprint computation for token binding.
 *
 * <p>Generates a SHA-256 hash of (clientIP + User-Agent) to bind
 * JWT tokens to a specific client environment. Gateway verifies
 * the fingerprint on every request — a stolen token used from a
 * different IP or browser will be rejected.
 *
 * <p>The hash is truncated to 16 hex chars (64 bits) to keep
 * the JWT compact while providing sufficient collision resistance
 * for session-scoped binding.
 */
public final class ClientFingerprint {

    private static final int FINGERPRINT_LENGTH = 16;

    private ClientFingerprint() {
    }

    /**
     * Compute fingerprint from client IP and User-Agent.
     *
     * @param clientIp  client IP address (may be null)
     * @param userAgent User-Agent header (may be null)
     * @return 16-char hex fingerprint
     */
    public static String compute(String clientIp, String userAgent) {
        String raw = normalize(clientIp) + "|" + normalize(userAgent);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(
                    raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash)
                    .substring(0, FINGERPRINT_LENGTH);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "SHA-256 not available", e);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
