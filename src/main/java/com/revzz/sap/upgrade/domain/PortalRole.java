package com.revzz.sap.upgrade.domain;

/** The portal's own roles - not SAP authorisation roles. */
public enum PortalRole {

    /** Signs in and sees the dashboard. */
    VIEWER,

    /** Everything a viewer sees, plus the system detail and the changelog. */
    ANALYST,

    /** Everything, including recording a patch attempt and exporting the report. */
    UPGRADE_ADMIN;

    public boolean canAnalyse() {
        return this != VIEWER;
    }

    public boolean canOperate() {
        return this == UPGRADE_ADMIN;
    }

    public String cssClass() {
        return name().toLowerCase();
    }
}
