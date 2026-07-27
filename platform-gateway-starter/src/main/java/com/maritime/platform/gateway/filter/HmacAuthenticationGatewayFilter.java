package com.maritime.platform.gateway.filter;

import com.maritime.platform.gateway.error.GatewayErrorWriter;
import com.maritime.platform.gateway.security.AuthMode;
import com.maritime.platform.gateway.security.GatewayPrincipal;
import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.RouteSecurityPolicy;
import com.maritime.platform.gateway.security.hmac.HmacAuthenticationManager;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationException;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Authenticates system-to-system requests using HMAC-SHA256 signatures.
 * <p>
 * Reads the request body once to compute the SHA-256 digest, then caches it
 * in a decorated exchange so downstream filters and handlers can still read it.
 * Stores the mapped {@link GatewayPrincipal.App} in the exchange.
 */
@Order(GatewayFilterOrder.HMAC_AUTHENTICATION)
public class HmacAuthenticationGatewayFilter implements GlobalFilter, Ordered {

	private final HmacAuthenticationManager authManager;
	private final GatewaySecurityProperties properties;
	private final GatewayErrorWriter errorWriter;

	public HmacAuthenticationGatewayFilter(HmacAuthenticationManager authManager,
			GatewaySecurityProperties properties, GatewayErrorWriter errorWriter) {
		this.authManager = authManager;
		this.properties = properties;
		this.errorWriter = errorWriter;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		RouteSecurityPolicy policy = exchange.getAttribute(RouteSecurityPolicyFilter.POLICY_ATTR);
		if (policy == null) {
			return chain.filter(exchange);
		}

		AuthMode mode = policy.getAuthMode();

		if (mode == AuthMode.JWT_AND_HMAC) {
			return errorWriter.write(exchange, GatewayAuthErrorCode.UNSUPPORTED_AUTH_MODE);
		}

		if (mode != AuthMode.HMAC && mode != AuthMode.JWT_OR_HMAC) {
			return chain.filter(stripHmacHeadersFromExchange(exchange));
		}

		// JWT_OR_HMAC: when a Bearer token is present, JWT handles auth;
		// skip HMAC to avoid interfering with the JWT path.
		if (mode == AuthMode.JWT_OR_HMAC && hasBearerToken(exchange)) {
			return chain.filter(stripHmacHeadersFromExchange(exchange));
		}

		GatewaySecurityProperties.Headers hdrs = properties.getHmac().getHeaders();
		String appKey = exchange.getRequest().getHeaders().getFirst(hdrs.getAppKey());
		String timestamp = exchange.getRequest().getHeaders().getFirst(hdrs.getTimestamp());
		String nonce = exchange.getRequest().getHeaders().getFirst(hdrs.getNonce());
		String bodyDigest = exchange.getRequest().getHeaders().getFirst(hdrs.getBodyDigest());
		String signature = exchange.getRequest().getHeaders().getFirst(hdrs.getSignature());

		if (isAnyBlank(appKey, timestamp, nonce, bodyDigest, signature)) {
			return errorWriter.write(exchange, GatewayAuthErrorCode.MISSING_HMAC_HEADERS);
		}

		return readAndCacheBody(exchange)
				.flatMap(cachedExchange -> {
					byte[] bodyBytes = cachedExchange.getAttribute("gateway.cachedBody");
					String method = exchange.getRequest().getMethod().name();
					String rawPath = exchange.getRequest().getURI().getRawPath();
					String rawQuery = exchange.getRequest().getURI().getRawQuery();

					return authManager.authenticate(appKey, timestamp, nonce, bodyDigest, signature,
									method, rawPath, rawQuery, bodyBytes != null ? bodyBytes : new byte[0])
							.flatMap(principal -> {
								cachedExchange.getAttributes().put(GatewayPrincipal.ATTRIBUTE, principal);
								// Strip raw HMAC signature headers before forwarding downstream
								ServerHttpRequest stripped = stripHmacHeaders(
										cachedExchange.getRequest(), hdrs, exchange);
								return chain.filter(cachedExchange.mutate().request(stripped).build());
							});
				})
				.onErrorResume(JwtAuthenticationException.class,
						e -> errorWriter.write(exchange, e.getErrorCode()));
	}

	@Override
	public int getOrder() {
		return GatewayFilterOrder.HMAC_AUTHENTICATION;
	}

	/**
	 * Removes raw HMAC signature headers from the request before forwarding
	 * downstream. Preserves any body decorator so the cached body stays readable.
	 */
	private ServerHttpRequest stripHmacHeaders(ServerHttpRequest request,
			GatewaySecurityProperties.Headers hdrs, ServerWebExchange exchange) {
		ServerHttpRequest headerStripped = request.mutate()
				.headers(h -> {
					h.remove(hdrs.getAppKey());
					h.remove(hdrs.getTimestamp());
					h.remove(hdrs.getNonce());
					h.remove(hdrs.getBodyDigest());
					h.remove(hdrs.getSignature());
				})
				.build();
		return new ServerHttpRequestDecorator(headerStripped) {
			@Override
			public Flux<DataBuffer> getBody() {
				return headerStripped.getBody();
			}
		};
	}

	/**
	 * Returns a new exchange whose request has all configured HMAC signature
	 * headers removed. Used on pass-through paths (NONE, JWT, JWT_OR_HMAC
	 * with Bearer) to ensure raw signature material never reaches downstream.
	 */
	private ServerWebExchange stripHmacHeadersFromExchange(ServerWebExchange exchange) {
		GatewaySecurityProperties.Headers hdrs = properties.getHmac().getHeaders();
		ServerHttpRequest stripped = exchange.getRequest().mutate()
				.headers(h -> {
					h.remove(hdrs.getAppKey());
					h.remove(hdrs.getTimestamp());
					h.remove(hdrs.getNonce());
					h.remove(hdrs.getBodyDigest());
					h.remove(hdrs.getSignature());
				})
				.build();
		return exchange.mutate().request(stripped).build();
	}

	/**
	 * Reads the request body once, caches the bytes, and returns a decorated exchange
	 * whose request body can be re-read by downstream filters and handlers.
	 */
	private Mono<ServerWebExchange> readAndCacheBody(ServerWebExchange exchange) {
		int maxBodyBytes = properties.getHmac().getMaxBodyBytes();
		return DataBufferUtils.join(exchange.getRequest().getBody())
				.map(buf -> {
					if (buf.readableByteCount() > maxBodyBytes) {
						DataBufferUtils.release(buf);
						throw new JwtAuthenticationException(
								GatewayAuthErrorCode.BODY_TOO_LARGE,
								"Request body exceeds maximum size of " + maxBodyBytes + " bytes");
					}
					byte[] bytes = new byte[buf.readableByteCount()];
					buf.read(bytes);
					DataBufferUtils.release(buf);
					return bytes;
				})
				.switchIfEmpty(Mono.just(new byte[0]))
				.map(bytes -> {
					ServerHttpRequest decorated = new ServerHttpRequestDecorator(exchange.getRequest()) {
						@Override
						public Flux<DataBuffer> getBody() {
							return Flux.just(
									exchange.getResponse().bufferFactory().wrap(bytes));
						}
					};
					ServerWebExchange cached = exchange.mutate().request(decorated).build();
					cached.getAttributes().put("gateway.cachedBody", bytes);
					return cached;
				});
	}

	private boolean hasBearerToken(ServerWebExchange exchange) {
		String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
		return auth != null && auth.startsWith("Bearer ");
	}

	private boolean isAnyBlank(String... values) {
		for (String v : values) {
			if (v == null || v.isBlank()) {
				return true;
			}
		}
		return false;
	}

}
