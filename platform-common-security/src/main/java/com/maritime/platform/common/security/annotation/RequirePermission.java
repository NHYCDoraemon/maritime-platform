package com.maritime.platform.common.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明 Controller 方法所需的权限码。
 *
 * <p>用法示例：
 * <pre>{@code
 * @RequirePermission("HSJG:lawCase:button:add")
 * @PostMapping("/cases")
 * public R<Void> createCase(@RequestBody CreateCaseRequest req) { ... }
 * }</pre>
 *
 * <p>未标注 {@code @RequirePermission} 且未标注 {@code @PublicApi} 的 Controller 方法，
 * 按 default-deny 策略自动返回 403。</p>
 *
 * <p>多权限码以逗号分隔，通过 {@link #logic()} 指定逻辑关系：
 * <ul>
 *   <li>{@link LogicType#ANY} — 满足任一即可（默认）</li>
 *   <li>{@link LogicType#ALL} — 必须全部满足</li>
 * </ul>
 *
 * <p>依赖：
 * <ul>
 *   <li>网关必须剥离外部请求的 {@code X-Internal-Call} 头</li>
 *   <li>iam-sdk 对带有 {@code X-Internal-Call} 头的服务间调用跳过权限校验</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /** 权限码（permission_expr），如 "HSJG:lawCase:button:add" */
    String value();

    /** 多个权限码时的逻辑关系，默认 ANY（满足任一即可） */
    LogicType logic() default LogicType.ANY;

    enum LogicType {
        ANY,
        ALL
    }
}
