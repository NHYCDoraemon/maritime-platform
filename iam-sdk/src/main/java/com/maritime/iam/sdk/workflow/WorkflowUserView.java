package com.maritime.iam.sdk.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * User summary returned by workflow-lookup endpoints. Minimal by design
 * — the engine picker UI only needs identity + display + org hint; a
 * richer {@link WorkflowUserContextView} is available when the engine
 * captures the full initiator snapshot at instance start.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowUserView(
        String userId,
        String displayName,
        String orgCode,
        String positionCode) {
}
