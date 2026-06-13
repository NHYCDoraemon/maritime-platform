package com.maritime.iam.sdk.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One organization unit in a workflow actor's org hierarchy — mirrors
 * process-engine's {@code ActorOrgUnit}. Carried inside
 * {@link WorkflowUserContextView} so the engine can build a full
 * {@code ActorContextSnapshot} (ADR-0011 / ADR-0067) without a second
 * round-trip.
 *
 * <p>{@code kind} and {@code structure} are transmitted as the engine
 * enum <em>names</em> (e.g. {@code "DEPARTMENT"}, {@code
 * "GOVERNMENT_THREE_FIXES"}) so the sdk stays free of any engine-side
 * enum dependency; the engine adapter maps the strings back to its
 * enums and fails closed on an unrecognized value.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowOrgUnitView(
        String orgCode,
        String orgName,
        String kind,
        String parentOrgCode,
        int level,
        String structure) {
}
