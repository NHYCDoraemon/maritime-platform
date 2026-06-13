package com.maritime.iam.sdk.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Full initiator / actor context — mirrors process-engine's
 * {@code InitiatorContext} (lightweight fields) and
 * {@code ActorContextSnapshot} (the richer {@code orgUnits} / {@code
 * duties} / {@code regionCodes}). Captured at process start and carried
 * for the lifetime of the instance.
 *
 * <p>The richer fields were added in ADR-0067 so the engine's real
 * {@code ActorContextResolverPort} adapter can build a faithful
 * {@code ActorContextSnapshot} from a single lookup. They are additive
 * and nullable: older iam-query-service builds that don't yet populate
 * them deserialize to empty lists, and the engine adapter fails closed
 * (rejects) when {@code orgUnits} is empty rather than fabricating an
 * org hierarchy.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowUserContextView(
        String userId,
        String displayName,
        String orgCode,
        List<String> orgPath,
        String positionCode,
        List<String> roleCodes,
        List<WorkflowOrgUnitView> orgUnits,
        List<String> duties,
        List<String> regionCodes) {

    public WorkflowUserContextView {
        orgPath = orgPath == null ? List.of() : List.copyOf(orgPath);
        roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);
        orgUnits = orgUnits == null ? List.of() : List.copyOf(orgUnits);
        duties = duties == null ? List.of() : List.copyOf(duties);
        regionCodes = regionCodes == null ? List.of() : List.copyOf(regionCodes);
    }
}
