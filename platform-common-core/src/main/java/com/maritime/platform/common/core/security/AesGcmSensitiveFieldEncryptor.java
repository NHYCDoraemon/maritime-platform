package com.maritime.platform.common.core.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM implementation of {@link SensitiveFieldEncryptor}.
 *
 * <p>Envelope format (Base64 encoded):
 * <pre>
 *   version[1] || keyId_length[1] || keyId_bytes[N] || nonce[12] || ciphertext+tag[...]
 * </pre>
 *
 * <ul>
 *   <li>Version byte is {@code 1} (current).</li>
 *   <li>Nonce is 12 random bytes (GCM recommended length).</li>
 *   <li>GCM tag is 128 bits, appended to ciphertext by the JCE provider.</li>
 * </ul>
 */
public class AesGcmSensitiveFieldEncryptor implements SensitiveFieldEncryptor {

    private static final byte VERSION = 1;
    private static final int NONCE_LEN = 12;
    private static final int TAG_LEN_BITS = 128;
    private static final int TAG_LEN_BYTES = TAG_LEN_BITS / 8;
    private static final int KEY_ID_MAX_LEN = 255;
    private static final String TRANSFORM = "AES/GCM/NoPadding";

    private final KeyProvider keyProvider;
    private final SecureRandom secureRandom;

    public AesGcmSensitiveFieldEncryptor(KeyProvider keyProvider) {
        this.keyProvider = keyProvider;
        this.secureRandom = new SecureRandom();
    }

    @Override
    public String encrypt(String plaintext, String keyId) {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext cannot be null");
        }
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalArgumentException("keyId cannot be blank");
        }
        SecretKey key = keyProvider.keyFor(keyId);

        byte[] nonce = new byte[NONCE_LEN];
        secureRandom.nextBytes(nonce);

        byte[] keyIdBytes = keyId.getBytes(StandardCharsets.UTF_8);
        if (keyIdBytes.length > KEY_ID_MAX_LEN) {
            throw new IllegalArgumentException("keyId too long (max 255 bytes)");
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, nonce));
            byte[] cipherAndTag = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buf = ByteBuffer.allocate(
                    1 + 1 + keyIdBytes.length + NONCE_LEN + cipherAndTag.length);
            buf.put(VERSION);
            buf.put((byte) keyIdBytes.length);
            buf.put(keyIdBytes);
            buf.put(nonce);
            buf.put(cipherAndTag);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new SecurityException("AES-GCM encryption failed", e);
        }
    }

    @Override
    public String decrypt(String ciphertext, String keyId) {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw new IllegalArgumentException("ciphertext cannot be blank");
        }
        byte[] envelope;
        try {
            envelope = Base64.getDecoder().decode(ciphertext);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ciphertext is not valid Base64", e);
        }
        if (envelope.length < 1 + 1 + NONCE_LEN + TAG_LEN_BYTES) {
            throw new IllegalArgumentException("ciphertext envelope is too short");
        }
        ByteBuffer buf = ByteBuffer.wrap(envelope);
        byte version = buf.get();
        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported envelope version: " + version);
        }
        int keyIdLen = buf.get() & 0xFF;
        if (keyIdLen <= 0 || buf.remaining() < keyIdLen + NONCE_LEN + TAG_LEN_BYTES) {
            throw new IllegalArgumentException("ciphertext envelope malformed");
        }
        byte[] keyIdBytes = new byte[keyIdLen];
        buf.get(keyIdBytes);
        String envelopeKeyId = new String(keyIdBytes, StandardCharsets.UTF_8);
        if (!envelopeKeyId.equals(keyId)) {
            throw new IllegalArgumentException(
                    "keyId mismatch: envelope=" + envelopeKeyId + ", caller=" + keyId);
        }
        byte[] nonce = new byte[NONCE_LEN];
        buf.get(nonce);
        byte[] cipherAndTag = new byte[buf.remaining()];
        buf.get(cipherAndTag);

        try {
            SecretKey key = keyProvider.keyFor(envelopeKeyId);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, nonce));
            byte[] plaintext = cipher.doFinal(cipherAndTag);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (javax.crypto.AEADBadTagException e) {
            throw new SecurityException(
                    "GCM tag validation failed - ciphertext tampered or wrong key", e);
        } catch (Exception e) {
            throw new SecurityException("AES-GCM decryption failed", e);
        }
    }
}