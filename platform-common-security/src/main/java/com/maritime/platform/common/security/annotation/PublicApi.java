package com.maritime.platform.common.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注不需要权限检查的公开端点。
 *
 * <p>仍需通过 JWT 认证（除非网关配置了白名单），仅跳过权限码校验。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PublicApi {
}
