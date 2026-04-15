package com.maritime.platform.common.security.config;

import com.maritime.platform.common.security.aspect.RequirePermissionAspect;
import com.maritime.platform.common.security.jwt.JwtProperties;
import com.maritime.platform.common.security.spi.PermissionChecker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for IAM security components.
 *
 * <p>Registers beans conditionally — does NOT use @ComponentScan
 * to avoid clashing with business systems that have their own
 * security aspects (e.g., todo-center's RequirePermissionAspect).
 */
@AutoConfiguration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnBean(PermissionChecker.class)
    @ConditionalOnMissingBean(name = "requirePermissionAspect")
    RequirePermissionAspect iamRequirePermissionAspect(
            PermissionChecker permissionChecker) {
        return new RequirePermissionAspect(permissionChecker);
    }
}
