package com.maritime.iam.sdk.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowOrgView(String orgCode, String displayName) {
}
