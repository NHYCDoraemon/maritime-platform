package com.maritime.platform.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
@Order(GatewayFilterOrder.REQUEST_LOG)
public class RequestLogGatewayFilter implements GlobalFilter, Ordered {

	private static final Logger log = LoggerFactory.getLogger(RequestLogGatewayFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		if (log.isDebugEnabled()) {
			ServerHttpRequest request = exchange.getRequest();
			String traceId = (String) exchange.getAttribute(TraceIdGatewayFilter.TRACE_ID_ATTR);
			log.debug("method={} path={} traceId={}",
					request.getMethod(), request.getURI().getPath(), traceId);
		}
		return chain.filter(exchange);
	}

	@Override
	public int getOrder() {
		return GatewayFilterOrder.REQUEST_LOG;
	}
}
