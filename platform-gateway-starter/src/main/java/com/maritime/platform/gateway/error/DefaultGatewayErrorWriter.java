package com.maritime.platform.gateway.error;

import com.maritime.platform.gateway.filter.TraceIdGatewayFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Default implementation of {@link GatewayErrorWriter}.
 * <p>
 * Produces JSON responses in the form
 * {@code {"code":401,"message":"MISSING_TOKEN","data":null}}.
 * The response includes an {@code X-Trace-Id} header when available.
 */
public class DefaultGatewayErrorWriter implements GatewayErrorWriter {

	@Override
	public Mono<Void> write(ServerWebExchange exchange, String errorCode) {
		HttpStatus status = resolveStatus(errorCode);
		exchange.getResponse().setStatusCode(status);
		exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

		String traceId = exchange.getAttribute(TraceIdGatewayFilter.TRACE_ID_ATTR);
		if (traceId != null) {
			exchange.getResponse().getHeaders().set(TraceIdGatewayFilter.TRACE_ID_HEADER, traceId);
		}

		String body = String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}",
				status.value(), escapeJson(errorCode));
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		return exchange.getResponse()
				.writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
	}

	private HttpStatus resolveStatus(String errorCode) {
		if ("FORBIDDEN".equals(errorCode)) {
			return HttpStatus.FORBIDDEN;
		}
		if ("UNSUPPORTED_AUTH_MODE".equals(errorCode)) {
			return HttpStatus.NOT_IMPLEMENTED;
		}
		if ("BODY_TOO_LARGE".equals(errorCode)) {
			return HttpStatus.PAYLOAD_TOO_LARGE;
		}
		return HttpStatus.UNAUTHORIZED;
	}

	private static String escapeJson(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}
}
