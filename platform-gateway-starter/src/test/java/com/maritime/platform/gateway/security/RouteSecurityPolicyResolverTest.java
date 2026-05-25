package com.maritime.platform.gateway.security;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RouteSecurityPolicyResolver tests")
class RouteSecurityPolicyResolverTest {

	// ---------- helpers ----------

	private static GatewaySecurityProperties propertiesWithDefault(AuthMode defaultMode) {
		GatewaySecurityProperties props = new GatewaySecurityProperties();
		props.setDefaultAuthMode(defaultMode);
		return props;
	}

	private static void addPublicPath(GatewaySecurityProperties props, String path) {
		props.getPublicPaths().add(path);
	}

	private static void addRoute(GatewaySecurityProperties props, String id,
	                             List<String> paths, List<String> methods, AuthMode mode) {
		GatewaySecurityProperties.RoutePolicy rp = new GatewaySecurityProperties.RoutePolicy();
		rp.setId(id);
		rp.setPaths(paths);
		if (methods != null) {
			rp.setMethods(methods);
		}
		rp.setAuthMode(mode);
		props.getRoutes().add(rp);
	}

	private RouteSecurityPolicy resolve(GatewaySecurityProperties props, String path, String method) {
		RouteSecurityPolicyResolver resolver = new RouteSecurityPolicyResolver(props);
		resolver.afterPropertiesSet();
		return resolver.resolve(path, method);
	}

	private RouteSecurityPolicy resolveWithCustomizers(GatewaySecurityProperties props,
	                                                   String path, String method,
	                                                   List<GatewaySecurityPolicyCustomizer> customizers) {
		RouteSecurityPolicyResolver resolver = new RouteSecurityPolicyResolver(props, customizers);
		resolver.afterPropertiesSet();
		return resolver.resolve(path, method);
	}

	// ---------- public path tests ----------

	@Nested
	@DisplayName("Public path priority")
	class PublicPathPriority {

		@Test
		@DisplayName("exact public path resolves to NONE")
		void exactMatch() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.JWT);
			addPublicPath(props, "/health");

			RouteSecurityPolicy result = resolve(props, "/health", "GET");

