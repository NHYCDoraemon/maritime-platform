package com.maritime.platform.gateway.filter;

import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
@Order(GatewayFilterOrder.TRACE_ID)
public class TraceIdGatewayFilter implements GlobalFilter, Ordered {

	public static final String TRACE_ID_HEADER = "X-Trace-Id";

	public static final String TRACE_ID_ATTR = "gateway.traceId";

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
		if (traceId == null || traceId.isBlank()) {
			traceId = UUID.randomUUID().toString().replace("-", "");
		}
		exchange.getAttributes().put(TRACE_ID_ATTR, traceId);
		exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
		return chain.filter(exchange);
	}

	@Override
	public int getOrder() {
		return GatewayFilterOrder.TRACE_ID;
	}
}
