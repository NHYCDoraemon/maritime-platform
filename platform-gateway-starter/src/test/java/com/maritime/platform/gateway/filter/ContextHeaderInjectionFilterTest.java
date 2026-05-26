package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.GatewayPrincipalHeaderCustomizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ContextHeaderInjectionFilter tests")
class ContextHeaderInjectionFilterTest {

	private static GatewayFilterChain capturingChain(AtomicReference<ServerWebExchange> ref) {
		return exchange -> {
			ref.set(exchange);
			return Mono.empty();
		};
	}

	private ContextHeaderInjectionFilter filter(List<GatewayPrincipalHeaderCustomizer> customizers) {
		return new ContextHeaderInjectionFilter(new TrustedHeaderWriter(), null, customizers);
	}

	// ---------- no principal ----------

	@Nested
	@DisplayName("No principal in exchange")
	class NoPrincipal {

		@Test
		@DisplayName("passes through when no principal attribute")
		void passesThrough() {
			var exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(filter(null).filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertThat(captured.get()).isNotNull();
		}

		@Test
		@DisplayName("no headers are added when no principal")
		void noHeadersAdded() {
			var exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(filter(null).filter(exchange, capturingChain(captured)))
					.verifyComplete();

			var headers = captured.get().getRequest().getHeaders();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_ID)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_CODE)).isNull();
		}
	}

	// ---------- JWT user injection ----------

	@Nested
	@DisplayName("JWT user principal injection")
	class JwtUserInjection {

		@Test
		@DisplayName("injects all user identity headers")
		void injectsUserHeaders() {
			var exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			GatewayPrincipal.User user = new GatewayPrincipal.User(
					"user-123", "Alice", "sess-abc",
					"ORG01", "Org One",
					List.of("admin", "read"),
					"SSO", "tenant-1");
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, user);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(filter(null).filter(exchange, capturingChain(captured)))
					.verifyComplete();

			var headers = captured.get().getRequest().getHeaders();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_ID)).isEqualTo("user-123");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_NAME)).isEqualTo("Alice");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_ACTIVE_ORG_CODE)).isEqualTo("ORG01");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_ACTIVE_ORG_NAME)).isEqualTo("Org One");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_TENANT_ID)).isEqualTo("tenant-1");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_SESSION_ID)).isEqualTo("sess-abc");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_SYSTEM_SCOPE)).isEqualTo("admin,read");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_SOURCE)).isEqualTo("SSO");
		}

		@Test
		@DisplayName("no app headers are injected for user principal")
		void noAppHeadersForUser() {
			var exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			GatewayPrincipal.User user = new GatewayPrincipal.User(
					"u", "n", "s", "o", "on", List.of(), "src", "t");
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, user);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(filter(null).filter(exchange, capturingChain(captured)))
					.verifyComplete();

			var headers = captured.get().getRequest().getHeaders();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_VERIFIED_APP_CODE)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_CODE)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_ID)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_TENANT_CODE)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_PERMISSIONS)).isNull();
		}
	}

	// ---------- HMAC app injection ----------

	@Nested
	@DisplayName("HMAC app principal injection")
	class HmacAppInjection {

		@Test
		@DisplayName("injects all app identity headers")
		void injectsAppHeaders() {
			var exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			GatewayPrincipal.App app = new GatewayPrincipal.App(
					"app-key-1", "my-app-code", "app-id-99",
					"tenant-5", "T005", List.of("read", "write", "delete"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, app);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(filter(null).filter(exchange, capturingChain(captured)))
					.verifyComplete();

			var headers = captured.get().getRequest().getHeaders();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_VERIFIED_APP_CODE)).isEqualTo("my-app-code");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_CODE)).isEqualTo("my-app-code");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_ID)).isEqualTo("app-id-99");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_TENANT_CODE)).isEqualTo("T005");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_TENANT_ID)).isEqualTo("tenant-5");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_PERMISSIONS)).isEqualTo("read,write,delete");
		}

		@Test
		@DisplayName("no user headers are injected for app principal")
		void noUserHeadersForApp() {
			var exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			GatewayPrincipal.App app = new GatewayPrincipal.App(
					"ak", "ac", "ai", "t", "tc", List.of());
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, app);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(filter(null).filter(exchange, capturingChain(captured)))
					.verifyComplete();

			var headers = captured.get().getRequest().getHeaders();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_ID)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_NAME)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_SESSION_ID)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_SYSTEM_SCOPE)).isNull();
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_SOURCE)).isNull();
		}
	}

	// ---------- customizer ----------

	@Nested
	@DisplayName("GatewayPrincipalHeaderCustomizer")
	class Customizer {

		@Test
		@DisplayName("customizer can add project-specific headers for user principal")
		void customizerAddsProjectHeadersForUser() {
			var exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			GatewayPrincipal.User user = new GatewayPrincipal.User(
					"u", "n", "s", "o", "on", List.of(), "src", "t");
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, user);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			GatewayPrincipalHeaderCustomizer customizer = (builder, principal) -> {
				if (principal instanceof GatewayPrincipal.User u) {
					builder.header("X-Custom-Header", u.userId() + "-custom");
				}
			};

			StepVerifier.create(filter(List.of(customizer)).filter(exchange, capturingChain(captured)))
					.verifyComplete();

			var headers = captured.get().getRequest().getHeaders();
			assertThat(headers.getFirst("X-Custom-Header")).isEqualTo("u-custom");
			// Standard headers still set
			assertThat(headers.getFirst(TrustedHeaderWriter.X_USER_ID)).isEqualTo("u");
		}

		@Test
		@DisplayName("customizer can add project-specific headers for app principal")
		void customizerAddsProjectHeadersForApp() {
			var exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			GatewayPrincipal.App app = new GatewayPrincipal.App(
					"ak", "ac", "ai", "t", "tc", List.of());
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, app);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			GatewayPrincipalHeaderCustomizer customizer = (builder, principal) -> {
				if (principal instanceof GatewayPrincipal.App a) {
					builder.header("X-App-Department", "engineering");
				}
			};

			StepVerifier.create(filter(List.of(customizer)).filter(exchange, capturingChain(captured)))
					.verifyComplete();

			var headers = captured.get().getRequest().getHeaders();
			assertThat(headers.getFirst("X-App-Department")).isEqualTo("engineering");
			assertThat(headers.getFirst(TrustedHeaderWriter.X_APP_CODE)).isEqualTo("ac");
		}

		@Test
		@DisplayName("multiple customizers all execute")
		void multipleCustomizersExecute() {
			var exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			GatewayPrincipal.User user = new GatewayPrincipal.User(
					"u", "n", "s", "o", "on", List.of(), "src", "t");
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, user);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			GatewayPrincipalHeaderCustomizer c1 = (b, p) -> b.header("X-C1", "v1");
			GatewayPrincipalHeaderCustomizer c2 = (b, p) -> b.header("X-C2", "v2");

			StepVerifier.create(filter(List.of(c1, c2)).filter(exchange, capturingChain(captured)))
					.verifyComplete();

			var headers = captured.get().getRequest().getHeaders();
			assertThat(headers.getFirst("X-C1")).isEqualTo("v1");
			assertThat(headers.getFirst("X-C2")).isEqualTo("v2");
		}

		@Test
		@DisplayName("no customizers registered, filter still works")
		void emptyCustomizersWorks() {
			var exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			GatewayPrincipal.User user = new GatewayPrincipal.User(
					"u", "n", "s", "o", "on", List.of(), "src", "t");
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, user);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(filter(List.of()).filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertThat(captured.get().getRequest().getHeaders().getFirst(TrustedHeaderWriter.X_USER_ID))
					.isEqualTo("u");
		}
	}
}
