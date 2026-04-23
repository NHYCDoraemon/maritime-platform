package com.maritime.iam.sdk.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response from the validate-scope endpoint. Callers should treat the
 * {@code code} field as the source of truth:
 * <ul>
 *   <li>{@code IN_SCOPE} — accept</li>
 *   <li>{@code OUT_OF_SCOPE} — reject: org anchor mismatch</li>
 *   <li>{@code MISSING_ROLE} / {@code MISSING_POSITION} — reject: hint not satisfied</li>
 *   <li>{@code UNKNOWN_USER} — reject: userId not found</li>
 *   <li>{@code NOT_IMPLEMENTED} — IAM is running the stub service; treat as reject</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ValidateScopeResponse(boolean valid, String code, String reason) {
}
