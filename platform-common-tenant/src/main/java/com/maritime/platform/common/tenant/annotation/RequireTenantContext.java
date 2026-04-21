package com.maritime.platform.common.tenant.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an application-layer method as requiring an active tenant context.
 *
 * <p>AOP aspect ({@link com.maritime.platform.common.tenant.aspect.RequireTenantContextAspect})
 * asserts {@link com.maritime.platform.common.tenant.context.TenantContext#current()}
 * is non-null and non-blank. Missing context throws {@link com.maritime.platform.common.tenant.exception.TenantContextMissingException}
 * before the method body runs.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireTenantContext {
}