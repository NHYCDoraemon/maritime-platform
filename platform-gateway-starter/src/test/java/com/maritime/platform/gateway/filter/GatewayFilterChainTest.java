package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.security.GatewaySecurityPolicyCustomizer;
import com.maritime.platform.gateway.security.AuthMode;
import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.RouteSecurityPolicy;
import com.maritime.platform.gateway.security.RouteSecurityPolicyResolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Gateway filter chain tests")
class GatewayFilterChainTest {

	// ---------- filter order ----------

	@Nested
	@DisplayName("Filter order")
	class FilterOrder {

		@Test
		@DisplayName("TraceId order is HIGHEST_PRECEDENCE")
		void traceIdOrder() {
			Order ann = AnnotationUtils.findAnnotation(TraceIdGatewayFilter.class, Order.class);
			assertThat(ann).isNotNull();
			assertThat(ann.value()).isEqualTo(GatewayFilterOrder.TRACE_ID);
		}

		@Test
		@DisplayName("UntrustedHeaderStrip order is HIGHEST_PRECEDENCE + 5")
		void headerStripOrder() {
			Order ann = AnnotationUtils.findAnnotation(UntrustedHeaderStripFilter.class, Order.class);
			assertThat(ann).isNotNull();
			assertThat(ann.value()).isEqualTo(GatewayFilterOrder.UNTRUSTED_HEADER_STRIP);
		}

		@Test
		@DisplayName("RequestLog order is HIGHEST_PRECEDENCE + 10")
		void requestLogOrder() {
			Order ann = AnnotationUtils.findAnnotation(RequestLogGatewayFilter.class, Order.class);
			assertThat(ann).isNotNull();
			assertThat(ann.value()).isEqualTo(GatewayFilterOrder.REQUEST_LOG);
		}

		@Test
		@DisplayName("RouteSecurityPolicy order is HIGHEST_PRECEDENCE + 20")
		void securityPolicyOrder() {
			Order ann = AnnotationUtils.findAnnotation(RouteSecurityPolicyFilter.class, Order.class);
			assertThat(ann).isNotNull();
			assertThat(ann.value()).isEqualTo(GatewayFilterOrder.SECURITY_POLICY);
		}

		@Test
		@DisplayName("ContextHeaderInjection order is HIGHEST_PRECEDENCE + 30")
		void contextInjectionOrder() {
			Order ann = AnnotationUtils.findAnnotation(ContextHeaderInjectionFilter.class, Order.class);
			assertThat(ann).isNotNull();
			assertThat(ann.value()).isEqualTo(GatewayFilterOrder.CONTEXT_HEADER_INJECTION);
		}

		@Test
		@DisplayName("filters are in correct relative order")
		void correctRelativeOrder() {
			assertThat(GatewayFilterOrder.TRACE_ID)
					.isLessThan(GatewayFilterOrder.UNTRUSTED_HEADER_STRIP);
			assertThat(GatewayFilterOrder.UNTRUSTED_HEADER_STRIP)
					.isLessThan(GatewayFilterOrder.REQUEST_LOG);
			assertThat(GatewayFilterOrder.REQUEST_LOG)
					.isLessThan(GatewayFilterOrder.SECURITY_POLICY);
			assertThat(GatewayFilterOrder.SECURITY_POLICY)
					.isLessThan(GatewayFilterOrder.CONTEXT_HEADER_INJECTION);
		}

		@Test
		@DisplayName("all filters implement Ordered")
		void allImplementOrdered() {
			assertThat(new TraceIdGatewayFilter()).isInstanceOf(Ordered.class);
			assertThat(new UntrustedHeaderStripFilter()).isInstanceOf(Ordered.class);
			assertThat(new RequestLogGatewayFilter()).isInstanceOf(Ordered.class);
			assertThat(new ContextHeaderInjectionFilter(new TrustedHeaderWriter(), null)).isInstanceOf(Ordered.class);
		}

