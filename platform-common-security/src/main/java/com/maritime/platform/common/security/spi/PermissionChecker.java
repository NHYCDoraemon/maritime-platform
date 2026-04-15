package com.maritime.platform.common.security.spi;

/**
 * 权限校验 SPI 接口。
 *
 * <p>由 iam-sdk (B11) 提供实现，基于页面快照做 API 级权限判定。
 * 本模块仅定义接口契约。</p>
 *
 * <p>实现约定：
 * <ul>
 *   <li>IAM 服务不可用时必须返回 {@code false}（fail-closed），不能返回 {@code true}</li>
 *   <li>带有 {@code X-Internal-Call} 头的服务间调用可跳过权限校验</li>
 * </ul>
 */
public interface PermissionChecker {

    boolean hasPermission(String permissionCode);

    boolean hasAnyPermission(String... permissionCodes);

    boolean hasAllPermissions(String... permissionCodes);
}
