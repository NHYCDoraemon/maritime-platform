package com.maritime.platform.common.redis.resilience;

import com.maritime.platform.common.redis.config.RedisCommonProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Auto-configures reusable Redis resilience primitives.
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnBean(StringRedisTemplate.class)
@EnableConfigurationProperties(RedisCommonProperties.class)
public class ResilienceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SlidingWindowRateLimiter slidingWindowRateLimiter(
            StringRedisTemplate redis,
            RedisCommonProperties properties) {
        return new RedisSlidingWindowRateLimiter(redis, properties.getResilienceKeyPrefix());
    }

    @Bean
    @ConditionalOnMissingBean
    public CircuitBreakerStore circuitBreakerStore(
            StringRedisTemplate redis,
            RedisCommonProperties properties) {
        return new RedisCircuitBreakerStore(redis, properties.getResilienceKeyPrefix());
    }

    @Bean
    @ConditionalOnMissingBean
    public TtlCache ttlCache(StringRedisTemplate redis, RedisCommonProperties properties) {
        return new RedisTtlCache(redis, properties.getResilienceKeyPrefix());
    }
}
