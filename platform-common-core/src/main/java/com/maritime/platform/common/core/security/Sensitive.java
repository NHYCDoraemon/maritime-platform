package com.maritime.platform.common.core.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for fields/parameters that must be encrypted-at-rest
 * via {@link SensitiveFieldEncryptor}. Interpretation is left to framework
 * integration points (MyBatis TypeHandler, Jackson serializer, etc.).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {
    /** Key identifier for multi-tenant / per-domain key rotation. Default "default". */
    String keyId() default "default";
}