		@Test
		@DisplayName("order constants use HIGHEST_PRECEDENCE as base")
		void constantsUseHighestPrecedence() {
			assertThat(GatewayFilterOrder.TRACE_ID).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
			assertThat(GatewayFilterOrder.UNTRUSTED_HEADER_STRIP).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 5);
			assertThat(GatewayFilterOrder.REQUEST_LOG).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 10);
			assertThat(GatewayFilterOrder.SECURITY_POLICY).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 20);
			assertThat(GatewayFilterOrder.CONTEXT_HEADER_INJECTION).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 30);
		}
	}

	// ---------- TraceId ----------

	@Nested
	@DisplayName("TraceIdGatewayFilter")
	class TraceId {

		@Test
		@DisplayName("generates trace ID when header is missing")
		void generatesTraceIdWhenMissing() {
			TraceIdGatewayFilter filter = new TraceIdGatewayFilter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/test"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			filter.filter(exchange, capturingChain(captured)).block();

			String traceId = (String) captured.get().getAttribute(TraceIdGatewayFilter.TRACE_ID_ATTR);
			assertThat(traceId).isNotNull().isNotEmpty();
			assertThat(traceId).doesNotContain("-"); // UUID with dashes removed
		}

		@Test
		@DisplayName("reuses incoming X-Trace-Id header")
		void reusesIncomingTraceId() {
			TraceIdGatewayFilter filter = new TraceIdGatewayFilter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/test")
							.header(TraceIdGatewayFilter.TRACE_ID_HEADER, "abc123def"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			filter.filter(exchange, capturingChain(captured)).block();

			String traceId = (String) captured.get().getAttribute(TraceIdGatewayFilter.TRACE_ID_ATTR);
			assertThat(traceId).isEqualTo("abc123def");
		}

		@Test
		@DisplayName("sets X-Trace-Id on response")
		void setsResponseTraceId() {
			TraceIdGatewayFilter filter = new TraceIdGatewayFilter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/test"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			filter.filter(exchange, capturingChain(captured)).block();

			String responseHeader = captured.get().getResponse().getHeaders()
					.getFirst(TraceIdGatewayFilter.TRACE_ID_HEADER);
			assertThat(responseHeader).isNotNull().isNotEmpty();
		}

		@Test
		@DisplayName("produces unique IDs for different requests")
		void uniqueIds() {
			TraceIdGatewayFilter filter = new TraceIdGatewayFilter();
			AtomicReference<ServerWebExchange> c1 = new AtomicReference<>();
			AtomicReference<ServerWebExchange> c2 = new AtomicReference<>();

			filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/a")),
					capturingChain(c1)).block();
			filter.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/b")),
					capturingChain(c2)).block();

			String id1 = (String) c1.get().getAttribute(TraceIdGatewayFilter.TRACE_ID_ATTR);
			String id2 = (String) c2.get().getAttribute(TraceIdGatewayFilter.TRACE_ID_ATTR);
			assertThat(id1).isNotEqualTo(id2);
		}
	}

	// ---------- header strip ----------

	@Nested
	@DisplayName("UntrustedHeaderStripFilter")
	class HeaderStrip {

		@Test
		@DisplayName("strips trusted internal headers from request")
		void stripsTrustedHeaders() {
			UntrustedHeaderStripFilter filter = new UntrustedHeaderStripFilter();
			MockServerHttpRequest request = MockServerHttpRequest.get("/test")
					.header("X-Internal-Call", "true")
					.header("X-User-Id", "hacker")
					.header("X-Active-Org-Code", "evil-corp")
					.header("X-Session-Id", "fake-session")
					.header("X-System-Scope", "admin")
					.header("X-App-Key", "fake-key")
					.header("X-Trace-Id", "spoofed-trace")
					.header("X-User-Source", "internal")
					.header("X-Tenant-Id", "hacked-tenant")
					.header("X-Tenant-Code", "hacked")
					.header("X-App-Code", "admin-app")
					.header("X-App-Id", "1")
					.header("X-Verified-App-Code", "verified-fake")
					.header("X-App-Permissions", "all")
					.header("X-User-Name", "admin")
					.header("X-Active-Org-Name", "Evil Corp")
					// benign header that should pass through
					.header("Content-Type", "application/json")
					.header("Authorization", "Bearer token123")
					.build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			filter.filter(exchange, capturingChain(captured)).block();

			HttpHeaders headers = captured.get().getRequest().getHeaders();
			for (String untrusted : UntrustedHeaderStripFilter.UNTRUSTED_HEADERS) {
				assertThat(headers.getFirst(untrusted))
						.as("header '%s' should have been stripped", untrusted)
						.isNull();
			}
			assertThat(headers.getFirst("Content-Type")).isEqualTo("application/json");
			assertThat(headers.getFirst("Authorization")).isEqualTo("Bearer token123");
		}

		@Test
		@DisplayName("strips X-Internal-Call on public path")
		void stripsInternalCallOnPublicPath() {
			UntrustedHeaderStripFilter filter = new UntrustedHeaderStripFilter();
			MockServerHttpRequest request = MockServerHttpRequest.get("/public/health")
					.header("X-Internal-Call", "yes")
					.header("X-User-Id", "attacker")
					.build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			filter.filter(exchange, capturingChain(captured)).block();

			HttpHeaders headers = captured.get().getRequest().getHeaders();
			assertThat(headers.getFirst("X-Internal-Call")).isNull();
			assertThat(headers.getFirst("X-User-Id")).isNull();
		}

		@Test
		@DisplayName("request without internal headers passes through unchanged for benign headers")
		void noInternalHeadersPassesThrough() {
			UntrustedHeaderStripFilter filter = new UntrustedHeaderStripFilter();
			MockServerHttpRequest request = MockServerHttpRequest.get("/api/data")
					.header("Accept", "application/json")
					.header("X-Request-Id", "custom-id")
					.build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			filter.filter(exchange, capturingChain(captured)).block();

			HttpHeaders headers = captured.get().getRequest().getHeaders();
			assertThat(headers.getFirst("Accept")).isEqualTo("application/json");
			assertThat(headers.getFirst("X-Request-Id")).isEqualTo("custom-id");
		}

		@Test
		@DisplayName("HMAC signature header X-App-Key is preserved for downstream HMAC auth")
		void preservesHmacAppKeyHeader() {
			UntrustedHeaderStripFilter filter = new UntrustedHeaderStripFilter();
			MockServerHttpRequest request = MockServerHttpRequest.get("/api/data")
					.header("X-App-Key", "app-001")
					.header("X-Timestamp", "1700000000000")
					.header("X-Nonce", "nonce-0123456789abcdef")
					.header("X-Body-Digest", "sha256-digest")
					.header("X-Signature", "signature-value")
					.header("X-App-Code", "forged-app")
					.header("X-App-Id", "999")
					.header("X-Verified-App-Code", "forged-verified")
					.header("X-App-Permissions", "admin")
					.header("X-Tenant-Id", "fake-tenant")
					.header("X-Tenant-Code", "HACKED")
					.build();
			MockServerWebExchange exchange = MockServerWebExchange.from(request);
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			filter.filter(exchange, capturingChain(captured)).block();

			HttpHeaders headers = captured.get().getRequest().getHeaders();
			// HMAC signature headers must survive the strip so HMAC auth can read them
			assertThat(headers.getFirst("X-App-Key")).isEqualTo("app-001");
			assertThat(headers.getFirst("X-Timestamp")).isEqualTo("1700000000000");
			assertThat(headers.getFirst("X-Nonce")).isEqualTo("nonce-0123456789abcdef");
			assertThat(headers.getFirst("X-Body-Digest")).isEqualTo("sha256-digest");
			assertThat(headers.getFirst("X-Signature")).isEqualTo("signature-value");
			// Downstream context headers must still be stripped
			assertThat(headers.getFirst("X-App-Code")).isNull();
			assertThat(headers.getFirst("X-App-Id")).isNull();
			assertThat(headers.getFirst("X-Verified-App-Code")).isNull();
			assertThat(headers.getFirst("X-App-Permissions")).isNull();
			assertThat(headers.getFirst("X-Tenant-Id")).isNull();
			assertThat(headers.getFirst("X-Tenant-Code")).isNull();
		}

		@Test
		@DisplayName("all paths are filtered including public")
		void allPathsAreFiltered() {
			UntrustedHeaderStripFilter filter = new UntrustedHeaderStripFilter();
			for (String path : List.of("/", "/public/health", "/api/secure", "/actuator/info")) {
				MockServerHttpRequest request = MockServerHttpRequest.get(path)
						.header("X-Internal-Call", "1")
						.build();
				MockServerWebExchange exchange = MockServerWebExchange.from(request);
				AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

				filter.filter(exchange, capturingChain(captured)).block();

				assertThat(captured.get().getRequest().getHeaders().getFirst("X-Internal-Call"))
						.as("X-Internal-Call should be stripped on path: %s", path)
						.isNull();
			}
		}
	}

	// ---------- security policy filter ----------

	@Nested
	@DisplayName("RouteSecurityPolicyFilter")
	class SecurityPolicy {

		@Test
		@DisplayName("stores resolved policy in exchange attribute")
		void storesPolicyAttribute() {
			GatewaySecurityProperties props = new GatewaySecurityProperties();
			props.setDefaultAuthMode(AuthMode.JWT);
			props.getPublicPaths().add("/public/**");
			RouteSecurityPolicyResolver resolver = new RouteSecurityPolicyResolver(props);
			resolver.afterPropertiesSet();

			RouteSecurityPolicyFilter filter = new RouteSecurityPolicyFilter(resolver);
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/public/health"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			filter.filter(exchange, capturingChain(captured)).block();

			RouteSecurityPolicy policy = captured.get().getAttribute(RouteSecurityPolicyFilter.POLICY_ATTR);
			assertThat(policy).isNotNull();
			assertThat(policy.getAuthMode()).isEqualTo(AuthMode.NONE);
		}

		@Test
		@DisplayName("forwards request after storing policy")
		void forwardsAfterStoringPolicy() {
			GatewaySecurityProperties props = new GatewaySecurityProperties();
			RouteSecurityPolicyResolver resolver = new RouteSecurityPolicyResolver(props);
			resolver.afterPropertiesSet();

			RouteSecurityPolicyFilter filter = new RouteSecurityPolicyFilter(resolver);
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));

			Mono<Void> result = filter.filter(exchange, capturingChain(null));
			StepVerifier.create(result).verifyComplete();
		}

		@Test
		@DisplayName("public path with NONE auth mode passes through")
		void publicPathPassesThrough() {
			GatewaySecurityProperties props = new GatewaySecurityProperties();
			props.getPublicPaths().add("/health");
			RouteSecurityPolicyResolver resolver = new RouteSecurityPolicyResolver(props);
			resolver.afterPropertiesSet();

			RouteSecurityPolicyFilter filter = new RouteSecurityPolicyFilter(resolver);
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/health"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			filter.filter(exchange, capturingChain(captured)).block();

			RouteSecurityPolicy policy = captured.get().getAttribute(RouteSecurityPolicyFilter.POLICY_ATTR);
			assertThat(policy).isNotNull();
			assertThat(policy.getAuthMode()).isEqualTo(AuthMode.NONE);
		}
	}

	// ---------- context injection filter ----------

	@Nested
	@DisplayName("ContextHeaderInjectionFilter")
	class ContextInjection {

		@Test
		@DisplayName("no principal passes request through unchanged")
		void passesThroughWhenNoPrincipal() {
			TrustedHeaderWriter writer = new TrustedHeaderWriter();
			ContextHeaderInjectionFilter filter = new ContextHeaderInjectionFilter(writer, null);
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/data"));

			Mono<Void> result = filter.filter(exchange, capturingChain(null));
			StepVerifier.create(result).verifyComplete();
		}
	}

	// ---------- full chain simulation ----------

	@Nested
	@DisplayName("Full filter chain simulation")
	class FullChain {

		@Test
		@DisplayName("public path with NONE auth traverses all filters without error")
		void publicPathTraversesAllFilters() {
			GatewaySecurityProperties props = new GatewaySecurityProperties();
			props.getPublicPaths().add("/public/**");
			RouteSecurityPolicyResolver resolver = new RouteSecurityPolicyResolver(props);
			resolver.afterPropertiesSet();

			TraceIdGatewayFilter trace = new TraceIdGatewayFilter();
			UntrustedHeaderStripFilter strip = new UntrustedHeaderStripFilter();
			RequestLogGatewayFilter logFilter = new RequestLogGatewayFilter();
			RouteSecurityPolicyFilter security = new RouteSecurityPolicyFilter(resolver);
			TrustedHeaderWriter writer = new TrustedHeaderWriter();
			ContextHeaderInjectionFilter inject = new ContextHeaderInjectionFilter(writer, null);

			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/public/health")
							.header("X-Internal-Call", "1")
							.header("X-User-Id", "attacker"));

			// Run filters in order
			AtomicReference<ServerWebExchange> afterTrace = new AtomicReference<>();
			trace.filter(exchange, capturingChain(afterTrace)).block();
			ServerWebExchange e1 = afterTrace.get();

			AtomicReference<ServerWebExchange> afterStrip = new AtomicReference<>();
			strip.filter(e1, capturingChain(afterStrip)).block();
			ServerWebExchange e2 = afterStrip.get();

			AtomicReference<ServerWebExchange> afterLog = new AtomicReference<>();
			logFilter.filter(e2, capturingChain(afterLog)).block();
			ServerWebExchange e3 = afterLog.get();

			AtomicReference<ServerWebExchange> afterSecurity = new AtomicReference<>();
			security.filter(e3, capturingChain(afterSecurity)).block();
			ServerWebExchange e4 = afterSecurity.get();

			AtomicReference<ServerWebExchange> afterInjection = new AtomicReference<>();
			inject.filter(e4, capturingChain(afterInjection)).block();
			ServerWebExchange e5 = afterInjection.get();

			// TraceId is present
			assertThat((String) e5.getAttribute(TraceIdGatewayFilter.TRACE_ID_ATTR)).isNotNull();

			// Internal headers are stripped
			assertThat(e5.getRequest().getHeaders().getFirst("X-Internal-Call")).isNull();
			assertThat(e5.getRequest().getHeaders().getFirst("X-User-Id")).isNull();

			// Security policy resolved
			RouteSecurityPolicy policy = e5.getAttribute(RouteSecurityPolicyFilter.POLICY_ATTR);
			assertThat(policy).isNotNull();
			assertThat(policy.getAuthMode()).isEqualTo(AuthMode.NONE);

			// Response has trace ID
			assertThat(e5.getResponse().getHeaders().getFirst(TraceIdGatewayFilter.TRACE_ID_HEADER))
					.isNotNull();
		}

		@Test
		@DisplayName("header strip runs even when trace ID is missing")
		void headerStripRunsWithoutTraceId() {
			UntrustedHeaderStripFilter strip = new UntrustedHeaderStripFilter();
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/any/path")
							.header("X-Internal-Call", "1"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			strip.filter(exchange, capturingChain(captured)).block();

			assertThat(captured.get().getRequest().getHeaders().getFirst("X-Internal-Call")).isNull();
		}

		@Test
		@DisplayName("security policy stores correct matched rule id for resolved path")
		void policyRuleId() {
			GatewaySecurityProperties props = new GatewaySecurityProperties();
			props.getPublicPaths().add("/actuator/**");
			RouteSecurityPolicyResolver resolver = new RouteSecurityPolicyResolver(props);
			resolver.afterPropertiesSet();

			RouteSecurityPolicyFilter filter = new RouteSecurityPolicyFilter(resolver);
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/actuator/health"));
			AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

			filter.filter(exchange, capturingChain(captured)).block();

			RouteSecurityPolicy policy = captured.get().getAttribute(RouteSecurityPolicyFilter.POLICY_ATTR);
			assertThat(policy.getMatchedRuleId()).startsWith("PUBLIC:");
			assertThat(policy.getMatchedRuleId()).contains("/actuator/**");
		}

		// ---------- forged header strip + trusted injection end-to-end ----------

		@Test
		@DisplayName("forged JWT headers stripped, then gateway-injected headers prevail")
		void forgedJwtHeadersStrippedAndInjected() {
			UntrustedHeaderStripFilter strip = new UntrustedHeaderStripFilter();
			TrustedHeaderWriter writer = new TrustedHeaderWriter();
			ContextHeaderInjectionFilter inject = new ContextHeaderInjectionFilter(writer, null);

			GatewayPrincipal.User user = new GatewayPrincipal.User(
					"real-user", "RealName", "real-session",
					"ORG-REAL", "Real Org",
					List.of("user"),
					"SSO", "tenant-real");

			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/secure")
							.header("X-User-Id", "forged-user")
							.header("X-User-Name", "Hacker")
							.header("X-Session-Id", "fake-session")
							.header("X-Active-Org-Code", "EVIL")
							.header("X-Active-Org-Name", "Evil Corp")
							.header("X-Tenant-Id", "fake-tenant")
							.header("X-System-Scope", "admin,root")
							.header("X-User-Source", "internal"));

			// Step 1: strip untrusted headers
			AtomicReference<ServerWebExchange> afterStrip = new AtomicReference<>();
			strip.filter(exchange, capturingChain(afterStrip)).block();
			ServerWebExchange stripped = afterStrip.get();

			// All forged headers must be gone
			var strippedHeaders = stripped.getRequest().getHeaders();
			assertThat(strippedHeaders.getFirst("X-User-Id")).isNull();
			assertThat(strippedHeaders.getFirst("X-User-Name")).isNull();
			assertThat(strippedHeaders.getFirst("X-Session-Id")).isNull();

			// Step 2: set the verified principal
			GatewayPrincipal.User verifiedUser = new GatewayPrincipal.User(
					"real-user", "RealName", "real-session",
					"ORG-REAL", "Real Org",
					List.of("user"),
					"SSO", "tenant-real");
			stripped.getAttributes().put(GatewayPrincipal.ATTRIBUTE, verifiedUser);

			// Step 3: inject trusted headers
			AtomicReference<ServerWebExchange> afterInjection = new AtomicReference<>();
			inject.filter(stripped, capturingChain(afterInjection)).block();
			ServerWebExchange finalExchange = afterInjection.get();

			var finalHeaders = finalExchange.getRequest().getHeaders();
			assertThat(finalHeaders.getFirst("X-User-Id")).isEqualTo("real-user");
			assertThat(finalHeaders.getFirst("X-User-Name")).isEqualTo("RealName");
			assertThat(finalHeaders.getFirst("X-Session-Id")).isEqualTo("real-session");
			assertThat(finalHeaders.getFirst("X-Active-Org-Code")).isEqualTo("ORG-REAL");
			assertThat(finalHeaders.getFirst("X-Tenant-Id")).isEqualTo("tenant-real");
		}

		@Test
		@DisplayName("forged app headers stripped, then HMAC-injected headers prevail")
		void forgedAppHeadersStrippedAndInjected() {
			UntrustedHeaderStripFilter strip = new UntrustedHeaderStripFilter();
			TrustedHeaderWriter writer = new TrustedHeaderWriter();
			ContextHeaderInjectionFilter inject = new ContextHeaderInjectionFilter(writer, null);

			GatewayPrincipal.App app = new GatewayPrincipal.App(
					"real-app-key", "real-app-code", "real-app-id",
					"real-tenant", "RT001", List.of("read"));

			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/openapi/data")
							.header("X-App-Code", "forged-app")
							.header("X-App-Id", "999")
							.header("X-Tenant-Code", "HACKED")
							.header("X-Tenant-Id", "fake-tenant")
							.header("X-Verified-App-Code", "forged-verified")
							.header("X-App-Permissions", "admin,delete"));

			// Step 1: strip
			AtomicReference<ServerWebExchange> afterStrip = new AtomicReference<>();
			strip.filter(exchange, capturingChain(afterStrip)).block();
			ServerWebExchange stripped = afterStrip.get();

			assertThat(stripped.getRequest().getHeaders().getFirst("X-App-Code")).isNull();
			assertThat(stripped.getRequest().getHeaders().getFirst("X-App-Id")).isNull();

			// Step 2: set principal
			stripped.getAttributes().put(GatewayPrincipal.ATTRIBUTE, app);

			// Step 3: inject
			AtomicReference<ServerWebExchange> afterInjection = new AtomicReference<>();
			inject.filter(stripped, capturingChain(afterInjection)).block();
			ServerWebExchange finalExchange = afterInjection.get();

			var finalHeaders = finalExchange.getRequest().getHeaders();
			assertThat(finalHeaders.getFirst("X-Verified-App-Code")).isEqualTo("real-app-code");
			assertThat(finalHeaders.getFirst("X-App-Code")).isEqualTo("real-app-code");
			assertThat(finalHeaders.getFirst("X-App-Id")).isEqualTo("real-app-id");
			assertThat(finalHeaders.getFirst("X-Tenant-Code")).isEqualTo("RT001");
			assertThat(finalHeaders.getFirst("X-Tenant-Id")).isEqualTo("real-tenant");
			assertThat(finalHeaders.getFirst("X-App-Permissions")).isEqualTo("read");
		}

		@Test
		@DisplayName("forged headers stripped even when no auth principal (public paths)")
		void forgedHeadersStrippedEvenWithoutAuth() {
			UntrustedHeaderStripFilter strip = new UntrustedHeaderStripFilter();
			TrustedHeaderWriter writer = new TrustedHeaderWriter();
			ContextHeaderInjectionFilter inject = new ContextHeaderInjectionFilter(writer, null);

			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/public/health")
							.header("X-User-Id", "forged-user")
							.header("X-App-Code", "forged-app")
							.header("X-Tenant-Id", "fake-tenant"));

			// Strip
			AtomicReference<ServerWebExchange> afterStrip = new AtomicReference<>();
			strip.filter(exchange, capturingChain(afterStrip)).block();
			ServerWebExchange stripped = afterStrip.get();

			// No principal on public path
			assertThat((Object) stripped.getAttribute(GatewayPrincipal.ATTRIBUTE)).isNull();

			// Inject (should pass through since no principal)
			AtomicReference<ServerWebExchange> afterInjection = new AtomicReference<>();
			inject.filter(stripped, capturingChain(afterInjection)).block();
			ServerWebExchange finalExchange = afterInjection.get();

			var finalHeaders = finalExchange.getRequest().getHeaders();
			assertThat(finalHeaders.getFirst("X-User-Id")).isNull();
			assertThat(finalHeaders.getFirst("X-App-Code")).isNull();
			assertThat(finalHeaders.getFirst("X-Tenant-Id")).isNull();
		}
	}

	// ---------- helpers ----------

	private static GatewayFilterChain capturingChain(AtomicReference<ServerWebExchange> ref) {
		return exchange -> {
			if (ref != null) {
				ref.set(exchange);
			}
			return Mono.empty();
		};
	}
}
