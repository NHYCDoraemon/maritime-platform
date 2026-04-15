package com.maritime.iam.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Five-line page data scope policy from page snapshot.
 *
 * <p>{@code dataScopeType} controls how the LINE condition is generated:
 * <ul>
 *   <li>{@code ALL} — no row filter, user sees all data under this line type</li>
 *   <li>{@code ORG} — filter to user's current org: {@code org_id = ?}</li>
 *   <li>{@code SELF} — filter to own records: {@code user_id = ?}</li>
 *   <li>{@code CUSTOM} — execute {@code dataScopeExpr} JSON expression</li>
 * </ul>
 *
 * <p>{@code dataScopeExpr} is a JSON array, same schema as
 * {@link com.maritime.iam.sdk.dataperm.DataPermissionExpression}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PagePolicy(
        String lineRoleType,
        String dataScopeType,
        String dataScopeExpr
) {

    public PagePolicy {
        if (dataScopeType == null) {
            dataScopeType = "ALL";
        }
    }

    /**
     * Convenience constructor for callers that don't carry expr.
     */
    public PagePolicy(String lineRoleType, String dataScopeType) {
        this(lineRoleType, dataScopeType, null);
    }
}
