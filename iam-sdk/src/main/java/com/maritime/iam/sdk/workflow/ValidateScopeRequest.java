package com.maritime.iam.sdk.workflow;

/**
 * Body for {@code POST /api/iam/workflow/users/validate-scope}. Used by
 * the engine's {@code AssigneeValidatePort} adapter to re-check a
 * chosen assignee at submit time.
 *
 * <p>Fields mirror the matching DTO in iam-query-service. Nullable
 * fields map to "not applicable" rather than "any" — the server
 * decides how to interpret.</p>
 */
public record ValidateScopeRequest(
        String userId,
        WorkflowScope scope,
        String initiatorOrg,
        String explicitOrgCode,
        String requiredRoleCode,
        String requiredPositionCode) {
}
