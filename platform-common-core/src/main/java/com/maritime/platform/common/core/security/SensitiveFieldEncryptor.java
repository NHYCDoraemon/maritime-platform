package com.maritime.platform.common.core.security;

/**
 * Symmetric field-level encryptor for values marked with {@link Sensitive}.
 *
 * <p>Implementations must produce self-describing envelopes so that
 * {@link #decrypt(String, String)} can recover the plaintext without
 * extra out-of-band state beyond the supplied {@code keyId}.
 */
public interface SensitiveFieldEncryptor {

    /**
     * Encrypt plaintext, returning a Base64 envelope that self-describes
     * enough for {@link #decrypt(String, String)} to recover the plaintext
     * (includes keyId + nonce).
     *
     * @param plaintext value to encrypt (must be non-null)
     * @param keyId    logical key identifier
     * @return Base64 encoded envelope
     * @throws IllegalArgumentException if keyId is unknown or plaintext is null
     */
    String encrypt(String plaintext, String keyId);

    /**
     * Decrypt a ciphertext produced by {@link #encrypt(String, String)}.
     *
     * @param ciphertext Base64 envelope
     * @param keyId     logical key identifier (must match the envelope)
     * @return original plaintext
     * @throws IllegalArgumentException if ciphertext is malformed
     * @throws SecurityException if MAC validation fails (tamper detected)
     */
    String decrypt(String ciphertext, String keyId);
}