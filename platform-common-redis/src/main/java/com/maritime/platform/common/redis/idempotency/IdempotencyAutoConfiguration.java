package com.maritime.platform.common.redis.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maritime.platform.common.redis.config.RedisCommonProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Auto-configures a default {@link IdempotencyPort} bean backed by
 * {@link RedisIdempotencyPort} when a {@link StringRedisTemplate} and an
 * {@link ObjectMapper} are available.
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnBean({StringRedisTemplate.class, ObjectMapper.class})
@EnableConfigurationProperties(RedisCommonProperties.class)
public class IdempotencyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyPort idempotencyPort(
            StringRedisTemplate redis,
            ObjectMapper om,
            RedisCommonProperties properties) {
        return new RedisIdempotencyPort(redis, om, properties.getIdempotencyKeyPrefix());
    }
}
