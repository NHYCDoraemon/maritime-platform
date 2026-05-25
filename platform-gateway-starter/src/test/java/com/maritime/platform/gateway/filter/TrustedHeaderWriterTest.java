package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.security.GatewayPrincipal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TrustedHeaderWriter tests")
class TrustedHeaderWriterTest {

	private final TrustedHeaderWriter writer = new TrustedHeaderWriter();

	// ---------- JWT user headers ----------

	@Nested
	@DisplayName("JWT user header injection")
	class JwtUserHeaders {

		@Test
		@DisplayName("all user headers are set on the request")
		void allUserHeadersSet() {
			GatewayPrincipal.User user = new GatewayPrincipal.User(
					"user-123", "TestUser", "session-abc",
					"ORG001", "Org One",
					List.of("scope-a", "scope-b"),
					"SSO", "tenant-1");

			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			var builder = exchange.getRequest().mutate();
			writer.writeUserHeaders(builder, user);

			var headers = builder.build().getHeaders();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_ID)).isEqualTo("user-123");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_NAME)).isEqualTo("TestUser");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_ACTIVE_ORG_CODE)).isEqualTo("ORG001");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_ACTIVE_ORG_NAME)).isEqualTo("Org One");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_TENANT_ID)).isEqualTo("tenant-1");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_SESSION_ID)).isEqualTo("session-abc");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_SYSTEM_SCOPE)).isEqualTo("scope-a,scope-b");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_SOURCE)).isEqualTo("SSO");
		}

		@Test
		@DisplayName("null fields are written as empty strings")
		void nullFieldsWrittenAsEmpty() {
			GatewayPrincipal.User user = new GatewayPrincipal.User(
					"user-1", null, "session-1",
					null, null, null, null, null);

			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			var builder = exchange.getRequest().mutate();
			writer.writeUserHeaders(builder, user);

			var headers = builder.build().getHeaders();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_ID)).isEqualTo("user-1");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_NAME)).isEmpty();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_ACTIVE_ORG_CODE)).isEmpty();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_ACTIVE_ORG_NAME)).isEmpty();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_TENANT_ID)).isEmpty();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_SESSION_ID)).isEqualTo("session-1");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_SYSTEM_SCOPE)).isEmpty();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_SOURCE)).isEmpty();
		}

		@Test
		@DisplayName("single scope value is written as a single string")
		void singleScopeValue() {
			GatewayPrincipal.User user = new GatewayPrincipal.User(
					"u-1", "n", "s-1", "o", "on", List.of("admin"), "src", "t-1");

			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			var builder = exchange.getRequest().mutate();
			writer.writeUserHeaders(builder, user);

			assertThat(builder.build().getHeaders().getFirst(TrustedHeaderWriter.X_SYSTEM_SCOPE))
					.isEqualTo("admin");
		}
	}

	// ---------- HMAC app headers ----------

	@Nested
	@DisplayName("HMAC app header injection")
	class HmacAppHeaders {

		@Test
		@DisplayName("all app headers are set on the request")
		void allAppHeadersSet() {
			GatewayPrincipal.App app = new GatewayPrincipal.App(
					"app-key-1", "app-code-1", "app-id-1",
					"tenant-1", "T001", List.of("read", "write"));

			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			var builder = exchange.getRequest().mutate();
			writer.writeAppHeaders(builder, app);

			var headers = builder.build().getHeaders();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_VERIFIED_APP_CODE)).isEqualTo("app-code-1");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_CODE)).isEqualTo("app-code-1");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_ID)).isEqualTo("app-id-1");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_TENANT_CODE)).isEqualTo("T001");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_TENANT_ID)).isEqualTo("tenant-1");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_PERMISSIONS)).isEqualTo("read,write");
		}

		@Test
		@DisplayName("null fields are written as empty strings")
		void nullFieldsWrittenAsEmpty() {
			GatewayPrincipal.App app = new GatewayPrincipal.App(
					"app-key-1", "app-code-1",
					null, null, null, null);

			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			var builder = exchange.getRequest().mutate();
			writer.writeAppHeaders(builder, app);

			var headers = builder.build().getHeaders();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_VERIFIED_APP_CODE)).isEqualTo("app-code-1");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_CODE)).isEqualTo("app-code-1");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_ID)).isEmpty();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_TENANT_CODE)).isEmpty();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_TENANT_ID)).isEmpty();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_PERMISSIONS)).isEmpty();
		}

		@Test
		@DisplayName("no app headers pollute user header space")
		void noCrossPollutionToUser() {
			GatewayPrincipal.App app = new GatewayPrincipal.App(
					"ak", "ac", "ai", "t", "tc", List.of());

			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			var builder = exchange.getRequest().mutate();
			writer.writeAppHeaders(builder, app);

			var headers = builder.build().getHeaders();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_ID)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_NAME)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_SESSION_ID)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_SYSTEM_SCOPE)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_SOURCE)).isNull();
		}

		@Test
		@DisplayName("no user headers pollute app header space")
		void noCrossPollutionToApp() {
			GatewayPrincipal.User user = new GatewayPrincipal.User(
					"u", "n", "s", "o", "on", List.of(), "src", "t");

			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			var builder = exchange.getRequest().mutate();
			writer.writeUserHeaders(builder, user);

			var headers = builder.build().getHeaders();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_VERIFIED_APP_CODE)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_CODE)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_ID)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_TENANT_CODE)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_PERMISSIONS)).isNull();
		}
	}
}
