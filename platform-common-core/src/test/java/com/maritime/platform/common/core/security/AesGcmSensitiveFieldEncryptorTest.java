package com.maritime.platform.common.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmSensitiveFieldEncryptorTest {

    private static final String TEST_KEY_ID = "test";

    private AesGcmSensitiveFieldEncryptor encryptor;
    private InMemoryKeyProvider keyProvider;

    @BeforeEach
    void setUp() {
        keyProvider = new InMemoryKeyProvider();
        keyProvider.addKey(TEST_KEY_ID, generateAes256Key());
        encryptor = new AesGcmSensitiveFieldEncryptor(keyProvider);
    }

    @Test
    void encrypt_then_decrypt_recoversPlaintext() {
        String plaintext = "sailor-passport-X123456";

        String ciphertext = encryptor.encrypt(plaintext, TEST_KEY_ID);
        String recovered = encryptor.decrypt(ciphertext, TEST_KEY_ID);

        assertThat(recovered).isEqualTo(plaintext);
    }

    @Test
    void encrypt_sameInputTwice_producesDifferentCiphertext() {
        String plaintext = "same-value";

        String first = encryptor.encrypt(plaintext, TEST_KEY_ID);
        String second = encryptor.encrypt(plaintext, TEST_KEY_ID);

        assertThat(first).isNotEqualTo(second);
        assertThat(encryptor.decrypt(first, TEST_KEY_ID)).isEqualTo(plaintext);
        assertThat(encryptor.decrypt(second, TEST_KEY_ID)).isEqualTo(plaintext);
    }

    @Test
    void decrypt_tamperedCiphertext_throwsSecurityException() {
        String ciphertext = encryptor.encrypt("hello-world", TEST_KEY_ID);
        byte[] bytes = Base64.getDecoder().decode(ciphertext);
        // Flip one byte near the end (inside ciphertext+tag region)
        bytes[bytes.length - 1] ^= (byte) 0x01;
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> encryptor.decrypt(tampered, TEST_KEY_ID))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void decrypt_withWrongKeyId_throwsIllegalArgumentException() {
        keyProvider.addKey("other", generateAes256Key());
        String ciphertext = encryptor.encrypt("value", TEST_KEY_ID);

        assertThatThrownBy(() -> encryptor.decrypt(ciphertext, "other"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keyId mismatch");
    }

    @Test
    void encrypt_nullPlaintext_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> encryptor.encrypt(null, TEST_KEY_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plaintext");
    }

    @Test
    void decrypt_invalidBase64_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> encryptor.decrypt("###not-base64###", TEST_KEY_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    void decrypt_truncatedEnvelope_throwsIllegalArgumentException() {
        String tiny = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});

        assertThatThrownBy(() -> encryptor.decrypt(tiny, TEST_KEY_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too short");
    }

    private static SecretKey generateAes256Key() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return new SecretKeySpec(bytes, "AES");
    }

    /** Test double: in-memory keystore. */
    private static final class InMemoryKeyProvider implements KeyProvider {
        private final Map<String, SecretKey> keys = new HashMap<>();

        void addKey(String keyId, SecretKey key) {
            keys.put(keyId, key);
        }

        @Override
        public SecretKey keyFor(String keyId) {
            SecretKey key = keys.get(keyId);
            if (key == null) {
                throw new IllegalArgumentException("Unknown keyId: " + keyId);
            }
            return key;
        }
    }
}