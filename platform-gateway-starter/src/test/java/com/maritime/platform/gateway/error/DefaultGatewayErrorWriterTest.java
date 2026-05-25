package com.maritime.platform.gateway.error;

import com.maritime.platform.gateway.filter.TraceIdGatewayFilter;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DefaultGatewayErrorWriter tests")
class DefaultGatewayErrorWriterTest {

	private DefaultGatewayErrorWriter writer;

	@BeforeEach
	void setUp() {
		writer = new DefaultGatewayErrorWriter();
	}

	@Nested
	@DisplayName("Response format")
	class ResponseFormat {

		@Test
		@DisplayName("produces valid JSON with code, message, and data fields")
		void producesCorrectJsonFormat() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/test"));

			writer.write(exchange, GatewayAuthErrorCode.MISSING_TOKEN).block();

			MockServerHttpResponse response = exchange.getResponse();
			String body = response.getBodyAsString().block();
			assertThat(body).isEqualTo(
					"{\"code\":401,\"message\":\"MISSING_TOKEN\",\"data\":null}");
		}

		@Test
		@DisplayName("response content type is application/json")
		void contentTypeIsJson() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/test"));

			writer.write(exchange, GatewayAuthErrorCode.INVALID_TOKEN).block();

			assertThat(exchange.getResponse().getHeaders().getContentType().toString())
					.contains("application/json");
		}

		@Test
		@DisplayName("escapes special characters in error code")
		void escapesSpecialCharacters() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/test"));

			writer.write(exchange, "BAD\"THING").block();

			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains("BAD\\\"THING");
		}
	}

	@Nested
	@DisplayName("HTTP status code mapping")
	class StatusCodeMapping {

		@Test
		@DisplayName("JWT errors return 401")
		void jwtErrorsReturn401() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/test"));

			writer.write(exchange, GatewayAuthErrorCode.MISSING_TOKEN).block();
			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
		}

		@Test
		@DisplayName("HMAC errors return 401")
		void hmacErrorsReturn401() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/test"));

			writer.write(exchange, GatewayAuthErrorCode.INVALID_SIGNATURE).block();
			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
		}

		@Test
		@DisplayName("nonce errors return 401")
		void nonceErrorsReturn401() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/test"));

			writer.write(exchange, GatewayAuthErrorCode.REPLAY_DETECTED).block();
			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
		}

		@Test
		@DisplayName("state errors return 401")
		void stateErrorsReturn401() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/test"));

			writer.write(exchange, GatewayAuthErrorCode.USER_DISABLED).block();
			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
		}

		@Test
		@DisplayName("APP_DISABLED returns 401")
		void appDisabledReturns401() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/test"));

			writer.write(exchange, GatewayAuthErrorCode.APP_DISABLED).block();
			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
		}

		@Test
		@DisplayName("FORBIDDEN returns 403")
		void forbiddenReturns403() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/test"));

			writer.write(exchange, GatewayAuthErrorCode.FORBIDDEN).block();
			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(403);

			String body = exchange.getResponse().getBodyAsString().block();
			assertThat(body).contains("\"code\":403");
			assertThat(body).contains("\"message\":\"FORBIDDEN\"");
		}
	}

	@Nested
	@DisplayName("X-Trace-Id header")
	class TraceIdHeader {

		@Test
		@DisplayName("includes X-Trace-Id in error response when present in exchange")
		void includesTraceIdFromExchangeAttribute() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/test"));
			exchange.getAttributes().put(TraceIdGatewayFilter.TRACE_ID_ATTR,
					"abc123def456");
			exchange.getResponse().getHeaders().set(
					TraceIdGatewayFilter.TRACE_ID_HEADER, "abc123def456");

			writer.write(exchange, GatewayAuthErrorCode.MISSING_TOKEN).block();

			assertThat(exchange.getResponse().getHeaders()
					.getFirst("X-Trace-Id")).isEqualTo("abc123def456");
		}

		@Test
		@DisplayName("does not fail when trace ID is absent from exchange")
		void missingTraceIdDoesNotFail() {
			MockServerWebExchange exchange = MockServerWebExchange.from(
					MockServerHttpRequest.get("/api/test"));

			writer.write(exchange, GatewayAuthErrorCode.MISSING_TOKEN).block();

			assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
		}
	}
}
