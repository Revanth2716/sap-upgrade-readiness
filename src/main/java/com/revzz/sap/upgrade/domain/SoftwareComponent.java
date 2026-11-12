package com.revzz.sap.upgrade.domain;

/**
 * A software component.
 *
 * <p>These are the units SAP delivers in support packages — SAP_BASIS,
 * SAP_ABA, SAP_HANA and so on. Whether a component is in a good state is a
 * property of the <em>system</em>, not of the component, which is why the
 * state lives in {@link ComponentStatus}.</p>
 */
public class SoftwareComponent {

    private String componentId;
    private String description;
    private String product;

    /** The component that replaces this one, when the product was replaced. */
    private String replacedBy;

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(String replacedBy) {
        this.replacedBy = replacedBy;
    }
}
