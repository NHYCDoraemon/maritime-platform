package com.maritime.iam.sdk.resource;

import java.util.List;

/**
 * Top-level resource manifest from iam-resources.yml.
 * One manifest per module (microservice).
 */
public class ResourceManifest {

    private String systemCode;
    private String moduleCode;
    private String moduleName;
    private List<ResourceDefinition> resources;

    public String getSystemCode() { return systemCode; }
    public void setSystemCode(String systemCode) { this.systemCode = systemCode; }
    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public List<ResourceDefinition> getResources() { return resources; }
    public void setResources(List<ResourceDefinition> resources) { this.resources = resources; }
}
