package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.error.DefaultGatewayErrorWriter;
import com.maritime.platform.gateway.error.GatewayErrorWriter;
import com.maritime.platform.gateway.security.AuthMode;
import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.RouteSecurityPolicy;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationException;
import com.maritime.platform.gateway.security.nonce.JwtNonceValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("JwtNonceGatewayFilter tests")
@ExtendWith(MockitoExtension.class)
class JwtNonceGatewayFilterTest {

	@Mock
	private JwtNonceValidator nonceValidator;

	private GatewaySecurityProperties properties;

	private static final GatewayPrincipal.User TEST_PRINCIPAL = new GatewayPrincipal.User(
			"user-123", "TestUser", "session-abc",
			null, null, List.of(), null, null);

	@BeforeEach
	void setUp() {
		properties = new GatewaySecurityProperties();
		properties.getJwt().setEnabled(true);
		properties.getJwt().setSecret("my-test-secret-key-minimum-256-bits-long!!");
		properties.getJwt().setIssuer("maritime-platform");
	}

	private final GatewayErrorWriter errorWriter = new DefaultGatewayErrorWriter();

	private JwtNonceGatewayFilter filter() {
		return new JwtNonceGatewayFilter(nonceValidator, properties, errorWriter);
	}

	private static GatewayFilterChain capturingChain(AtomicReference<ServerWebExchange> ref) {
		return exchange -> {
			if (ref != null) {
				ref.set(exchange);
			}
			return Mono.empty();
		};
	}

	private static GatewayFilterChain passThroughChain() {
		return exchange -> Mono.empty();
	}

	// ---------- filter order ----------

	@Nested
	@DisplayName("Filter order")
	class FilterOrderTests {

		@Test
		@DisplayName("JwtNonceGatewayFilter order is JWT_NONCE")
		void filterOrderIsJwtNonce() {
			Order ann = AnnotationUtils.findAnnotation(JwtNonceGatewayFilter.class, Order.class);
			assertThat(ann).isNotNull();
			assertThat(ann.value()).isEqualTo(GatewayFilterOrder.JWT_NONCE);
		}

		@Test
		@DisplayName("JWT_NONCE is between JWT_AUTHENTICATION and CONTEXT_HEADER_INJECTION")
		void orderPositionInCorrectRange() {
			assertThat(GatewayFilterOrder.JWT_NONCE)
					.isGreaterThan(GatewayFilterOrder.JWT_AUTHENTICATION);
			assertThat(GatewayFilterOrder.JWT_NONCE)
					.isLessThan(GatewayFilterOrder.CONTEXT_HEADER_INJECTION);
		}

		@Test
		@DisplayName("filter implements Ordered")
		void implementsOrdered() {
			assertThat(filter()).isInstanceOf(Ordered.class);
		}
	}

	// ---------- nonce required (missing header) ----------

	@Nested
	@DisplayName("Nonce required")
	class NonceRequired {

