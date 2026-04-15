package com.maritime.iam.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.maritime.iam.sdk.dataperm.DataPermissionExpression;
import java.util.List;

/**
 * Page-level snapshot returned by iam-query-service.
 * Loaded on-demand per page, ~0.5 KB.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PageSnapshot(
        String pageCode,
        List<String> buttons,
        List<String> apis,
        List<DataPermissionExpression> dataPermissions,
        List<PagePolicy> pagePolicies
) {

    public PageSnapshot {
        buttons = buttons != null
                ? List.copyOf(buttons) : List.of();
        apis = apis != null
                ? List.copyOf(apis) : List.of();
        dataPermissions = dataPermissions != null
                ? List.copyOf(dataPermissions) : List.of();
        pagePolicies = pagePolicies != null
                ? List.copyOf(pagePolicies) : List.of();
    }
}
