package com.revzz.sap.upgrade.domain;

import java.time.LocalDateTime;

/**
 * One failed patch attempt, from the SPAM patch log.
 *
 * <p>A failed patch is not a cosmetic problem. The system is left in a state
 * that nobody can reason about until somebody reads the log.</p>
 */
public class PatchError {

    private String errorId;
    private String systemId;
    private String componentId;
    private String spId;
    private String logText;
    private LocalDateTime at;

    public String getAtDisplay() {
        if (at == null) {
            return "";
        }
        return at.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    public String getErrorId() {
        return errorId;
    }

    public void setErrorId(String errorId) {
        this.errorId = errorId;
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

    public String getSpId() {
        return spId;
    }

    public void setSpId(String spId) {
        this.spId = spId;
    }

    public String getLogText() {
        return logText;
    }

    public void setLogText(String logText) {
        this.logText = logText;
    }

    public LocalDateTime getAt() {
        return at;
    }

    public void setAt(LocalDateTime at) {
        this.at = at;
    }
}
