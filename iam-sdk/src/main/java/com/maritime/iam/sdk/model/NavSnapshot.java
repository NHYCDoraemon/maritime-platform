package com.maritime.iam.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Navigation snapshot returned by iam-query-service.
 * Loaded once at login, ~8 KB. Contains roles and resource tree.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NavSnapshot(
        String userId,
        String systemCode,
        String activeOrgCode,
        List<String> roles,
        List<String> functionRoles,
        List<String> lineRoles,
        List<String> deniedResources,
        long version
) {

    public NavSnapshot {
        roles = roles != null ? List.copyOf(roles) : List.of();
        functionRoles = functionRoles != null
                ? List.copyOf(functionRoles) : List.of();
        lineRoles = lineRoles != null
                ? List.copyOf(lineRoles) : List.of();
        deniedResources = deniedResources != null
                ? List.copyOf(deniedResources) : List.of();
    }
}
