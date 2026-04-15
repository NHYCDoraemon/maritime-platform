package com.maritime.iam.sdk.dataperm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Structured data permission expression from page snapshot.
 *
 * <p>After validation by {@link ExpressionParser}:
 * <ul>
 *   <li>field passes whitelist regex {@code ^[a-zA-Z_][a-zA-Z0-9_]{0,63}$}</li>
 *   <li>op is one of the supported operators</li>
 *   <li>value is typed (String/Number/List)</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DataPermissionExpression(
        String field,
        String op,
        Object value
) {
}
