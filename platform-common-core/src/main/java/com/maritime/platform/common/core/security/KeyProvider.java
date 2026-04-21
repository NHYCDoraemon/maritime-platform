package com.maritime.platform.common.core.security;

import javax.crypto.SecretKey;

/**
 * SPI for supplying AES secret keys to {@link SensitiveFieldEncryptor}.
 *
 * <p>The default implementation is {@link EnvKeyProvider}. Production
 * deployments should override this bean with a KMS-backed provider.
 */
public interface KeyProvider {

    /**
     * Resolve an AES-256 {@link SecretKey} for the given keyId.
     *
     * @param keyId logical key identifier
     * @return resolved secret key (must be 256-bit)
     * @throws IllegalArgumentException if keyId is unknown
     */
    SecretKey keyFor(String keyId);
}
