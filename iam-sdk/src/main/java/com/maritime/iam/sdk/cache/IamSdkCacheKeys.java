package com.maritime.iam.sdk.cache;

import java.util.Objects;

/**
 * Cache keys owned by the IAM SDK in a business system's Redis.
 *
 * <p>These keys are deliberately separate from IAM Center's L3 snapshot
 * cache. A business system may use a different Redis database or cluster,
 * so invalidation must be performed in the consumer's own cache.</p>
 */
public final class IamSdkCacheKeys {

    private static final String NO_ORG = "_NO_ORG_";

    private IamSdkCacheKeys() {
    }

    /**
     * Business L2 navigation snapshot.
     */
    public static String nav(String systemCode, String userId,
                             String activeOrgCode) {
        return "biz:nav:" + required(systemCode, "systemCode")
                + ":" + required(userId, "userId")
                + ":" + orgOrDefault(activeOrgCode);
    }

    /**
     * Pattern for all navigation snapshots of one user.
     */
    public static String navUserPattern(String systemCode,
                                        String userId) {
        return "biz:nav:" + required(systemCode, "systemCode")
                + ":" + required(userId, "userId") + ":*";
    }

    /**
     * Pattern for all navigation snapshots of one system.
     */
    public static String navSystemPattern(String systemCode) {
        return "biz:nav:" + required(systemCode, "systemCode")
                + ":*";
    }

    /**
     * Business L2 page snapshot.
     */
    public static String page(String systemCode, String userId,
                              String activeOrgCode, String pageCode) {
        return "biz:page:" + required(systemCode, "systemCode")
                + ":" + required(userId, "userId")
                + ":" + orgOrDefault(activeOrgCode)
                + ":" + required(pageCode, "pageCode");
    }

    /**
     * Pattern for all page snapshots of one user.
     */
    public static String pageUserPattern(String systemCode,
                                         String userId) {
        return "biz:page:" + required(systemCode, "systemCode")
                + ":" + required(userId, "userId") + ":*";
    }

    /**
     * Pattern for all page snapshots of one system.
     */
    public static String pageSystemPattern(String systemCode) {
        return "biz:page:" + required(systemCode, "systemCode")
                + ":*";
    }

    /**
     * Permission codes injected by a business gateway.
     *
     * <p>The key keeps the historical {@code iam:perms} prefix so existing
     * consumers can migrate without a cold-cache compatibility window.</p>
     */
    public static String permissionCodes(String systemCode,
                                         String userId,
                                         String activeOrgCode) {
        return "iam:perms:" + required(systemCode, "systemCode")
                + ":" + required(userId, "userId")
                + ":" + orgOrDefault(activeOrgCode);
    }

    /**
     * Pattern for all permission-code snapshots of one user.
     */
    public static String permissionCodesUserPattern(
            String systemCode, String userId) {
        return "iam:perms:" + required(systemCode, "systemCode")
                + ":" + required(userId, "userId") + ":*";
    }

    /**
     * Pattern for all permission-code snapshots of one system.
     */
    public static String permissionCodesSystemPattern(
            String systemCode) {
        return "iam:perms:" + required(systemCode, "systemCode")
                + ":*";
    }

    private static String orgOrDefault(String activeOrgCode) {
        return activeOrgCode == null || activeOrgCode.isBlank()
                ? NO_ORG : activeOrgCode;
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name + " required");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " required");
        }
        return value;
    }
}
