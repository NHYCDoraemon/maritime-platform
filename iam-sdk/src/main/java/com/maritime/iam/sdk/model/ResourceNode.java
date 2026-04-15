package com.maritime.iam.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Resource tree node for building apiCode to pageCode mapping.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResourceNode(
        String resourceCode,
        String resourceType,
        String parentCode,
        String systemCode
) {
}
