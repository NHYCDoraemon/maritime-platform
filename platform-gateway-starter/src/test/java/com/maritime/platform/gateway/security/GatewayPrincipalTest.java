package com.maritime.platform.gateway.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GatewayPrincipal tests")
class GatewayPrincipalTest {

    @Nested
    @DisplayName("User record")
    class UserRecord {

        @Test
        @DisplayName("constructs user with all fields")
        void constructUser() {
            GatewayPrincipal.User user = new GatewayPrincipal.User(
                    "user-123", "Test User", "session-abc",
                    "ORG001", "Test Org",
                    List.of("scope-a", "scope-b"),
                    "SSO", "tenant-001");

            assertThat(user.userId()).isEqualTo("user-123");
            assertThat(user.userName()).isEqualTo("Test User");
            assertThat(user.sessionId()).isEqualTo("session-abc");
            assertThat(user.activeOrgCode()).isEqualTo("ORG001");
            assertThat(user.activeOrgName()).isEqualTo("Test Org");
            assertThat(user.systemScope()).containsExactly("scope-a", "scope-b");
            assertThat(user.userSource()).isEqualTo("SSO");
            assertThat(user.tenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("constructs user with null optional fields")
        void nullOptionalFields() {
            GatewayPrincipal.User user = new GatewayPrincipal.User(
                    "user-123", null, null,
                    null, null,
                    null, null, null);

            assertThat(user.userId()).isEqualTo("user-123");
            assertThat(user.userName()).isNull();
            assertThat(user.sessionId()).isNull();
            assertThat(user.activeOrgCode()).isNull();
            assertThat(user.activeOrgName()).isNull();
            assertThat(user.systemScope()).isEmpty();
            assertThat(user.userSource()).isNull();
            assertThat(user.tenantId()).isNull();
        }

        @Test
        @DisplayName("systemScope list is defensively copied at construction")
        void systemScopeDefensivelyCopied() {
            List<String> mutable = new ArrayList<>(List.of("scope-a"));
            GatewayPrincipal.User user = new GatewayPrincipal.User(
                    "user-123", "Test", "sess",
                    "ORG", "Org",
                    mutable, "SSO", "tenant");

            mutable.add("scope-b");

            assertThat(user.systemScope()).containsExactly("scope-a");
        }

        @Test
        @DisplayName("returned systemScope is unmodifiable when non-null")
        void systemScopeIsUnmodifiable() {
            GatewayPrincipal.User user = new GatewayPrincipal.User(
                    "user-123", "Test", "sess",
                    "ORG", "Org",
                    List.of("scope-a"), "SSO", "tenant");

            assertThatThrownBy(() -> user.systemScope().add("extra"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("User is instance of GatewayPrincipal")
        void userIsGatewayPrincipal() {
            GatewayPrincipal.User user = new GatewayPrincipal.User(
                    "u", null, null, null, null, null, null, null);
            assertThat(user).isInstanceOf(GatewayPrincipal.class);
        }

        @Test
        @DisplayName("user record equality works correctly")
        void recordEquality() {
            GatewayPrincipal.User u1 = new GatewayPrincipal.User(
                    "id", "name", "sess", "org", "OrgName",
                    List.of("s1"), "src", "tid");
            GatewayPrincipal.User u2 = new GatewayPrincipal.User(
                    "id", "name", "sess", "org", "OrgName",
                    List.of("s1"), "src", "tid");

            assertThat(u1).isEqualTo(u2);
            assertThat(u2).isEqualTo(u1);
            assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
        }

        @Test
        @DisplayName("user record toString includes field values")
        void recordToString() {
            GatewayPrincipal.User user = new GatewayPrincipal.User(
                    "user-123", "Test User", "session-abc",
                    "ORG001", "Test Org",
                    List.of("scope-a"), "SSO", "tenant-001");

            String s = user.toString();
            assertThat(s).contains("user-123");
            assertThat(s).contains("session-abc");
        }
    }

    @Nested
    @DisplayName("App record")
    class AppRecord {

        @Test
        @DisplayName("constructs app with all fields")
        void constructApp() {
            GatewayPrincipal.App app = new GatewayPrincipal.App(
                    "app-key-001", "my-app-code", "app-id-001",
                    "tenant-001", "T001", List.of("read", "write"));

            assertThat(app.appKey()).isEqualTo("app-key-001");
            assertThat(app.appCode()).isEqualTo("my-app-code");
            assertThat(app.appId()).isEqualTo("app-id-001");
            assertThat(app.tenantId()).isEqualTo("tenant-001");
            assertThat(app.tenantCode()).isEqualTo("T001");
            assertThat(app.permissions()).containsExactly("read", "write");
        }

        @Test
        @DisplayName("constructs app with null optional fields")
        void nullOptionalFields() {
            GatewayPrincipal.App app = new GatewayPrincipal.App(
                    "app-key-001", null, null,
                    null, null, null);

            assertThat(app.appKey()).isEqualTo("app-key-001");
            assertThat(app.appCode()).isNull();
            assertThat(app.appId()).isNull();
            assertThat(app.tenantId()).isNull();
            assertThat(app.tenantCode()).isNull();
            assertThat(app.permissions()).isEmpty();
        }

        @Test
        @DisplayName("permissions list is defensively copied at construction")
        void permissionsDefensivelyCopied() {
            List<String> mutable = new ArrayList<>(List.of("read"));
            GatewayPrincipal.App app = new GatewayPrincipal.App(
                    "key", "code", "id",
                    "tid", "tcode", mutable);

            mutable.add("write");

            assertThat(app.permissions()).containsExactly("read");
        }

        @Test
        @DisplayName("returned permissions is unmodifiable when non-null")
        void permissionsIsUnmodifiable() {
            GatewayPrincipal.App app = new GatewayPrincipal.App(
                    "key", "code", "id",
                    "tid", "tcode", List.of("read"));

            assertThatThrownBy(() -> app.permissions().add("write"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("App is instance of GatewayPrincipal")
        void appIsGatewayPrincipal() {
            GatewayPrincipal.App app = new GatewayPrincipal.App(
                    "key", null, null, null, null, null);
            assertThat(app).isInstanceOf(GatewayPrincipal.class);
        }

        @Test
        @DisplayName("App is NOT instance of User")
        void appIsNotUser() {
            GatewayPrincipal.App app = new GatewayPrincipal.App(
                    "key", null, null, null, null, null);
            assertThat(app).isNotInstanceOf(GatewayPrincipal.User.class);
        }

        @Test
        @DisplayName("User is NOT instance of App")
        void userIsNotApp() {
            GatewayPrincipal.User user = new GatewayPrincipal.User(
                    "u", null, null, null, null, null, null, null);
            assertThat(user).isNotInstanceOf(GatewayPrincipal.App.class);
        }

        @Test
        @DisplayName("app record equality works correctly")
        void recordEquality() {
            GatewayPrincipal.App a1 = new GatewayPrincipal.App(
                    "key", "code", "id", "tid", "tc", List.of("p1"));
            GatewayPrincipal.App a2 = new GatewayPrincipal.App(
                    "key", "code", "id", "tid", "tc", List.of("p1"));

            assertThat(a1).isEqualTo(a2);
            assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
        }
    }

    @Nested
    @DisplayName("Principal attribute constant")
    class AttributeConstant {

        @Test
        @DisplayName("ATTRIBUTE constant has expected value")
        void attributeConstant() {
            assertThat(GatewayPrincipal.ATTRIBUTE).isEqualTo("gateway.principal");
        }
    }
}
