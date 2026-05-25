package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.error.DefaultGatewayErrorWriter;
import com.maritime.platform.gateway.error.GatewayErrorWriter;
import com.maritime.platform.gateway.security.AuthMode;
import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.RouteSecurityPolicy;
import com.maritime.platform.gateway.security.jwt.DefaultJwtClaimsMapper;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationManager;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("JwtAuthenticationGatewayFilter tests")
class JwtAuthenticationGatewayFilterTest {

	private static final String SECRET = "my-test-secret-key-minimum-256-bits-long!!";
	private static final String ISSUER = "maritime-platform";

	private GatewaySecurityProperties properties;
	private SecretKey signingKey;

	@BeforeEach
	void setUp() {
		properties = new GatewaySecurityProperties();
		properties.getJwt().setEnabled(true);
		properties.getJwt().setSecret(SECRET);
		properties.getJwt().setIssuer(ISSUER);
		signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
	}

	private final GatewayErrorWriter errorWriter = new DefaultGatewayErrorWriter();

	private JwtAuthenticationGatewayFilter filter() {
		JwtAuthenticationManager manager = new JwtAuthenticationManager(properties,
				new DefaultJwtClaimsMapper(properties));
		return new JwtAuthenticationGatewayFilter(manager, errorWriter);
	}

	private String createValidToken() {
		Instant now = Instant.now();
		return Jwts.builder()
				.issuer(ISSUER)
				.claim("userId", "user-123")
				.claim("userName", "TestUser")
				.claim("sessionId", "session-abc")
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
				.signWith(signingKey)
				.compact();
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
		@DisplayName("JwtAuthenticationGatewayFilter order is JWT_AUTHENTICATION")
		void filterOrderIsJwtAuthentication() {
			Order ann = AnnotationUtils.findAnnotation(JwtAuthenticationGatewayFilter.class, Order.class);
			assertThat(ann).isNotNull();
			assertThat(ann.value()).isEqualTo(GatewayFilterOrder.JWT_AUTHENTICATION);
		}

		@Test
		@DisplayName("JWT_AUTHENTICATION is between SECURITY_POLICY and CONTEXT_HEADER_INJECTION")
		void orderPositionInCorrectRange() {
			assertThat(GatewayFilterOrder.JWT_AUTHENTICATION)
					.isGreaterThan(GatewayFilterOrder.SECURITY_POLICY);
			assertThat(GatewayFilterOrder.JWT_AUTHENTICATION)
					.isLessThan(GatewayFilterOrder.CONTEXT_HEADER_INJECTION);
		}

		@Test
		@DisplayName("filters implement Ordered")
		void implementsOrdered() {
			assertThat(filter()).isInstanceOf(Ordered.class);
		}
	}

	// ---------- token extraction ----------

	@Nested
	@DisplayName("Token extraction")
	class TokenExtraction {

		@Test
		@DisplayName("missing Authorization header returns MISSING_TOKEN")
		void missingAuthHeaderReturnsMissingToken() {
			JwtAuthenticationGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			MockServerHttpResponse response = exchange.getResponse();
			assertThat(response.getStatusCode().value()).isEqualTo(401);
			assertThat(response.getHeaders().getContentType().toString()).contains("application/json");
			String body = response.getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.MISSING_TOKEN);
		}

		@Test
		@DisplayName("Authorization header without Bearer prefix returns MISSING_TOKEN")
		void authHeaderWithoutBearerReturnsMissingToken() {
			JwtAuthenticationGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data")
							.header("Authorization", "Basic dXNlcjpwYXNz"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.MISSING_TOKEN);
		}

