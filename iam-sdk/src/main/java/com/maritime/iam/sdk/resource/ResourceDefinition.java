package com.maritime.iam.sdk.resource;

import java.util.List;

/**
 * Single resource node in the manifest tree.
 * Matches iam-resources.yml structure.
 */
public class ResourceDefinition {

    private String code;
    private String name;
    private String type;
    private String routePath;
    private String component;
    private String permissionExpr;
    private Integer sortNo;
    private List<ResourceDefinition> children;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRoutePath() { return routePath; }
    public void setRoutePath(String routePath) { this.routePath = routePath; }
    public String getComponent() { return component; }
    public void setComponent(String component) { this.component = component; }
    public String getPermissionExpr() { return permissionExpr; }
    public void setPermissionExpr(String permissionExpr) { this.permissionExpr = permissionExpr; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public List<ResourceDefinition> getChildren() { return children; }
    public void setChildren(List<ResourceDefinition> children) { this.children = children; }
}
