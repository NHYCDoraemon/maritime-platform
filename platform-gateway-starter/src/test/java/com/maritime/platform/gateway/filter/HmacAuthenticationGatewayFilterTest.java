package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.error.DefaultGatewayErrorWriter;
import com.maritime.platform.gateway.error.GatewayErrorWriter;
import com.maritime.platform.gateway.security.AuthMode;
import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.RouteSecurityPolicy;
import com.maritime.platform.gateway.security.hmac.HmacAuthenticationManager;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationException;

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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("HmacAuthenticationGatewayFilter tests")
@ExtendWith(MockitoExtension.class)
class HmacAuthenticationGatewayFilterTest {

	private static final String APP_KEY = "app-001";
	private static final String APP_CODE = "test-app";
	private static final String APP_ID = "app-id-001";
	private static final String TENANT_ID = "tenant-001";
	private static final String TENANT_CODE = "T001";
	private static final java.util.List<String> PERMISSIONS = java.util.List.of("read", "write");

	private static GatewayPrincipal.App testAppPrincipal() {
		return new GatewayPrincipal.App(APP_KEY, APP_CODE, APP_ID, TENANT_ID, TENANT_CODE, PERMISSIONS);
	}
	private static final String APP_KEY_HEADER = "X-App-Key";
	private static final String TIMESTAMP_HEADER = "X-Timestamp";
	private static final String NONCE_HEADER = "X-Nonce";
	private static final String BODY_DIGEST_HEADER = "X-Body-Digest";
	private static final String SIGNATURE_HEADER = "X-Signature";

	@Mock
	private HmacAuthenticationManager authManager;

	private GatewaySecurityProperties properties;

	@BeforeEach
	void setUp() {
		properties = new GatewaySecurityProperties();
		properties.getHmac().setEnabled(true);
	}

	private final GatewayErrorWriter errorWriter = new DefaultGatewayErrorWriter();

	private HmacAuthenticationGatewayFilter filter() {
		return new HmacAuthenticationGatewayFilter(authManager, properties, errorWriter);
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

	private MockServerWebExchange exchangeWithBody(String method, String path, String body) {
		MockServerHttpRequest request = MockServerHttpRequest
				.method(HttpMethod.valueOf(method), path)
				.header(APP_KEY_HEADER, APP_KEY)
				.header(TIMESTAMP_HEADER, "1700000000000")
				.header(NONCE_HEADER, "nonce-0123456789abcdef")
				.header(BODY_DIGEST_HEADER, "sha256-digest-placeholder")
				.header(SIGNATURE_HEADER, "signature-placeholder")
				.body(body != null ? body : "");
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
				new RouteSecurityPolicy(AuthMode.HMAC, "hmac-route"));
		return exchange;
	}

	private MockServerWebExchange exchange(String method, String path) {
		return exchangeWithBody(method, path, null);
	}

	// ---------- filter order ----------

	@Nested
	@DisplayName("Filter order")
	class FilterOrderTests {

		@Test
		@DisplayName("HmacAuthenticationGatewayFilter order is HMAC_AUTHENTICATION")
		void filterOrderIsHmacAuthentication() {
			Order ann = AnnotationUtils.findAnnotation(HmacAuthenticationGatewayFilter.class, Order.class);
			assertThat(ann).isNotNull();
			assertThat(ann.value()).isEqualTo(GatewayFilterOrder.HMAC_AUTHENTICATION);
		}

		@Test
		@DisplayName("HMAC_AUTHENTICATION is between JWT_NONCE and CONTEXT_HEADER_INJECTION")
		void orderPositionInCorrectRange() {
			assertThat(GatewayFilterOrder.HMAC_AUTHENTICATION)
					.isGreaterThan(GatewayFilterOrder.JWT_NONCE);
			assertThat(GatewayFilterOrder.HMAC_AUTHENTICATION)
					.isLessThan(GatewayFilterOrder.CONTEXT_HEADER_INJECTION);
		}

		@Test
		@DisplayName("filter implements Ordered")
		void implementsOrdered() {
			assertThat(filter()).isInstanceOf(Ordered.class);
		}
	}

	// ---------- missing HMAC headers ----------

	@Nested
	@DisplayName("Missing HMAC headers")
	class MissingHeaders {

