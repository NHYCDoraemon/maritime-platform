package com.maritime.platform.common.tenant.config;

import com.maritime.platform.common.tenant.aspect.RequireTenantContextAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@AutoConfiguration
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@EnableAspectJAutoProxy
public class TenantAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RequireTenantContextAspect requireTenantContextAspect() {
        return new RequireTenantContextAspect();
    }
}
