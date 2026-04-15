package com.maritime.platform.common.core.domain;

public enum RoleDomain {
    ADMIN,
    FUNCTION,
    LINE;

    public boolean isLineRole() {
        return this == LINE;
    }
}
