package com.maritime.platform.gateway.security.hmac;

import com.maritime.platform.gateway.security.GatewaySecurityProperties;
import com.maritime.platform.gateway.security.jwt.GatewayAuthErrorCode;
import com.maritime.platform.gateway.security.jwt.JwtAuthenticationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@DisplayName("HmacNonceValidator tests")
@ExtendWith(MockitoExtension.class)
class HmacNonceValidatorTest {

	@Mock
	private ReactiveRedisOperations<String, String> redisOps;

	@Mock
	private ReactiveValueOperations<String, String> valueOps;

	private GatewaySecurityProperties properties;

	@BeforeEach
	void setUp() {
		properties = new GatewaySecurityProperties();
		properties.getHmac().setEnabled(true);
		lenient().when(redisOps.opsForValue()).thenReturn(valueOps);
	}

	private HmacNonceValidator validator() {
		return new HmacNonceValidator(redisOps, properties);
	}

	@Nested
	@DisplayName("Successful nonce registration")
	class SuccessfulNonce {

		@Test
		@DisplayName("setIfAbsent returns true, validation passes")
		void nonceAcceptedPasses() {
			when(valueOps.setIfAbsent(eq("platform:gateway:hmac:nonce:app-123:nonce-xyz"),
					eq("1"), any(Duration.class)))
					.thenReturn(Mono.just(true));

			StepVerifier.create(validator().validate("app-123", "nonce-xyz"))
					.verifyComplete();
		}

		@Test
		@DisplayName("key format uses configured prefix with appKey and nonce")
		void keyFormatFollowsPattern() {
			when(valueOps.setIfAbsent(eq("platform:gateway:hmac:nonce:app-key-1:nonce-456"),
					eq("1"), any(Duration.class)))
					.thenReturn(Mono.just(true));

			StepVerifier.create(validator().validate("app-key-1", "nonce-456"))
					.verifyComplete();
		}
	}

	@Nested
	@DisplayName("Replay detection")
	class ReplayDetection {

		@Test
		@DisplayName("setIfAbsent returns false, returns REPLAY_DETECTED")
		void duplicateNonceReturnsReplayDetected() {
			when(valueOps.setIfAbsent(eq("platform:gateway:hmac:nonce:app-123:nonce-dup"),
					eq("1"), any(Duration.class)))
					.thenReturn(Mono.just(false));

			StepVerifier.create(validator().validate("app-123", "nonce-dup"))
					.verifyErrorSatisfies(e -> {
						assertThat(e)
								.isInstanceOf(JwtAuthenticationException.class)
								.extracting("errorCode").isEqualTo(GatewayAuthErrorCode.REPLAY_DETECTED);
						assertThat(e.getMessage()).contains("nonce-dup");
					});
		}
	}

	@Nested
	@DisplayName("Custom key prefix")
	class CustomKeyPrefix {

		@Test
		@DisplayName("custom nonce-key-prefix is used in Redis key")
		void customPrefixUsed() {
			properties.getHmac().setNonceKeyPrefix("custom:hmac:nonce:");
			when(valueOps.setIfAbsent(eq("custom:hmac:nonce:app-123:nonce-xyz"),
					eq("1"), any(Duration.class)))
					.thenReturn(Mono.just(true));

			StepVerifier.create(validator().validate("app-123", "nonce-xyz"))
					.verifyComplete();
		}
	}

	@Nested
	@DisplayName("TTL is passed through")
	class TtlPassed {

		@Test
		@DisplayName("configurable TTL is passed to setIfAbsent")
		void customTtlPassed() {
			properties.getHmac().setNonceTtl(Duration.ofSeconds(120));
			when(valueOps.setIfAbsent(eq("platform:gateway:hmac:nonce:app-123:nonce-xyz"),
					eq("1"), eq(Duration.ofSeconds(120))))
					.thenReturn(Mono.just(true));

			StepVerifier.create(validator().validate("app-123", "nonce-xyz"))
					.verifyComplete();
		}
	}
}
