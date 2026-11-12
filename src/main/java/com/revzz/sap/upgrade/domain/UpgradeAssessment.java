package com.revzz.sap.upgrade.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * The upgrade readiness of one system.
 *
 * <p>The score is for sorting; the findings are what make it actionable.
 * The severity is the worst finding, so an obsolete component can never hide
 * behind an otherwise healthy system.</p>
 */
public class UpgradeAssessment {

    private UpgradeSystem system;
    private int score;
    private Severity severity;
    private List<UpgradeFinding> findings = new ArrayList<UpgradeFinding>();
    private List<ComponentStatus> componentStatuses = new ArrayList<ComponentStatus>();
    private List<ComponentStatus> blockers = new ArrayList<ComponentStatus>();
    private List<PatchError> errors = new ArrayList<PatchError>();

    /** How many patch levels behind the highest level in the landscape. */
    private int drift;

    /** The highest patch level anywhere in the landscape. */
    private int highestLevel;

    public boolean hasFinding(String category) {
        for (UpgradeFinding finding : findings) {
            if (category.equals(finding.getCategory())) {
                return true;
            }
        }
        return false;
    }

    public int findingCount() {
        return findings.size();
    }

    public boolean isReady() {
        return blockers.isEmpty() && errors.isEmpty();
    }

    public String getDriftDisplay() {
        if (drift <= 0) {
            return "at the latest level";
        }
        return drift + (drift == 1 ? " level behind" : " levels behind");
    }

    public UpgradeSystem getSystem() {
        return system;
    }

    public void setSystem(UpgradeSystem system) {
        this.system = system;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public List<UpgradeFinding> getFindings() {
        return findings;
    }

    public void setFindings(List<UpgradeFinding> findings) {
        this.findings = findings;
    }

    public List<ComponentStatus> getComponentStatuses() {
        return componentStatuses;
    }

    public void setComponentStatuses(List<ComponentStatus> componentStatuses) {
        this.componentStatuses = componentStatuses;
    }

    public List<ComponentStatus> getBlockers() {
        return blockers;
    }

    public void setBlockers(List<ComponentStatus> blockers) {
        this.blockers = blockers;
    }

    public List<PatchError> getErrors() {
        return errors;
    }

    public void setErrors(List<PatchError> errors) {
        this.errors = errors;
    }

    public int getDrift() {
        return drift;
    }

    public void setDrift(int drift) {
        this.drift = drift;
    }

    public int getHighestLevel() {
        return highestLevel;
    }

    public void setHighestLevel(int highestLevel) {
        this.highestLevel = highestLevel;
    }
}
