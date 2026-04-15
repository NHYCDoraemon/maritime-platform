package com.maritime.iam.sdk.resource;

/**
 * Flattened resource node for Nacos publication (JSON array).
 * Each node carries its parentCode for tree reconstruction.
 */
public record FlatResourceNode(
        String code,
        String name,
        String type,
        String parentCode,
        String routePath,
        String component,
        String permissionExpr,
        Integer sortNo
) {
}