		@Test
		@DisplayName("POST without X-Nonce header returns NONCE_REQUIRED")
		void postWithoutNonceReturnsNonceRequired() {
			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, TEST_PRINCIPAL);

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			MockServerHttpResponse response = exchange.getResponse();
			assertThat(response.getStatusCode().value()).isEqualTo(401);
			String body = response.getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.NONCE_REQUIRED);
		}

		@Test
		@DisplayName("PUT without X-Nonce header returns NONCE_REQUIRED")
		void putWithoutNonceReturnsNonceRequired() {
			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.put("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, TEST_PRINCIPAL);

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.NONCE_REQUIRED);
		}

		@Test
		@DisplayName("DELETE without X-Nonce header returns NONCE_REQUIRED")
		void deleteWithoutNonceReturnsNonceRequired() {
			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.delete("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, TEST_PRINCIPAL);

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.NONCE_REQUIRED);
		}

		@Test
		@DisplayName("PATCH without X-Nonce header returns NONCE_REQUIRED")
		void patchWithoutNonceReturnsNonceRequired() {
			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.patch("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, TEST_PRINCIPAL);

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.NONCE_REQUIRED);
		}

		@Test
		@DisplayName("empty X-Nonce header returns NONCE_REQUIRED")
		void emptyNonceReturnsNonceRequired() {
			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data")
							.header("X-Nonce", ""));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, TEST_PRINCIPAL);

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.NONCE_REQUIRED);
		}
	}

	// ---------- replay detection ----------

	@Nested
	@DisplayName("Replay detection")
	class ReplayDetection {

		@Test
		@DisplayName("duplicate nonce returns REPLAY_DETECTED")
		void duplicateNonceReturnsReplayDetected() {
			when(nonceValidator.validate("session-abc", "nonce-dup"))
					.thenReturn(Mono.error(new JwtAuthenticationException(
							GatewayAuthErrorCode.REPLAY_DETECTED,
							"Nonce has already been used: nonce-dup")));

			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data")
							.header("X-Nonce", "nonce-dup"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, TEST_PRINCIPAL);

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			MockServerHttpResponse response = exchange.getResponse();
			assertThat(response.getStatusCode().value()).isEqualTo(401);
			String body = response.getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.REPLAY_DETECTED);
		}
	}

	// ---------- successful nonce validation ----------

	@Nested
	@DisplayName("Successful nonce validation")
	class SuccessfulNonce {

		@Test
		@DisplayName("POST with valid nonce forwards to next filter")
		void validNonceForwards() {
			when(nonceValidator.validate("session-abc", "nonce-xyz"))
					.thenReturn(Mono.empty());

			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data")
							.header("X-Nonce", "nonce-xyz"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, TEST_PRINCIPAL);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(f.filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertThat(captured.get()).isNotNull();
		}
	}

	// ---------- method not required ----------

	@Nested
	@DisplayName("Method not required")
	class MethodNotRequired {

		@Test
		@DisplayName("GET without X-Nonce passes through")
		void getWithoutNoncePassesThrough() {
			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, TEST_PRINCIPAL);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(f.filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertThat(captured.get()).isNotNull();
		}

		@Test
		@DisplayName("HEAD without X-Nonce passes through")
		void headWithoutNoncePassesThrough() {
			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.head("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, TEST_PRINCIPAL);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(f.filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertThat(captured.get()).isNotNull();
		}

		@Test
		@DisplayName("OPTIONS without X-Nonce passes through")
		void optionsWithoutNoncePassesThrough() {
			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.options("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, TEST_PRINCIPAL);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(f.filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertThat(captured.get()).isNotNull();
		}

		@Test
		@DisplayName("custom required-methods only enforces configured methods")
		void customRequiredMethods() {
			// Configure only POST to require nonce
			properties.getJwt().getNonce().setRequiredMethods(List.of("POST"));

			// PUT should pass through (not in required-methods) without calling validator
			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.put("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, TEST_PRINCIPAL);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(f.filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertThat(captured.get()).isNotNull();
		}
	}

	// ---------- nonce disabled ----------

	@Nested
	@DisplayName("Nonce disabled")
	class NonceDisabled {

		@Test
		@DisplayName("nonce disabled skips validation for POST without X-Nonce")
		void nonceDisabledSkipsValidation() {
			properties.getJwt().getNonce().setEnabled(false);

			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, TEST_PRINCIPAL);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(f.filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertThat(captured.get()).isNotNull();
		}
	}

	// ---------- auth mode passthrough ----------

	@Nested
	@DisplayName("Auth mode passthrough")
	class AuthModePassthrough {

		@Test
		@DisplayName("NONE auth mode passes through without nonce check")
		void noneAuthModePassesThrough() {
			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/public/health"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.NONE, "PUBLIC:/health"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(f.filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertThat(captured.get()).isNotNull();
		}

		@Test
		@DisplayName("null policy passes through without nonce check")
		void nullPolicyPassesThrough() {
			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data")
							.header("X-Nonce", "nonce-xyz"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(f.filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertThat(captured.get()).isNotNull();
		}

		@Test
		@DisplayName("JWT route without principal passes through")
		void jwtRouteWithoutPrincipalPassesThrough() {
			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			// No principal set in exchange attributes
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(f.filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertThat(captured.get()).isNotNull();
		}
	}

	// ---------- error response format ----------

	@Nested
	@DisplayName("Error response format")
	class ErrorResponseFormat {

		@Test
		@DisplayName("NONCE_REQUIRED response is valid JSON")
		void nonceRequiredResponseIsJson() {
			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, TEST_PRINCIPAL);

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains("\"code\"");
			assertThat(body).contains("\"message\"");
			assertThat(body).startsWith("{");
			assertThat(body).endsWith("}");
			assertThat(exchange.getResponse().getHeaders().getContentType().toString())
					.contains("application/json");
		}

		@Test
		@DisplayName("error response status is 401")
		void errorResponseIs401() {
			JwtNonceGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			exchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, TEST_PRINCIPAL);

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
		}
	}
}
