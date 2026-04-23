package com.maritime.iam.sdk.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Full initiator context — mirrors process-engine's
 * {@code InitiatorContext}. Captured at process start and carried for
 * the lifetime of the instance.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowUserContextView(
        String userId,
        String displayName,
        String orgCode,
        List<String> orgPath,
        String positionCode,
        List<String> roleCodes) {

    public WorkflowUserContextView {
        orgPath = orgPath == null ? List.of() : List.copyOf(orgPath);
        roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);
    }
}
