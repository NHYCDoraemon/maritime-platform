package com.maritime.platform.gateway.security.hmac;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AppCredential tests")
class AppCredentialTest {

    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("builds credential with all fields set")
        void buildsWithAllFields() {
            AppCredential credential = AppCredential.builder()
                    .appKey("app-key-001")
                    .appSecret("secret-value")
                    .appCode("my-app")
                    .appId("app-id-001")
                    .tenantId("tenant-001")
                    .tenantCode("T001")
                    .permissions(List.of("read", "write"))
                    .enabled(true)
                    .build();

            assertThat(credential.getAppKey()).isEqualTo("app-key-001");
            assertThat(credential.getAppSecret()).isEqualTo("secret-value");
            assertThat(credential.getAppCode()).isEqualTo("my-app");
            assertThat(credential.getAppId()).isEqualTo("app-id-001");
            assertThat(credential.getTenantId()).isEqualTo("tenant-001");
            assertThat(credential.getTenantCode()).isEqualTo("T001");
            assertThat(credential.getPermissions()).containsExactly("read", "write");
            assertThat(credential.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("defaults enabled to true")
        void defaultsEnabledToTrue() {
            AppCredential credential = AppCredential.builder()
                    .appKey("key")
                    .appSecret("secret")
                    .build();

            assertThat(credential.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("defaults permissions to empty list")
        void defaultsPermissionsToEmptyList() {
            AppCredential credential = AppCredential.builder()
                    .appKey("key")
                    .appSecret("secret")
                    .build();

            assertThat(credential.getPermissions()).isEmpty();
        }

        @Test
        @DisplayName("explicitly set enabled to false")
        void disabledCredential() {
            AppCredential credential = AppCredential.builder()
                    .appKey("key")
                    .appSecret("secret")
                    .enabled(false)
                    .build();

            assertThat(credential.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("null permissions becomes empty list")
        void nullPermissionsBecomesEmpty() {
            AppCredential credential = AppCredential.builder()
                    .appKey("key")
                    .appSecret("secret")
                    .permissions(null)
                    .build();

            assertThat(credential.getPermissions()).isEmpty();
        }

        @Test
        @DisplayName("optional fields default to null")
        void optionalFieldsDefaultToNull() {
            AppCredential credential = AppCredential.builder()
                    .appKey("key")
                    .appSecret("secret")
                    .build();

            assertThat(credential.getAppCode()).isNull();
            assertThat(credential.getAppId()).isNull();
            assertThat(credential.getTenantId()).isNull();
            assertThat(credential.getTenantCode()).isNull();
        }
    }

    @Nested
    @DisplayName("Permissions immutability")
    class PermissionsImmutability {

        @Test
        @DisplayName("permissions list is defensively copied from builder")
        void permissionsDefensivelyCopied() {
            List<String> mutableList = new ArrayList<>(List.of("read", "write"));
            AppCredential credential = AppCredential.builder()
                    .appKey("key")
                    .appSecret("secret")
                    .permissions(mutableList)
                    .build();

            mutableList.add("admin");

            assertThat(credential.getPermissions()).containsExactly("read", "write");
        }

        @Test
        @DisplayName("returned permissions list is unmodifiable")
        void returnedPermissionsIsUnmodifiable() {
            AppCredential credential = AppCredential.builder()
                    .appKey("key")
                    .appSecret("secret")
                    .permissions(List.of("read"))
                    .build();

            assertThatThrownBy(() -> credential.getPermissions().add("write"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("credentials with same values are not equal (no equals override)")
        void noEqualsOverride() {
            AppCredential c1 = AppCredential.builder()
                    .appKey("key").appSecret("secret").appCode("code").build();
            AppCredential c2 = AppCredential.builder()
                    .appKey("key").appSecret("secret").appCode("code").build();

            assertThat(c1).isNotEqualTo(c2);
        }
    }
}
