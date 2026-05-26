package com.maritime.platform.gateway.security.hmac;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HmacCanonicalRequestBuilder tests")
class HmacCanonicalRequestBuilderTest {

	private final HmacCanonicalRequestBuilder builder = new HmacCanonicalRequestBuilder();

	@Nested
	@DisplayName("Canonical string format")
	class CanonicalStringFormat {

		@Test
		@DisplayName("all components appear in correct order with field name labels and linefeed separators")
		void allComponentsInOrder() {
			String result = builder.build("app-123", "POST", "/api/data",
					"b=2&a=1", "1700000000000", "nonce-abcdefghijklmnop", "abc123");

			String[] lines = result.split("\n");
			assertThat(lines).hasSize(7);
			assertThat(lines[0]).isEqualTo("appKey=app-123");
			assertThat(lines[1]).isEqualTo("method=POST");
			assertThat(lines[2]).isEqualTo("path=/api/data");
			assertThat(lines[3]).isEqualTo("query=a=1&b=2");
			assertThat(lines[4]).isEqualTo("timestamp=1700000000000");
			assertThat(lines[5]).isEqualTo("nonce=nonce-abcdefghijklmnop");
			assertThat(lines[6]).isEqualTo("bodyDigest=abc123");
		}

		@Test
		@DisplayName("method is normalized to uppercase")
		void methodIsUppercase() {
			String result = builder.build("app", "get", "/path", null,
					"1", "n", "d");
			assertThat(result).contains("method=GET");
		}

		@Test
		@DisplayName("null rawPath produces empty path value")
		void nullRawPathProducesEmpty() {
			String result = builder.build("app", "GET", null, null,
					"1", "n", "d");
			assertThat(result).contains("path=\n");
		}

		@Test
		@DisplayName("canonical string is deterministic for same inputs")
		void deterministicForSameInputs() {
			String args1 = builder.build("app", "POST", "/api", "a=1",
					"1000", "nonce", "digest");
			String args2 = builder.build("app", "POST", "/api", "a=1",
					"1000", "nonce", "digest");
			assertThat(args1).isEqualTo(args2);
		}
	}

	@Nested
	@DisplayName("Query string canonicalization")
	class QueryCanonicalization {

		@Test
		@DisplayName("null query returns empty string")
		void nullQueryReturnsEmpty() {
			assertThat(builder.canonicalizeQuery(null)).isEmpty();
		}

		@Test
		@DisplayName("empty query returns empty string")
		void emptyQueryReturnsEmpty() {
			assertThat(builder.canonicalizeQuery("")).isEmpty();
		}

		@Test
		@DisplayName("query parameters are sorted by key")
		void paramsSortedByKey() {
			String result = builder.canonicalizeQuery("b=2&a=1&c=3");
			assertThat(result).isEqualTo("a=1&b=2&c=3");
		}

		@Test
		@DisplayName("multiple values for same key preserve order")
		void multipleValuesPreserveOrder() {
			String result = builder.canonicalizeQuery("a=2&a=1");
			assertThat(result).isEqualTo("a=2&a=1");
		}

		@Test
		@DisplayName("URL-encoded values are decoded then re-encoded")
		void encodedValuesHandled() {
			String result = builder.canonicalizeQuery("key=%2Fpath%2Fto");
			assertThat(result).contains("%2Fpath%2Fto");
		}

		@Test
		@DisplayName("key without value produces key= form")
		void keyWithoutValue() {
			String result = builder.canonicalizeQuery("flag");
			assertThat(result).isEqualTo("flag=");
		}
	}
}
