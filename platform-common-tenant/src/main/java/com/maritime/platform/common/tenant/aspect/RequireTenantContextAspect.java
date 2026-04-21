package com.maritime.platform.common.tenant.aspect;

import com.maritime.platform.common.tenant.annotation.RequireTenantContext;
import com.maritime.platform.common.tenant.context.TenantContext;
import com.maritime.platform.common.tenant.exception.TenantContextMissingException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

@Aspect
public class RequireTenantContextAspect {

    @Before("@annotation(requireTenantContext)")
    public void assertTenantContext(JoinPoint jp, RequireTenantContext requireTenantContext) {
        String tenantId = TenantContext.current();
        if (tenantId == null || tenantId.isBlank()) {
            throw new TenantContextMissingException(jp.getSignature().toShortString());
        }
    }
}
