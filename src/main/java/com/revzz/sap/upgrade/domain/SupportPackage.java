package com.revzz.sap.upgrade.domain;

import java.time.LocalDate;

/** A support package, and the patch level it carries. */
public class SupportPackage {

    private String spId;
    private int patchLevel;
    private LocalDate releasedOn;
    private String description;

    public String getReleasedOnDisplay() {
        if (releasedOn == null) {
            return "";
        }
        return releasedOn.toString();
    }

    public String getSpId() {
        return spId;
    }

    public void setSpId(String spId) {
        this.spId = spId;
    }

    public int getPatchLevel() {
        return patchLevel;
    }

    public void setPatchLevel(int patchLevel) {
        this.patchLevel = patchLevel;
    }

    public LocalDate getReleasedOn() {
        return releasedOn;
    }

    public void setReleasedOn(LocalDate releasedOn) {
        this.releasedOn = releasedOn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
