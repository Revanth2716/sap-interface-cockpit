package com.revzz.sap.cockpit.domain;

import java.time.LocalDateTime;

/** A raised alert. Alerts are deduplicated on interface + rule. */
public class Alert {

    public static final String RULE_FAILURE_BURST = "FAILURE_BURST";
    public static final String RULE_SLA_BREACH = "SLA_BREACH";
    public static final String RULE_LATENCY_BREACH = "LATENCY_BREACH";
    public static final String RULE_AGEING = "AGEING";
    public static final String RULE_INTERFACE_DOWN = "INTERFACE_DOWN";

    private String alertId;
    private String interfaceName;
    private String rule;
    private Severity severity;
    private String message;
    private LocalDateTime raisedAt;
    private boolean acknowledged;
    private String acknowledgedBy;
    private LocalDateTime acknowledgedAt;

    public String getAlertId() {
        return alertId;
    }

    public void setAlertId(String alertId) {
        this.alertId = alertId;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getRaisedAt() {
        return raisedAt;
    }

    public void setRaisedAt(LocalDateTime raisedAt) {
        this.raisedAt = raisedAt;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    /** Formatted for display - the raw timestamp carries nanoseconds. */
    public String getRaisedAtDisplay() {
        if (raisedAt == null) {
            return "";
        }
        return raisedAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
