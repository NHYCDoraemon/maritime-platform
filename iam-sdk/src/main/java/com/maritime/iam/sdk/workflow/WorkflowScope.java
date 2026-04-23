package com.maritime.iam.sdk.workflow;

/**
 * Organizational scope for workflow-engine lookups. Mirrors
 * {@code com.maritime.iam.query.interfaces.dto.workflow.WorkflowScope}
 * (iam-query-service) and
 * {@code com.maritime.process.engine.domain.assignment.OrgScope}
 * (process-engine). Values are serialized as the enum name on the wire
 * so all three layers stay independently upgradeable.
 *
 * @see com.maritime.iam.sdk.client.IamQueryClient
 */
public enum WorkflowScope {

    INITIATOR_SELF,
    INITIATOR_ORG,
    INITIATOR_ORG_PARENT,
    INITIATOR_ORG_ROOT,
    INITIATOR_ORG_SUBTREE,
    EXPLICIT_ORG,
    ALL
}