			assertThat(result.getAuthMode()).isEqualTo(AuthMode.NONE);
			assertThat(result.getMatchedRuleId()).startsWith("PUBLIC:");
		}

		@Test
		@DisplayName("wildcard public path resolves to NONE")
		void wildcardMatch() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.JWT);
			addPublicPath(props, "/actuator/**");

			RouteSecurityPolicy result = resolve(props, "/actuator/health", "GET");

			assertThat(result.getAuthMode()).isEqualTo(AuthMode.NONE);
		}

		@Test
		@DisplayName("public path always NONE regardless of HTTP method")
		void anyMethodReturnsNone() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.JWT);
			addPublicPath(props, "/health");

			assertThat(resolve(props, "/health", "GET").getAuthMode()).isEqualTo(AuthMode.NONE);
			assertThat(resolve(props, "/health", "POST").getAuthMode()).isEqualTo(AuthMode.NONE);
			assertThat(resolve(props, "/health", "DELETE").getAuthMode()).isEqualTo(AuthMode.NONE);
			assertThat(resolve(props, "/health", null).getAuthMode()).isEqualTo(AuthMode.NONE);
		}

		@Test
		@DisplayName("public path overrides route policies")
		void overrideRoutePolicies() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.HMAC);
			addPublicPath(props, "/api/public/**");
			addRoute(props, "secure-api", List.of("/api/**"), null, AuthMode.JWT);

			RouteSecurityPolicy result = resolve(props, "/api/public/data", "GET");

			assertThat(result.getAuthMode()).isEqualTo(AuthMode.NONE);
			assertThat(result.getMatchedRuleId()).startsWith("PUBLIC:");
		}

		@Test
		@DisplayName("multiple public paths all resolve to NONE")
		void multiplePublicPaths() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.JWT);
			addPublicPath(props, "/health");
			addPublicPath(props, "/swagger-ui/**");

			assertThat(resolve(props, "/health", "GET").getAuthMode()).isEqualTo(AuthMode.NONE);
			assertThat(resolve(props, "/swagger-ui/index.html", "GET").getAuthMode()).isEqualTo(AuthMode.NONE);
		}
	}

	// ---------- route policy tests ----------

	@Nested
	@DisplayName("Route policy matching")
	class RoutePolicyMatching {

		@Test
		@DisplayName("route policy matches by path")
		void pathMatch() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.NONE);
			addRoute(props, "app-api", List.of("/api/**"), null, AuthMode.JWT);

			RouteSecurityPolicy result = resolve(props, "/api/users", "GET");

			assertThat(result.getAuthMode()).isEqualTo(AuthMode.JWT);
			assertThat(result.getMatchedRuleId()).isEqualTo("app-api");
		}

		@Test
		@DisplayName("route policy with methods filters by HTTP method")
		void methodFilterMatch() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.NONE);
			addRoute(props, "write-api", List.of("/api/**"), List.of("POST", "PUT", "DELETE"), AuthMode.HMAC);

			assertThat(resolve(props, "/api/data", "POST").getAuthMode()).isEqualTo(AuthMode.HMAC);
			assertThat(resolve(props, "/api/data", "PUT").getAuthMode()).isEqualTo(AuthMode.HMAC);
			assertThat(resolve(props, "/api/data", "DELETE").getAuthMode()).isEqualTo(AuthMode.HMAC);
		}

		@Test
		@DisplayName("route policy with methods does not match non-listed methods")
		void methodFilterNoMatch() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.JWT);
			addRoute(props, "write-api", List.of("/api/**"), List.of("POST", "PUT"), AuthMode.HMAC);

			RouteSecurityPolicy result = resolve(props, "/api/data", "GET");

			assertThat(result.getAuthMode()).isEqualTo(AuthMode.JWT);
			assertThat(result.getMatchedRuleId()).isEqualTo("DEFAULT");
		}

		@Test
		@DisplayName("same path with different methods can have different policies")
		void samePathDifferentMethods() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.JWT);
			addRoute(props, "read-api", List.of("/api/data"), List.of("GET", "HEAD"), AuthMode.NONE);
			addRoute(props, "write-api", List.of("/api/data"), List.of("POST", "PUT", "DELETE"), AuthMode.HMAC);

			assertThat(resolve(props, "/api/data", "GET").getAuthMode()).isEqualTo(AuthMode.NONE);
			assertThat(resolve(props, "/api/data", "HEAD").getAuthMode()).isEqualTo(AuthMode.NONE);
			assertThat(resolve(props, "/api/data", "POST").getAuthMode()).isEqualTo(AuthMode.HMAC);
			assertThat(resolve(props, "/api/data", "PUT").getAuthMode()).isEqualTo(AuthMode.HMAC);
			assertThat(resolve(props, "/api/data", "DELETE").getAuthMode()).isEqualTo(AuthMode.HMAC);
		}

		@Test
		@DisplayName("route policy without methods matches any HTTP method")
		void noMethodRestriction() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.NONE);
			addRoute(props, "all-api", List.of("/api/**"), null, AuthMode.JWT);

			assertThat(resolve(props, "/api/x", "GET").getAuthMode()).isEqualTo(AuthMode.JWT);
			assertThat(resolve(props, "/api/x", "POST").getAuthMode()).isEqualTo(AuthMode.JWT);
			assertThat(resolve(props, "/api/x", "DELETE").getAuthMode()).isEqualTo(AuthMode.JWT);
		}

		@Test
		@DisplayName("route policy with empty methods list matches any method")
		void emptyMethodList() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.NONE);
			addRoute(props, "any-method", List.of("/api/**"), Collections.emptyList(), AuthMode.JWT);

			assertThat(resolve(props, "/api/x", "GET").getAuthMode()).isEqualTo(AuthMode.JWT);
			assertThat(resolve(props, "/api/x", "POST").getAuthMode()).isEqualTo(AuthMode.JWT);
		}

		@Test
		@DisplayName("multiple route policies matched in registration order")
		void firstMatchWins() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.NONE);
			addRoute(props, "catch-all", List.of("/**"), null, AuthMode.JWT_OR_HMAC);
			addRoute(props, "specific", List.of("/api/specific"), null, AuthMode.JWT);

			// catch-all registered first, so it matches even though specific is more specific
			RouteSecurityPolicy result = resolve(props, "/api/specific", "GET");
			assertThat(result.getMatchedRuleId()).isEqualTo("catch-all");
			assertThat(result.getAuthMode()).isEqualTo(AuthMode.JWT_OR_HMAC);
		}

		@Test
		@DisplayName("route policy matches nested paths with wildcard")
		void nestedWildcardMatch() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.NONE);
			addRoute(props, "deep-api", List.of("/api/v1/**"), null, AuthMode.JWT);

			assertThat(resolve(props, "/api/v1/users", "GET").getAuthMode()).isEqualTo(AuthMode.JWT);
			assertThat(resolve(props, "/api/v1/users/123/profile", "GET").getAuthMode()).isEqualTo(AuthMode.JWT);
		}

		@Test
		@DisplayName("route policy matches path variables")
		void pathVariableMatch() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.NONE);
			addRoute(props, "user-api", List.of("/api/users/{id}"), null, AuthMode.JWT);

			assertThat(resolve(props, "/api/users/42", "GET").getAuthMode()).isEqualTo(AuthMode.JWT);
		}
	}

	// ---------- default fallback tests ----------

	@Nested
	@DisplayName("Default fallback")
	class DefaultFallback {

		@Test
		@DisplayName("no match falls back to default auth mode")
		void noMatchUsesDefault() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.HMAC);

			RouteSecurityPolicy result = resolve(props, "/anything", "GET");

			assertThat(result.getAuthMode()).isEqualTo(AuthMode.HMAC);
			assertThat(result.getMatchedRuleId()).isEqualTo("DEFAULT");
		}

		@Test
		@DisplayName("empty config uses default for everything")
		void emptyConfigUsesDefault() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.JWT);

			assertThat(resolve(props, "/path1", "GET").getAuthMode()).isEqualTo(AuthMode.JWT);
			assertThat(resolve(props, "/path2", "POST").getAuthMode()).isEqualTo(AuthMode.JWT);
		}

		@Test
		@DisplayName("custom default auth mode is honored")
		void customDefaultAuthMode() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.HMAC);

			assertThat(resolve(props, "/unmatched", "GET").getAuthMode()).isEqualTo(AuthMode.HMAC);
		}
	}

	// ---------- customizer tests ----------

	@Nested
	@DisplayName("GatewaySecurityPolicyCustomizer extension")
	class CustomizerExtension {

		@Test
		@DisplayName("customizer can add public paths")
		void customizerAddsPublicPath() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.JWT);
			List<GatewaySecurityPolicyCustomizer> customizers = List.of(resolver -> {
				resolver.addPublicPath("/custom-public/**");
			});

			RouteSecurityPolicy result = resolveWithCustomizers(props, "/custom-public/data", "POST", customizers);

			assertThat(result.getAuthMode()).isEqualTo(AuthMode.NONE);
			assertThat(result.getMatchedRuleId()).contains("custom");
		}

		@Test
		@DisplayName("customizer can add route policies")
		void customizerAddsRoutePolicy() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.NONE);
			List<GatewaySecurityPolicyCustomizer> customizers = List.of(resolver -> {
				resolver.addRoutePolicy("custom-route", List.of("/custom/**"),
						List.of("POST"), AuthMode.HMAC);
			});

			RouteSecurityPolicy result = resolveWithCustomizers(props, "/custom/action", "POST", customizers);

			assertThat(result.getAuthMode()).isEqualTo(AuthMode.HMAC);
			assertThat(result.getMatchedRuleId()).isEqualTo("custom-route");
		}

		@Test
		@DisplayName("customizer public paths override properties-defined route policies")
		void customizerPublicPathOverridesRoutes() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.HMAC);
			addRoute(props, "secure", List.of("/api/**"), null, AuthMode.JWT);
			List<GatewaySecurityPolicyCustomizer> customizers = List.of(resolver -> {
				resolver.addPublicPath("/api/open/**");
			});

			RouteSecurityPolicy result = resolveWithCustomizers(props, "/api/open/data", "GET", customizers);

			assertThat(result.getAuthMode()).isEqualTo(AuthMode.NONE);
		}

		@Test
		@DisplayName("multiple customizers are all applied")
		void multipleCustomizers() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.JWT);
			List<GatewaySecurityPolicyCustomizer> customizers = List.of(
					resolver -> resolver.addPublicPath("/open/**"),
					resolver -> resolver.addRoutePolicy("extra", List.of("/extra/**"), null, AuthMode.NONE)
			);

			assertThat(resolveWithCustomizers(props, "/open/data", "GET", customizers)
					.getAuthMode()).isEqualTo(AuthMode.NONE);
			assertThat(resolveWithCustomizers(props, "/extra/data", "GET", customizers)
					.getAuthMode()).isEqualTo(AuthMode.NONE);
		}

		@Test
		@DisplayName("customizer route policy with method restriction")
		void customizerMethodRestriction() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.JWT);
			List<GatewaySecurityPolicyCustomizer> customizers = List.of(resolver -> {
				resolver.addRoutePolicy("read-only", List.of("/data/**"),
						List.of("GET", "HEAD"), AuthMode.NONE);
			});

			assertThat(resolveWithCustomizers(props, "/data/item", "GET", customizers)
					.getAuthMode()).isEqualTo(AuthMode.NONE);
			assertThat(resolveWithCustomizers(props, "/data/item", "POST", customizers)
					.getAuthMode()).isEqualTo(AuthMode.JWT);
		}

		@Test
		@DisplayName("addRoutePolicy with JWT_AND_HMAC throws IllegalArgumentException")
		void addRoutePolicyRejectsJwtAndHmac() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.JWT);
			List<GatewaySecurityPolicyCustomizer> customizers = List.of(resolver -> {
				assertThatThrownBy(() -> resolver.addRoutePolicy("dual",
						List.of("/dual/**"), null, AuthMode.JWT_AND_HMAC))
						.isInstanceOf(IllegalArgumentException.class)
						.hasMessageContaining("JWT_AND_HMAC");
			});
			resolveWithCustomizers(props, "/other", "GET", customizers);
		}

		@Test
		@DisplayName("addRoutePolicy with null authMode throws IllegalArgumentException")
		void addRoutePolicyRejectsNullAuthMode() {
			GatewaySecurityProperties props = propertiesWithDefault(AuthMode.JWT);
			List<GatewaySecurityPolicyCustomizer> customizers = List.of(resolver -> {
				assertThatThrownBy(() -> resolver.addRoutePolicy("null-mode",
						List.of("/null/**"), null, null))
						.isInstanceOf(IllegalArgumentException.class)
						.hasMessageContaining("null-mode")
						.hasMessageContaining("must not be null");
			});
			resolveWithCustomizers(props, "/other", "GET", customizers);
		}
	}

	// ---------- RouteSecurityPolicy value object ----------

	@Nested
	@DisplayName("RouteSecurityPolicy value object")
	class RouteSecurityPolicyValue {

		@Test
		@DisplayName("equals and hashCode work correctly")
		void equality() {
			RouteSecurityPolicy p1 = new RouteSecurityPolicy(AuthMode.JWT, "test-rule");
			RouteSecurityPolicy p2 = new RouteSecurityPolicy(AuthMode.JWT, "test-rule");
			RouteSecurityPolicy p3 = new RouteSecurityPolicy(AuthMode.NONE, "test-rule");
			RouteSecurityPolicy p4 = new RouteSecurityPolicy(AuthMode.JWT, "other-rule");

			assertThat(p1).isEqualTo(p2);
			assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
			assertThat(p1).isNotEqualTo(p3);
			assertThat(p1).isNotEqualTo(p4);
		}

		@Test
		@DisplayName("toString includes auth mode and matched rule")
		void stringRepresentation() {
			RouteSecurityPolicy policy = new RouteSecurityPolicy(AuthMode.HMAC, "app-route");
			assertThat(policy.toString()).contains("HMAC").contains("app-route");
		}
	}
}
