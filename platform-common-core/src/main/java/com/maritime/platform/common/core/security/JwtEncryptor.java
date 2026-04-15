package com.maritime.platform.common.core.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for JWT tokens.
 *
 * <p>Wraps a signed JWT string into an opaque encrypted blob
 * so the client cannot read the claims. The gateway decrypts
 * before JWT signature verification.
 *
 * <p>Format: Base64(IV[12] + ciphertext + tag[16])
 *
 * <p>Derives the AES key from the JWT shared secret via
 * SHA-256, so no additional key management is needed.
 */
public final class JwtEncryptor {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey aesKey;

    /**
     * @param jwtSecret the same shared secret used for JWT signing.
     *                  SHA-256 derived to produce a 256-bit AES key.
     */
    public JwtEncryptor(String jwtSecret) {
        this.aesKey = deriveKey(jwtSecret);
    }

    /**
     * Encrypt a signed JWT string into an opaque Base64 blob.
     */
    public String encrypt(String jwt) {
        try {
            byte[] plaintext = jwt.getBytes(StandardCharsets.UTF_8);
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            // IV + ciphertext (includes GCM tag)
            ByteBuffer buffer = ByteBuffer.allocate(
                    IV_LENGTH + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "JWT encryption failed", e);
        }
    }

    /**
     * Decrypt an opaque Base64 blob back to the signed JWT string.
     *
     * @return the original JWT string, or null if decryption fails
     */
    public String decrypt(String encryptedToken) {
        try {
            byte[] decoded = Base64.getUrlDecoder()
                    .decode(encryptedToken);
            if (decoded.length < IV_LENGTH + 1) {
                return null;
            }

            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, aesKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static SecretKey deriveKey(String secret) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(
                    secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException(
                    "AES key derivation failed", e);
        }
    }
}
