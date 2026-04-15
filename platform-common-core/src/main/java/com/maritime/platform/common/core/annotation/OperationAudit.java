package com.maritime.platform.common.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注管理端写操作，触发审计日志记录。
 *
 * <p>用法示例：
 * <pre>{@code
 * @OperationAudit(operationType = IamPermissions.OP_GRANT,
 *                 targetType = "USER",
 *                 targetCodeExpr = "#cmd.userId")
 * public void grantRole(GrantRoleCommand cmd) { ... }
 * }</pre>
 *
 * <p>batch_id 支持：{@code batch = true} 时自动生成 UUID 作为 batchId，
 * 同一方法调用内的多条审计记录共享此 batchId，便于聚合查询。</p>
 *
 * <p>切面实现由 B10 审计日志块提供。SpEL 上下文仅暴露方法参数，
 * 不暴露 Spring 容器（防止 SpEL 注入）。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationAudit {

    /** 操作类型，使用 IamPermissions.OP_* 常量 */
    String operationType();

    /** 目标类型：USER / ROLE / RESOURCE / ADMIN / APP */
    String targetType();

    /** SpEL 表达式，从方法参数提取 target code（如 #cmd.roleCode） */
    String targetCodeExpr() default "";

    /** 是否为批量操作（true 时自动生成 batchId） */
    boolean batch() default false;
}
