package com.maritime.platform.gateway.filter;

import org.springframework.core.Ordered;

/**
 * Defines the execution order for the gateway filter chain.
 * <p>
 * Lower values run first. TraceId must run before header strip
 * so the incoming trace ID can be captured before it is removed.
 * Header strip must run before all other logic so no filter sees
 * client-supplied internal headers.
 */
public final class GatewayFilterOrder {

	private GatewayFilterOrder() {
	}

	public static final int TRACE_ID = Ordered.HIGHEST_PRECEDENCE;

	public static final int UNTRUSTED_HEADER_STRIP = Ordered.HIGHEST_PRECEDENCE + 5;

	public static final int REQUEST_LOG = Ordered.HIGHEST_PRECEDENCE + 10;

	public static final int SECURITY_POLICY = Ordered.HIGHEST_PRECEDENCE + 20;

	public static final int JWT_AUTHENTICATION = Ordered.HIGHEST_PRECEDENCE + 25;

	public static final int CONTEXT_HEADER_INJECTION = Ordered.HIGHEST_PRECEDENCE + 30;
}
