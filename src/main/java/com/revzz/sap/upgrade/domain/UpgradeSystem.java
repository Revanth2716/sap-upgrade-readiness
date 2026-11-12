package com.revzz.sap.upgrade.domain;

import java.time.LocalDateTime;

/** One system in the upgrade landscape. */
public class UpgradeSystem {

    public static final String ROLE_DEVELOPMENT = "DEVELOPMENT";
    public static final String ROLE_QUALITY = "QUALITY";
    public static final String ROLE_PRODUCTION = "PRODUCTION";

    private String systemId;
    private String description;
    private String systemRole;
    private boolean target;
    private int patchLevel;
    private LocalDateTime lastPatch;
    private String patchedBy;

    public int daysSinceLastPatch(LocalDateTime now) {
        if (lastPatch == null) {
            return 0;
        }
        return (int) java.time.Duration.between(lastPatch, now).toDays();
    }

    public String getLastPatchDisplay() {
        if (lastPatch == null) {
            return "never";
        }
        return lastPatch.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSystemRole() {
        return systemRole;
    }

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
    }

    public boolean isTarget() {
        return target;
    }

    public void setTarget(boolean target) {
        this.target = target;
    }

    public int getPatchLevel() {
        return patchLevel;
    }

    public void setPatchLevel(int patchLevel) {
        this.patchLevel = patchLevel;
    }

    public LocalDateTime getLastPatch() {
        return lastPatch;
    }

    public void setLastPatch(LocalDateTime lastPatch) {
        this.lastPatch = lastPatch;
    }

    public String getPatchedBy() {
        return patchedBy;
    }

    public void setPatchedBy(String patchedBy) {
        this.patchedBy = patchedBy;
    }
}
