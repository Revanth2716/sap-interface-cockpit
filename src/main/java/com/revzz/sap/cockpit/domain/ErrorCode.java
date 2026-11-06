package com.revzz.sap.cockpit.domain;

/**
 * One entry of the error catalogue.
 *
 * <p>This is the part that turns a red dashboard into a fix: every error
 * code carries its cause and the action that resolves it, so a support
 * engineer does not have to guess.</p>
 */
public class ErrorCode {

    private String code;
    private Severity severity;
    private String cause;
    private String remediation;
    private boolean common;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }

    public String getRemediation() {
        return remediation;
    }

    public void setRemediation(String remediation) {
        this.remediation = remediation;
    }

    public boolean isCommon() {
        return common;
    }

    public void setCommon(boolean common) {
        this.common = common;
    }
}
