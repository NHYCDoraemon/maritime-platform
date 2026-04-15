package com.maritime.platform.common.security.aspect;

import com.maritime.platform.common.core.exception.BusinessException;
import com.maritime.platform.common.core.result.ResultCode;
import com.maritime.platform.common.security.annotation.RequirePermission;
import com.maritime.platform.common.security.spi.PermissionChecker;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * 权限校验切面骨架 — 依赖 {@link PermissionChecker} SPI 接口。
 *
 * <p>仅在 Spring 容器中存在 {@link PermissionChecker} 实现时激活。
 * 实现由 iam-sdk (B11) 提供。</p>
 */
@Aspect
public class RequirePermissionAspect {

    private final PermissionChecker permissionChecker;

    public RequirePermissionAspect(PermissionChecker permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint,
                                  RequirePermission requirePermission)
            throws Throwable {
        String permCode = requirePermission.value();
        boolean allowed = switch (requirePermission.logic()) {
            case ANY -> permissionChecker.hasAnyPermission(
                    permCode.split(","));
            case ALL -> permissionChecker.hasAllPermissions(
                    permCode.split(","));
        };
        if (!allowed) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        return joinPoint.proceed();
    }
}
