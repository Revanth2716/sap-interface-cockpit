package com.revzz.sap.cockpit.service;

import com.revzz.sap.cockpit.config.AppProperties;
import com.revzz.sap.cockpit.domain.Alert;
import com.revzz.sap.cockpit.domain.ErrorCluster;
import com.revzz.sap.cockpit.domain.MessageRecord;
import com.revzz.sap.cockpit.domain.Severity;
import com.revzz.sap.cockpit.domain.SlaReport;
import com.revzz.sap.cockpit.repo.OperationsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The alerting rules.
 *
 * <p>Four rules, each one a question an integration team actually asks:</p>
 *
 * <ul>
 *   <li><b>FAILURE_BURST</b> - is one interface failing repeatedly?</li>
 *   <li><b>SLA_BREACH</b> - is an interface below its agreed success rate?</li>
 *   <li><b>LATENCY_BREACH</b> - is an interface too slow?</li>
 *   <li><b>AGEING</b> - is a failure nobody has touched?</li>
 * </ul>
 *
 * <p>Alerts are keyed on interface + rule, so re-running the evaluation
 * refreshes an alert instead of raising a second one.</p>
 */
@Service
public class AlertService {

    private final OperationsRepository operations;
    private final InterfaceHealthService healthService;
    private final ErrorTriageService triageService;
    private final AppProperties properties;

    public AlertService(OperationsRepository operations,
                        InterfaceHealthService healthService,
                        ErrorTriageService triageService,
                        AppProperties properties) {
        this.operations = operations;
        this.healthService = healthService;
        this.triageService = triageService;
        this.properties = properties;
    }

    /** What one evaluation did. */
    public static class ScanResult {
        private int raised;
        private int refreshed;
        private int cleared;

        public int getRaised() {
            return raised;
        }

        public int getRefreshed() {
            return refreshed;
        }

        public int getCleared() {
            return cleared;
        }

        public int getTotal() {
            return raised + refreshed;
        }
    }

    /** Evaluate every rule and persist the result. */
    public ScanResult evaluate() {
        ScanResult result = new ScanResult();

        for (SlaReport report : healthService.slaReports()) {
            if (report.getErrors() >= properties.getMonitoring().getFailureBurstThreshold()) {
                apply(result, report.getInterfaceName(), Alert.RULE_FAILURE_BURST, Severity.CRITICAL,
                        report.getErrors() + " failures of " + report.getTotal() + " calls in the last "
                                + properties.getMonitoring().getSlaWindowHours() + " hours.");
            }
            if (report.breaching() && report.getTotal() > 0) {
                apply(result, report.getInterfaceName(), Alert.RULE_SLA_BREACH, Severity.MEDIUM,
                        "Success rate " + report.getSuccessRate() + "% is below the agreed SLA of "
                                + report.getSlaPercent() + "%.");
            }
            if (report.getP95Ms() > properties.getMonitoring().getLatencyThresholdMs()) {
                apply(result, report.getInterfaceName(), Alert.RULE_LATENCY_BREACH, Severity.MEDIUM,
                        "p95 response time " + report.getP95Ms() + " ms is above the "
                                + properties.getMonitoring().getLatencyThresholdMs() + " ms target.");
            }
            if (report.getOldestFailureAgeHours() >= properties.getMonitoring().getAgeingHours()) {
                apply(result, report.getInterfaceName(), Alert.RULE_AGEING, Severity.MEDIUM,
                        "The oldest unresolved failure is " + report.getOldestFailureAgeHours()
                                + " hours old.");
            }
            if (report.getTotal() > 0 && report.getOk() == 0) {
                apply(result, report.getInterfaceName(), Alert.RULE_INTERFACE_DOWN, Severity.CRITICAL,
                        "No successful call in the last "
                                + properties.getMonitoring().getSlaWindowHours() + " hours.");
            }
        }

        // A cluster that repeats the same cause is worth naming explicitly.
        for (ErrorCluster cluster : triageService.clusters()) {
            if (cluster.getOccurrences() >= properties.getMonitoring().getFailureBurstThreshold()
                    && cluster.getCause() != null) {
                apply(result, cluster.getInterfaceName(), "RECURRING_CAUSE", Severity.HIGH,
                        cluster.getOccurrences() + " calls failed with " + cluster.getErrorCode()
                                + ": " + cluster.getCause());
            }
        }

        return result;
    }

    private void apply(ScanResult result, String interfaceName, String rule,
                        Severity severity, String message) {
        Alert existing = operations.findAlertByRule(interfaceName, rule);
        Alert alert = new Alert();
        alert.setInterfaceName(interfaceName);
        alert.setRule(rule);
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setRaisedAt(LocalDateTime.now());

        if (existing == null) {
            alert.setAlertId("AL" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            alert.setAcknowledged(false);
            operations.insertAlert(alert);
            result.raised++;
        } else {
            alert.setAlertId(existing.getAlertId());
            alert.setAcknowledged(existing.isAcknowledged());
            alert.setAcknowledgedBy(existing.getAcknowledgedBy());
            alert.setAcknowledgedAt(existing.getAcknowledgedAt());
            operations.updateAlert(alert);
            result.refreshed++;
        }
    }

    public List<Alert> list() {
        return operations.findAlerts();
    }

    public void acknowledge(String alertId, String username) {
        Alert alert = operations.findAlert(alertId);
        if (alert == null) {
            throw new IllegalArgumentException("Alert " + alertId + " was not found.");
        }
        operations.acknowledge(alertId, username, LocalDateTime.now());
    }

    /** The open alerts, most severe first. */
    public List<Alert> open() {
        java.util.List<Alert> open = new java.util.ArrayList<Alert>();
        for (Alert alert : operations.findAlerts()) {
            if (!alert.isAcknowledged()) {
                open.add(alert);
            }
        }
        java.util.Collections.sort(open, new java.util.Comparator<Alert>() {
            @Override
            public int compare(Alert left, Alert right) {
                int bySeverity = left.getSeverity().rank() - right.getSeverity().rank();
                return bySeverity != 0 ? bySeverity
                        : left.getRaisedAt().compareTo(right.getRaisedAt());
            }
        });
        return open;
    }
}
