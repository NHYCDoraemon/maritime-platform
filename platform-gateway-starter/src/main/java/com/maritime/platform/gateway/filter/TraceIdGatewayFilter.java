package com.maritime.platform.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Order(GatewayFilterOrder.TRACE_ID)
public class TraceIdGatewayFilter implements GlobalFilter, Ordered {

	public static final String TRACE_ID_HEADER = "X-Trace-Id";

	public static final String TRACE_ID_ATTR = "gateway.traceId";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String raw = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
		String traceId = TraceIdNormalizer.normalize(raw);
		exchange.getAttributes().put(TRACE_ID_ATTR, traceId);
		exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
		return chain.filter(exchange);
	}

	@Override
	public int getOrder() {
		return GatewayFilterOrder.TRACE_ID;
	}
}