		@Test
		@DisplayName("empty Bearer token returns MISSING_TOKEN")
		void emptyBearerTokenReturnsMissingToken() {
			JwtAuthenticationGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data")
							.header("Authorization", "Bearer "));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.MISSING_TOKEN);
		}
	}

	// ---------- invalid token ----------

	@Nested
	@DisplayName("Invalid token handling")
	class InvalidTokenHandling {

		@Test
		@DisplayName("malformed JWT returns INVALID_TOKEN")
		void malformedJwtReturnsInvalidToken() {
			JwtAuthenticationGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data")
							.header("Authorization", "Bearer not.a.valid.jwt"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.INVALID_TOKEN);
		}

		@Test
		@DisplayName("token with invalid signature returns INVALID_TOKEN")
		void invalidSignatureReturnsInvalidToken() {
			SecretKey otherKey = Keys.hmacShaKeyFor("other-secret-key-for-testing-only!!".getBytes(StandardCharsets.UTF_8));
			Instant now = Instant.now();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.claim("userId", "user-123")
					.claim("sessionId", "session-abc")
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plus(1, ChronoUnit.HOURS)))
					.signWith(otherKey)
					.compact();

			JwtAuthenticationGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data")
							.header("Authorization", "Bearer " + token));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.INVALID_TOKEN);
		}
	}

	// ---------- expired token ----------

	@Nested
	@DisplayName("Expired token handling")
	class ExpiredTokenHandling {

		@Test
		@DisplayName("expired token returns TOKEN_EXPIRED")
		void expiredTokenReturnsTokenExpired() {
			Instant now = Instant.now();
			String token = Jwts.builder()
					.issuer(ISSUER)
					.claim("userId", "user-123")
					.claim("sessionId", "session-abc")
					.issuedAt(Date.from(now.minus(2, ChronoUnit.HOURS)))
					.expiration(Date.from(now.minus(1, ChronoUnit.HOURS)))
					.signWith(signingKey)
					.compact();

			JwtAuthenticationGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data")
							.header("Authorization", "Bearer " + token));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.TOKEN_EXPIRED);
		}
	}

	// ---------- successful authentication ----------

	@Nested
	@DisplayName("Successful authentication")
	class SuccessfulAuth {

		@Test
		@DisplayName("valid token stores principal in exchange attribute")
		void storesPrincipalInExchange() {
			String token = createValidToken();
			JwtAuthenticationGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data")
							.header("Authorization", "Bearer " + token));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(f.filter(exchange, capturingChain(captured)))
					.verifyComplete();

			ServerWebExchange result = captured.get();
			GatewayPrincipal.User principal = result.getAttribute(GatewayPrincipal.ATTRIBUTE);
			assertThat(principal).isNotNull();
			assertThat(principal.userId()).isEqualTo("user-123");
			assertThat(principal.userName()).isEqualTo("TestUser");
			assertThat(principal.sessionId()).isEqualTo("session-abc");
		}

		@Test
		@DisplayName("valid token forwards to next filter")
		void forwardsToNextFilter() {
			String token = createValidToken();
			JwtAuthenticationGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data")
							.header("Authorization", "Bearer " + token));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));

			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
			Mono<Void> result = f.filter(exchange, capturingChain(captured));

			StepVerifier.create(result).verifyComplete();
			assertThat(captured.get()).isNotNull();
		}

		@Test
		@DisplayName("JWT_OR_HMAC mode with valid JWT succeeds")
		void jwtOrHmacModeSucceeds() {
			String token = createValidToken();
			JwtAuthenticationGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data")
							.header("Authorization", "Bearer " + token));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT_OR_HMAC, "test-route"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(f.filter(exchange, capturingChain(captured)))
					.verifyComplete();

			GatewayPrincipal.User principal = captured.get().getAttribute(GatewayPrincipal.ATTRIBUTE);
			assertThat(principal).isNotNull();
			assertThat(principal.userId()).isEqualTo("user-123");
		}
	}

	// ---------- auth mode passthrough ----------

	@Nested
	@DisplayName("Auth mode passthrough")
	class AuthModePassthrough {

		@Test
		@DisplayName("NONE auth mode passes through without authentication")
		void noneAuthModePassesThrough() {
			JwtAuthenticationGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/public/health"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.NONE, "PUBLIC:/health"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(f.filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertNull(captured.get().getAttribute(GatewayPrincipal.ATTRIBUTE));
		}

		@Test
		@DisplayName("HMAC auth mode passes through (handled by HMAC filter)")
		void hmacAuthModePassesThrough() {
			JwtAuthenticationGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/openapi/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.HMAC, "hmac-route"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(f.filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertNull(captured.get().getAttribute(GatewayPrincipal.ATTRIBUTE));
		}

		@Test
		@DisplayName("null policy passes through")
		void nullPolicyPassesThrough() {
			JwtAuthenticationGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data")
							.header("Authorization", "Bearer faketoken"));
			// No policy attribute set
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(f.filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertNull(captured.get().getAttribute(GatewayPrincipal.ATTRIBUTE));
		}
	}

	// ---------- error response format ----------

	@Nested
	@DisplayName("Error response format")
	class ErrorResponseFormat {

		@Test
		@DisplayName("error response is valid JSON with code and message")
		void errorResponseIsJson() {
			JwtAuthenticationGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));

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
			JwtAuthenticationGatewayFilter f = filter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "test-route"));

			StepVerifier.create(f.filter(exchange, passThroughChain()))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
		}
	}
}
