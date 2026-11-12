package com.revzz.sap.upgrade.domain;

/**
 * An upgrade finding.
 *
 * <p>Every finding carries the recommended action, because "SAP_MDM is
 * obsolete" is not something anybody can act on at 09:00.</p>
 */
public class UpgradeFinding {

    private Severity severity;
    private String category;
    private String systemId;
    private String message;
    private String recommendation;

    public UpgradeFinding() {
    }

    public UpgradeFinding(Severity severity, String category, String systemId,
                           String message, String recommendation) {
        this.severity = severity;
        this.category = category;
        this.systemId = systemId;
        this.message = message;
        this.recommendation = recommendation;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
