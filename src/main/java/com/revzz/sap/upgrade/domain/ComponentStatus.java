package com.revzz.sap.upgrade.domain;

import java.time.LocalDateTime;

/**
 * The state of one component in one system.
 *
 * <p>The status is the thing that decides whether an upgrade can happen at
 * all:</p>
 * <ul>
 *   <li><b>PATCHED</b> — the component is at the level the system is on.</li>
 *   <li><b>NEW</b> — the component was added after the system was last
 *       patched, so it has never been patched.</li>
 *   <li><b>OBSOLETE</b> — the product has been replaced and the component
 *       must be migrated before the system can be upgraded.</li>
 *   <li><b>RELEASED</b> — SAP no longer delivers this component. Same
 *       consequence as obsolete, and no way to patch it.</li>
 * </ul>
 */
public class ComponentStatus {

    public static final String STATUS_PATCHED = "PATCHED";
    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_OBSOLETE = "OBSOLETE";
    public static final String STATUS_RELEASED = "RELEASED";

    private String systemId;
    private String componentId;
    private int patchLevel;
    private String status;
    private String statusText;
    private LocalDateTime lastChanged;

    public boolean isPatched() {
        return STATUS_PATCHED.equals(status);
    }

    /** A component that blocks the upgrade, whatever the reason. */
    public boolean isBlocking() {
        return STATUS_OBSOLETE.equals(status) || STATUS_RELEASED.equals(status);
    }

    public String getLastChangedDisplay() {
        if (lastChanged == null) {
            return "";
        }
        return lastChanged.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public int getPatchLevel() {
        return patchLevel;
    }

    public void setPatchLevel(int patchLevel) {
        this.patchLevel = patchLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public LocalDateTime getLastChanged() {
        return lastChanged;
    }

    public void setLastChanged(LocalDateTime lastChanged) {
        this.lastChanged = lastChanged;
    }
}
