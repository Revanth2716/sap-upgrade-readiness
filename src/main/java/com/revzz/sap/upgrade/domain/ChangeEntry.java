package com.revzz.sap.upgrade.domain;

/**
 * One entry of the support package changelog.
 *
 * <p>The category is what makes it useful: an <b>incompatible</b> entry is
 * the one that breaks custom code, and it is the one nobody reads.</p>
 */
public class ChangeEntry {

    public static final String CATEGORY_FEATURE = "FEATURE";
    public static final String CATEGORY_FIX = "FIX";
    public static final String CATEGORY_SECURITY = "SECURITY";
    public static final String CATEGORY_INCOMPATIBLE = "INCOMPATIBLE";

    private String entryId;
    private String spId;
    private String componentId;
    private String entryText;
    private String category;

    public boolean isBreaking() {
        return CATEGORY_INCOMPATIBLE.equals(category);
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getSpId() {
        return spId;
    }

    public void setSpId(String spId) {
        this.spId = spId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getEntryText() {
        return entryText;
    }

    public void setEntryText(String entryText) {
        this.entryText = entryText;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
