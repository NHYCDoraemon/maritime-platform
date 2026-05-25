package com.maritime.platform.gateway.error;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Writes authentication error responses in a consistent JSON format.
 * <p>
 * Override by registering a custom {@link GatewayErrorWriter} bean.
 */
@FunctionalInterface
public interface GatewayErrorWriter {

	/**
	 * Write an authentication error response.
	 *
	 * @param exchange  the current exchange
	 * @param errorCode the gateway auth error code (e.g. {@code MISSING_TOKEN})
	 * @return a Mono that completes after the response is written
	 */
	Mono<Void> write(ServerWebExchange exchange, String errorCode);
}
