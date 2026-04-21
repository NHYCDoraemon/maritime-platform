package com.maritime.platform.common.core.security;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Reads AES-256 keys from environment variables.
 *
 * <p>Variable naming: {@code SENSITIVE_FIELD_KEY_<keyId_uppercase>}.
 * For example, keyId {@code "default"} maps to env var
 * {@code SENSITIVE_FIELD_KEY_DEFAULT}.
 *
 * <p>The env var value must be Base64-encoded 32 raw bytes (AES-256).
 *
 * <p>Production deployments should override this bean with a KMS-backed
 * provider.
 */
public class EnvKeyProvider implements KeyProvider {

    private static final String ENV_PREFIX = "SENSITIVE_FIELD_KEY_";
    private static final int AES_256_KEY_BYTES = 32;

    @Override
    public SecretKey keyFor(String keyId) {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("keyId cannot be blank");
        }
        String varName = ENV_PREFIX + keyId.toUpperCase();
        String base64Key = System.getenv(varName);
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing environment variable " + varName + " for keyId '" + keyId + "'");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Environment variable " + varName + " is not valid Base64", e);
        }
        if (keyBytes.length != AES_256_KEY_BYTES) {
            throw new IllegalArgumentException(
                    "AES-256 key must be 32 bytes, got " + keyBytes.length + " for " + varName);
        }
        return new SecretKeySpec(keyBytes, "AES");
    }
}