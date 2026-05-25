package com.maritime.platform.gateway.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Provides a {@link ReactiveRedisTemplate} with String serialization
 * for simple key/value Redis operations used in gateway state checks.
 */
@AutoConfiguration
@ConditionalOnClass(ReactiveRedisConnectionFactory.class)
public class GatewayRedisConfiguration {

	@Bean
	@ConditionalOnBean(ReactiveRedisConnectionFactory.class)
	ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate(
			ReactiveRedisConnectionFactory connectionFactory) {
		RedisSerializationContext<String, String> context = RedisSerializationContext
				.<String, String>newSerializationContext(StringRedisSerializer.UTF_8)
				.key(StringRedisSerializer.UTF_8)
				.value(StringRedisSerializer.UTF_8)
				.hashKey(StringRedisSerializer.UTF_8)
				.hashValue(StringRedisSerializer.UTF_8)
				.build();
		return new ReactiveRedisTemplate<>(connectionFactory, context);
	}
}
