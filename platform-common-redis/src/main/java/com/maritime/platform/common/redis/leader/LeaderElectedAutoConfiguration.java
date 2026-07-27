package com.maritime.platform.common.redis.leader;

import com.maritime.platform.common.redis.lockport.LockPort;
import com.maritime.platform.common.redis.lockport.LockPortAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configures {@link LeaderElectedAspect} when Spring AOP and a
 * {@link LockPort} bean are both available.
 */
@AutoConfiguration(after = LockPortAutoConfiguration.class)
@ConditionalOnClass(org.aspectj.lang.ProceedingJoinPoint.class)
@ConditionalOnBean(LockPort.class)
public class LeaderElectedAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LeaderElectedAspect leaderElectedAspect(LockPort lockPort) {
        return new LeaderElectedAspect(lockPort);
    }
}
