package com.revzz.sap.upgrade.domain;

/**
 * A dependency between two components.
 *
 * <p>Some components have to move together: the ABAP kernel needs the ABAP
 * runtime at the same level or higher. When one of them is patched and the
 * other is not, the system is in a state SAP does not support.</p>
 */
public class ComponentDependency {

    private String dependencyId;
    private String componentId;
    private String requiresComponent;

    public String getDependencyId() {
        return dependencyId;
    }

    public void setDependencyId(String dependencyId) {
        this.dependencyId = dependencyId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getRequiresComponent() {
        return requiresComponent;
    }

    public void setRequiresComponent(String requiresComponent) {
        this.requiresComponent = requiresComponent;
    }
}
