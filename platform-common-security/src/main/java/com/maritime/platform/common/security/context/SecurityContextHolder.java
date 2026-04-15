package com.maritime.platform.common.security.context;

import com.maritime.platform.common.core.exception.BusinessException;
import com.maritime.platform.common.core.result.ResultCode;

/**
 * ThreadLocal-based holder for the current authenticated user context.
 */
public final class SecurityContextHolder {

    private static final ThreadLocal<SecurityUser> CONTEXT = new ThreadLocal<>();

    private SecurityContextHolder() {
    }

    public static void set(SecurityUser user) {
        CONTEXT.set(user);
    }

    public static SecurityUser get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Returns the current security user or throws if none is present.
     *
     * @return the current {@link SecurityUser}, never {@code null}
     * @throws BusinessException with {@link ResultCode#UNAUTHORIZED} when no user is set
     */
    public static SecurityUser require() {
        SecurityUser user = get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }
}
