package com.maritime.iam.sdk.response;

/**
 * Structured 403 response with differentiated reason codes.
 *
 * <p>Frontend uses {@code reason} for differentiated UX:
 * <ul>
 *   <li>{@code NO_PERMISSION} - user never had this permission</li>
 *   <li>{@code PERMISSION_REVOKED} - permission was revoked</li>
 *   <li>{@code IAM_UNAVAILABLE} - IAM service is down</li>
 * </ul>
 */
public record IamResponse403(
        int code,
        String reason,
        String message
) {

    public static IamResponse403 noPermission() {
        return new IamResponse403(
                403, "NO_PERMISSION", "无访问权限");
    }

    public static IamResponse403 permissionRevoked() {
        return new IamResponse403(
                403, "PERMISSION_REVOKED", "权限已被撤销");
    }

    public static IamResponse403 iamUnavailable() {
        return new IamResponse403(
                403, "IAM_UNAVAILABLE", "权限服务暂不可用");
    }
}
