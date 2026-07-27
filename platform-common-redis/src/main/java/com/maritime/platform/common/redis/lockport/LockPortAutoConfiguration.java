package com.maritime.platform.common.redis.lockport;

import com.maritime.platform.common.redis.config.RedisCommonProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Auto-configures a default {@link LockPort} bean backed by {@link RedisLockPort}
 * when a {@link StringRedisTemplate} is available.
 */
@AutoConfiguration(after = RedisAutoConfiguration.class)
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnBean(StringRedisTemplate.class)
@EnableConfigurationProperties(RedisCommonProperties.class)
public class LockPortAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LockPort lockPort(StringRedisTemplate redis, RedisCommonProperties properties) {
        return new RedisLockPort(redis, properties.getLockKeyPrefix());
    }
}
