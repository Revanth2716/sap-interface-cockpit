package com.revzz.sap.cockpit.domain;

/**
 * A cluster of failures that share one interface and one error code.
 *
 * <p>This is the triage unit. One interface failing forty times with the
 * same error code is one problem, not forty - and turning a wall of red
 * rows into that sentence is the whole point of the tool.</p>
 */
public class ErrorCluster {

    private String interfaceName;
    private String errorCode;
    private String errorText;
    private String cause;
    private String remediation;
    private Severity severity;
    private int occurrences;
    private int distinctHosts;
    private long firstSeenEpochMs;
    private long lastSeenEpochMs;
    private double successRate;

    /** How long the cluster has been running, in hours. */
    public int ageHours(long nowEpochMs) {
        if (firstSeenEpochMs <= 0) {
            return 0;
        }
        return (int) ((nowEpochMs - firstSeenEpochMs) / 3600000L);
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(String errorText) {
        this.errorText = errorText;
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

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(int occurrences) {
        this.occurrences = occurrences;
    }

    public int getDistinctHosts() {
        return distinctHosts;
    }

    public void setDistinctHosts(int distinctHosts) {
        this.distinctHosts = distinctHosts;
    }

    public long getFirstSeenEpochMs() {
        return firstSeenEpochMs;
    }

    public void setFirstSeenEpochMs(long firstSeenEpochMs) {
        this.firstSeenEpochMs = firstSeenEpochMs;
    }

    public long getLastSeenEpochMs() {
        return lastSeenEpochMs;
    }

    public void setLastSeenEpochMs(long lastSeenEpochMs) {
        this.lastSeenEpochMs = lastSeenEpochMs;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }
}