		@Test
		@DisplayName("no HMAC headers returns MISSING_HMAC_HEADERS")
		void noHmacHeaders() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.HMAC, "hmac-route"));

			StepVerifier.create(filter().filter(exchange, passThroughChain()))
					.verifyComplete();

			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.MISSING_HMAC_HEADERS);
			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
		}

		@Test
		@DisplayName("missing app key header returns MISSING_HMAC_HEADERS")
		void missingAppKey() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data")
							.header(TIMESTAMP_HEADER, "1700000000000")
							.header(NONCE_HEADER, "nonce-0123456789abcdef")
							.header(BODY_DIGEST_HEADER, "digest")
							.header(SIGNATURE_HEADER, "sig"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.HMAC, "hmac-route"));

			StepVerifier.create(filter().filter(exchange, passThroughChain()))
					.verifyComplete();

			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.MISSING_HMAC_HEADERS);
		}

		@Test
		@DisplayName("blank app key header returns MISSING_HMAC_HEADERS")
		void blankAppKey() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data")
							.header(APP_KEY_HEADER, "  ")
							.header(TIMESTAMP_HEADER, "1700000000000")
							.header(NONCE_HEADER, "nonce-0123456789abcdef")
							.header(BODY_DIGEST_HEADER, "digest")
							.header(SIGNATURE_HEADER, "sig"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.HMAC, "hmac-route"));

			StepVerifier.create(filter().filter(exchange, passThroughChain()))
					.verifyComplete();

			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.MISSING_HMAC_HEADERS);
		}
	}

	// ---------- error propagation from manager ----------

	@Nested
	@DisplayName("Error propagation from authentication manager")
	class ErrorPropagation {

		@Test
		@DisplayName("timestamp expired from manager returns TIMESTAMP_EXPIRED")
		void timestampExpiredFromManager() {
			MockServerWebExchange exchange = exchange("POST", "/api/data");

			when(authManager.authenticate(any(), any(), any(),
					any(), any(), any(), any(), any(), any()))
					.thenReturn(Mono.error(new JwtAuthenticationException(
							GatewayAuthErrorCode.TIMESTAMP_EXPIRED,
							"Timestamp is outside the allowed tolerance window")));

			StepVerifier.create(filter().filter(exchange, passThroughChain()))
					.verifyComplete();

			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.TIMESTAMP_EXPIRED);
		}

		@Test
		@DisplayName("replay detected from manager returns REPLAY_DETECTED")
		void replayDetectedFromManager() {
			MockServerWebExchange exchange = exchange("POST", "/api/data");

			when(authManager.authenticate(any(), any(), any(),
					any(), any(), any(), any(), any(), any()))
					.thenReturn(Mono.error(new JwtAuthenticationException(
							GatewayAuthErrorCode.REPLAY_DETECTED,
							"Nonce already used")));

			StepVerifier.create(filter().filter(exchange, passThroughChain()))
					.verifyComplete();

			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.REPLAY_DETECTED);
		}

		@Test
		@DisplayName("invalid signature from manager returns INVALID_SIGNATURE")
		void invalidSignatureFromManager() {
			MockServerWebExchange exchange = exchange("POST", "/api/data");

			when(authManager.authenticate(any(), any(), any(),
					any(), any(), any(), any(), any(), any()))
					.thenReturn(Mono.error(new JwtAuthenticationException(
							GatewayAuthErrorCode.INVALID_SIGNATURE,
							"Signature does not match")));

			StepVerifier.create(filter().filter(exchange, passThroughChain()))
					.verifyComplete();

			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains(GatewayAuthErrorCode.INVALID_SIGNATURE);
		}
	}

	// ---------- successful authentication ----------

	@Nested
	@DisplayName("Successful authentication")
	class SuccessfulAuth {

		@Test
		@DisplayName("valid HMAC request stores App principal in exchange attribute")
		void storesAppPrincipalInExchange() {
			MockServerWebExchange exchange = exchange("POST", "/api/data");
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			GatewayPrincipal.App expectedPrincipal = testAppPrincipal();
			when(authManager.authenticate(any(), any(), any(), any(), any(), any(), any(), any(), any()))
					.thenReturn(Mono.just(expectedPrincipal));

			StepVerifier.create(filter().filter(exchange, capturingChain(captured)))
					.verifyComplete();

			GatewayPrincipal.App principal = captured.get().getAttribute(GatewayPrincipal.ATTRIBUTE);
			assertThat(principal).isNotNull();
			assertThat(principal.appKey()).isEqualTo(APP_KEY);
			assertThat(principal.appCode()).isEqualTo(APP_CODE);
		}

		@Test
		@DisplayName("JWT_OR_HMAC mode with HMAC succeeds")
		void jwtOrHmacModeSucceeds() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data")
							.header(APP_KEY_HEADER, APP_KEY)
							.header(TIMESTAMP_HEADER, "1700000000000")
							.header(NONCE_HEADER, "nonce-0123456789abcdef")
							.header(BODY_DIGEST_HEADER, "digest")
							.header(SIGNATURE_HEADER, "sig"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT_OR_HMAC, "dual-route"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			GatewayPrincipal.App expectedPrincipal = testAppPrincipal();
			when(authManager.authenticate(any(), any(), any(), any(), any(), any(), any(), any(), any()))
					.thenReturn(Mono.just(expectedPrincipal));

			StepVerifier.create(filter().filter(exchange, capturingChain(captured)))
					.verifyComplete();

			GatewayPrincipal.App principal = captured.get().getAttribute(GatewayPrincipal.ATTRIBUTE);
			assertThat(principal).isNotNull();
		}
	}

	// ---------- auth mode passthrough ----------

	@Nested
	@DisplayName("Auth mode passthrough")
	class AuthModePassthrough {

		@Test
		@DisplayName("NONE auth mode passes through without authentication")
		void noneAuthModePassesThrough() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/public/health"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.NONE, "PUBLIC:/health"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(filter().filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertNull(captured.get().getAttribute(GatewayPrincipal.ATTRIBUTE));
		}

		@Test
		@DisplayName("JWT auth mode passes through (handled by JWT filter)")
		void jwtAuthModePassesThrough() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.JWT, "jwt-route"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(filter().filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertNull(captured.get().getAttribute(GatewayPrincipal.ATTRIBUTE));
		}

		@Test
		@DisplayName("null policy passes through")
		void nullPolicyPassesThrough() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data")
							.header(APP_KEY_HEADER, "app-key"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			StepVerifier.create(filter().filter(exchange, capturingChain(captured)))
					.verifyComplete();

			assertNull(captured.get().getAttribute(GatewayPrincipal.ATTRIBUTE));
		}
	}

	// ---------- body caching ----------

	@Nested
	@DisplayName("Body caching for downstream")
	class BodyCaching {

		@Test
		@DisplayName("POST body is cached and available to downstream chain")
		void bodyCachedForDownstream() {
			String requestBody = "{\"key\":\"value\"}";
			MockServerWebExchange exchange = exchangeWithBody("POST", "/api/data", requestBody);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			GatewayPrincipal.App expectedPrincipal = testAppPrincipal();
			when(authManager.authenticate(any(), any(), any(),
					any(), any(), any(), any(), any(), any()))
					.thenReturn(Mono.just(expectedPrincipal));

			StepVerifier.create(filter().filter(exchange, capturingChain(captured)))
					.verifyComplete();

			ServerWebExchange downstream = captured.get();
			byte[] cachedBody = downstream.getAttribute("gateway.cachedBody");
			assertThat(cachedBody).isNotNull();
			assertThat(new String(cachedBody, StandardCharsets.UTF_8)).isEqualTo(requestBody);
		}

		@Test
		@DisplayName("GET request with no body caches empty array")
		void getRequestCachesEmptyBody() {
			MockServerWebExchange exchange = exchange("GET", "/api/data");
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			GatewayPrincipal.App expectedPrincipal = testAppPrincipal();
			when(authManager.authenticate(any(), any(), any(),
					any(), any(), any(), any(), any(), any()))
					.thenReturn(Mono.just(expectedPrincipal));

			StepVerifier.create(filter().filter(exchange, capturingChain(captured)))
					.verifyComplete();

			byte[] cachedBody = captured.get().getAttribute("gateway.cachedBody");
			assertThat(cachedBody).isNotNull();
			assertThat(cachedBody).isEmpty();
		}
	}

	// ---------- error response format ----------

	@Nested
	@DisplayName("Error response format")
	class ErrorResponseFormat {

		@Test
		@DisplayName("error response is valid JSON with code and message")
		void errorResponseIsJson() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.HMAC, "hmac-route"));

			StepVerifier.create(filter().filter(exchange, passThroughChain()))
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
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.post("/api/data"));
			exchange.getAttributes().put(RouteSecurityPolicyFilter.POLICY_ATTR,
					new RouteSecurityPolicy(AuthMode.HMAC, "hmac-route"));

			StepVerifier.create(filter().filter(exchange, passThroughChain()))
					.verifyComplete();

			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
		}
	}
}
