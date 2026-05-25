package com.maritime.platform.gateway.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtEncryptor tests")
class JwtEncryptorTest {

    private static final String SECRET = "my-test-secret-key-minimum-256-bits-long!!";

    private final JwtEncryptor encryptor = new JwtEncryptor(SECRET);

    @Nested
    @DisplayName("Round-trip encrypt/decrypt")
    class RoundTrip {

        @Test
        @DisplayName("encrypted token decrypts back to original")
        void roundTrip() {
            String original = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiIxMjMifQ.signature";
            String encrypted = encryptor.encrypt(original);
            assertThat(encrypted).isNotEqualTo(original);
            assertThat(encryptor.decrypt(encrypted)).isEqualTo(original);
        }

        @Test
        @DisplayName("empty string round-trips")
        void emptyString() {
            String encrypted = encryptor.encrypt("");
            assertThat(encryptor.decrypt(encrypted)).isEmpty();
        }

        @Test
        @DisplayName("single character round-trips")
        void singleChar() {
            String encrypted = encryptor.encrypt("x");
            assertThat(encryptor.decrypt(encrypted)).isEqualTo("x");
        }

        @Test
        @DisplayName("long JWT token round-trips")
        void longToken() {
            StringBuilder sb = new StringBuilder();
            sb.append("eyJhbGciOiJIUzI1NiJ9.");
            sb.append("e".repeat(500));
            sb.append(".signature");
            String original = sb.toString();
            String encrypted = encryptor.encrypt(original);
            assertThat(encryptor.decrypt(encrypted)).isEqualTo(original);
        }

        @Test
        @DisplayName("each encryption produces different ciphertext")
        void differentCiphertexts() {
            String plaintext = "same-token";
            String e1 = encryptor.encrypt(plaintext);
            String e2 = encryptor.encrypt(plaintext);
            assertThat(e1).isNotEqualTo(e2);
        }
    }

    @Nested
    @DisplayName("Decryption failure")
    class DecryptionFailure {

        @Test
        @DisplayName("decrypting garbage data returns null")
        void garbageReturnsNull() {
            assertThat(encryptor.decrypt("not-valid-base64!!!")).isNull();
        }

        @Test
        @DisplayName("decrypting empty string returns null")
        void emptyStringReturnsNull() {
            assertThat(encryptor.decrypt("")).isNull();
        }

        @Test
        @DisplayName("decrypting null returns null")
        void nullReturnsNull() {
            assertThat(encryptor.decrypt(null)).isNull();
        }

        @Test
        @DisplayName("decrypting too-short data returns null")
        void tooShortReturnsNull() {
            assertThat(encryptor.decrypt("YQ")).isNull();
        }

        @Test
        @DisplayName("decrypting with wrong key returns null")
        void wrongKeyReturnsNull() {
            JwtEncryptor otherEncryptor = new JwtEncryptor("other-secret-key-for-testing-purpose!");
            JwtEncryptor thisEncryptor = new JwtEncryptor(SECRET);

            String encrypted = otherEncryptor.encrypt("test-token");
            assertThat(thisEncryptor.decrypt(encrypted)).isNull();
        }

        @Test
        @DisplayName("decrypting tampered ciphertext returns null")
        void tamperedCiphertextReturnsNull() {
            String original = "test-jwt-token-for-tampering";
            String encrypted = encryptor.encrypt(original);

            byte[] decoded = java.util.Base64.getUrlDecoder().decode(encrypted);
            decoded[decoded.length - 1] ^= 0xFF;
            String tampered = java.util.Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(decoded);

            assertThat(encryptor.decrypt(tampered)).isNull();
        }
    }

    @Nested
    @DisplayName("Key derivation")
    class KeyDerivation {

        @Test
        @DisplayName("same secret produces compatible encryptors")
        void sameSecretCompatible() {
            JwtEncryptor e1 = new JwtEncryptor(SECRET);
            JwtEncryptor e2 = new JwtEncryptor(SECRET);

            String encrypted = e1.encrypt("hello");
            assertThat(e2.decrypt(encrypted)).isEqualTo("hello");
        }

        @Test
        @DisplayName("different secrets produce incompatible encryptors")
        void differentSecretsIncompatible() {
            JwtEncryptor e1 = new JwtEncryptor("secret-one-with-minimum-256-bit-length!");
            JwtEncryptor e2 = new JwtEncryptor("secret-two-with-minimum-256-bit-length!");

            String encrypted = e1.encrypt("hello");
            assertThat(e2.decrypt(encrypted)).isNull();
        }
    }
}
