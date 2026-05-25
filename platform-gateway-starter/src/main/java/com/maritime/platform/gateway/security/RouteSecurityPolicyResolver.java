package com.maritime.platform.gateway.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

@Component
public class RouteSecurityPolicyResolver implements InitializingBean {

	private final GatewaySecurityProperties properties;

	private final List<GatewaySecurityPolicyCustomizer> customizers;

	private final List<String> programmaticPublicPaths = new ArrayList<>();

	private final List<ProgrammaticRouteRule> programmaticRoutes = new ArrayList<>();

	private List<CompiledPublicPath> compiledPublicPaths = Collections.emptyList();

	private List<CompiledRouteRule> compiledRoutes = Collections.emptyList();

	private AuthMode defaultAuthMode;

	private volatile boolean initialized;

	public RouteSecurityPolicyResolver(GatewaySecurityProperties properties) {
		this(properties, Collections.emptyList());
	}

	@Autowired
	public RouteSecurityPolicyResolver(GatewaySecurityProperties properties,
	                                   List<GatewaySecurityPolicyCustomizer> customizers) {
		this.properties = properties;
		this.customizers = customizers != null ? customizers : Collections.emptyList();
	}

	// ---------- programmatic registration (for customizers) ----------

	public void addPublicPath(String publicPath) {
		checkNotInitialized();
		programmaticPublicPaths.add(publicPath);
	}

	public void addRoutePolicy(String id, List<String> paths, List<String> methods, AuthMode authMode) {
		checkNotInitialized();
		if (authMode == AuthMode.JWT_AND_HMAC) {
			throw new IllegalArgumentException(
					"Route '" + id + "' uses JWT_AND_HMAC which is reserved and not yet implemented");
		}
		programmaticRoutes.add(new ProgrammaticRouteRule(id, paths, methods, authMode));
	}

	// ---------- resolution ----------

	public RouteSecurityPolicy resolve(ServerHttpRequest request) {
		return resolve(request.getURI().getPath(), request.getMethod() != null ? request.getMethod().name() : null);
	}

	public RouteSecurityPolicy resolve(String path, String httpMethod) {
		if (!initialized) {
			compile();
		}
		// 1. Public paths take absolute priority — always NONE
		for (CompiledPublicPath cpp : compiledPublicPaths) {
			if (cpp.pattern.matches(PathContainerAdapter.parse(path))) {
				return new RouteSecurityPolicy(AuthMode.NONE, cpp.ruleId);
			}
		}
		// 2. Route policies
		for (CompiledRouteRule crr : compiledRoutes) {
			if (crr.matches(path, httpMethod)) {
				return new RouteSecurityPolicy(crr.authMode, crr.ruleId);
			}
		}
		// 3. Default
		return new RouteSecurityPolicy(defaultAuthMode, "DEFAULT");
	}

	public AuthMode getDefaultAuthMode() {
		return defaultAuthMode;
	}

	// ---------- initialization ----------

	@Override
	public void afterPropertiesSet() {
		compile();
	}

	private void compile() {
		if (initialized) {
			return;
		}
		PathPatternParser parser = new PathPatternParser();
		parser.setMatchOptionalTrailingSeparator(true);

		List<CompiledPublicPath> publicPatterns = new ArrayList<>();
		List<CompiledRouteRule> routeRules = new ArrayList<>();

		// Apply customizers to collect programmatic registrations
		for (GatewaySecurityPolicyCustomizer customizer : customizers) {
			customizer.customize(this);
		}

		// Compile public paths from properties
		for (String path : properties.getPublicPaths()) {
			publicPatterns.add(new CompiledPublicPath(compilePattern(parser, path), "PUBLIC:" + path));
		}
		// Compile programmatic public paths
		for (String path : programmaticPublicPaths) {
			publicPatterns.add(new CompiledPublicPath(compilePattern(parser, path), "PUBLIC(custom):" + path));
		}

		// Compile route policies from properties
		for (GatewaySecurityProperties.RoutePolicy rp : properties.getRoutes()) {
			Set<String> methods = uppercaseSet(rp.getMethods());
			for (String path : rp.getPaths()) {
				routeRules.add(new CompiledRouteRule(
						compilePattern(parser, path), rp.getId(), methods, rp.getAuthMode()));
			}
		}
		// Compile programmatic route policies
		for (ProgrammaticRouteRule pr : programmaticRoutes) {
			Set<String> methods = uppercaseSet(pr.methods);
			for (String path : pr.paths) {
				routeRules.add(new CompiledRouteRule(
						compilePattern(parser, path), pr.id, methods, pr.authMode));
			}
		}

		this.compiledPublicPaths = Collections.unmodifiableList(publicPatterns);
		this.compiledRoutes = Collections.unmodifiableList(routeRules);
		this.defaultAuthMode = properties.getDefaultAuthMode();
		this.initialized = true;
	}

	private static PathPattern compilePattern(PathPatternParser parser, String pattern) {
		try {
			return parser.parse(pattern);
		} catch (PatternParseException e) {
			throw new IllegalStateException("Invalid path pattern '" + pattern + "': " + e.getMessage(), e);
		}
	}

	private static Set<String> uppercaseSet(List<String> values) {
		if (values == null || values.isEmpty()) {
			return Collections.emptySet();
		}
		Set<String> set = new HashSet<>(values.size());
		for (String v : values) {
			if (v != null && !v.isBlank()) {
				set.add(v.strip().toUpperCase(Locale.ROOT));
			}
		}
		return Collections.unmodifiableSet(set);
	}

	private void checkNotInitialized() {
		if (initialized) {
			throw new IllegalStateException(
					"RouteSecurityPolicyResolver is already initialized. " +
					"addPublicPath / addRoutePolicy must be called from a GatewaySecurityPolicyCustomizer.");
		}
	}

	// ---------- internal types ----------

	private static class CompiledPublicPath {
		final PathPattern pattern;
		final String ruleId;

		CompiledPublicPath(PathPattern pattern, String ruleId) {
			this.pattern = pattern;
			this.ruleId = ruleId;
		}
	}

	private static class CompiledRouteRule {
		final PathPattern pattern;
		final String ruleId;
		final Set<String> methods;
		final AuthMode authMode;

		CompiledRouteRule(PathPattern pattern, String ruleId, Set<String> methods, AuthMode authMode) {
			this.pattern = pattern;
			this.ruleId = ruleId;
			this.methods = methods;
			this.authMode = authMode;
		}

		boolean matches(String path, String httpMethod) {
			if (!this.pattern.matches(PathContainerAdapter.parse(path))) {
				return false;
			}
			if (methods.isEmpty()) {
				return true;
			}
			if (httpMethod == null) {
				return false;
			}
			return methods.contains(httpMethod.toUpperCase(Locale.ROOT));
		}
	}

	private static class ProgrammaticRouteRule {
		final String id;
		final List<String> paths;
		final List<String> methods;
		final AuthMode authMode;

		ProgrammaticRouteRule(String id, List<String> paths, List<String> methods, AuthMode authMode) {
			this.id = id;
			this.paths = paths != null ? paths : Collections.emptyList();
			this.methods = methods != null ? methods : Collections.emptyList();
			this.authMode = authMode;
		}
	}

	/**
	/**
	 * Adapter that bridges a string path to {@link org.springframework.http.server.PathContainer}
	 * without requiring the spring-web dependency at the API level.
	 */
	private static final class PathContainerAdapter {
		private PathContainerAdapter() {}

		static org.springframework.http.server.PathContainer parse(String path) {
			return org.springframework.http.server.PathContainer.parsePath(path);
		}
	}
}
