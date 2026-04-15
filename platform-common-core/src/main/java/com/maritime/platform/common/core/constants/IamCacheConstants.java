package com.maritime.platform.common.core.constants;

public final class IamCacheConstants {

    private IamCacheConstants() {
    }

    public static final String PREFIX = "iam:";

    public static final String NAV_KEY = "iam:nav:{systemCode}:{userId}:{activeOrgCode}";

    public static final String PAGE_KEY = "iam:page:{systemCode}:{userId}:{activeOrgCode}:{pageCode}";

    public static final String ADMIN_SCOPE_KEY = "iam:admin:scope:{systemCode}:{userId}";

    public static final String DEFAULT_PERM_KEY = "iam:perm:default:{systemCode}";

    public static final String SESSION_KEY = "iam:session:{sessionId}";

    public static final String TOKEN_BLACKLIST_KEY = "iam:token:blacklist:{jti}";

    public static final String TEST_USERS_KEY = "iam:test:users";

    public static final String USER_ORGS_KEY = "iam:user:orgs:{userId}";
}
