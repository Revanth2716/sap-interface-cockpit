package com.revzz.sap.cockpit.service;

import com.revzz.sap.cockpit.domain.Alert;
import com.revzz.sap.cockpit.domain.ErrorCluster;
import com.revzz.sap.cockpit.domain.MessageRecord;
import com.revzz.sap.cockpit.domain.SlaReport;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The numbers on the landing page. */
@Service
public class DashboardService {

    private final InterfaceHealthService healthService;
    private final ErrorTriageService triageService;
    private final AlertService alertService;
    private final ReplayService replayService;

    public DashboardService(InterfaceHealthService healthService,
                            ErrorTriageService triageService,
                            AlertService alertService,
                            ReplayService replayService) {
        this.healthService = healthService;
        this.triageService = triageService;
        this.alertService = alertService;
        this.replayService = replayService;
    }

    public Map<String, Object> summary() {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();

        List<MessageRecord> messages = healthService.messagesInWindow();
        int errors = 0;
        int warnings = 0;
        for (MessageRecord message : messages) {
            if (message.isError()) {
                errors++;
            } else if (message.isWarning()) {
                warnings++;
            }
        }

        List<SlaReport> reports = healthService.slaReports();
        int breaching = 0;
        for (SlaReport report : reports) {
            if (report.breaching()) {
                breaching++;
            }
        }

        List<ErrorCluster> clusters = triageService.clusters();
        List<Alert> open = alertService.open();

        summary.put("interfaces", healthService.interfaces().size());
        summary.put("messages", messages.size());
        summary.put("errors", errors);
        summary.put("warnings", warnings);
        summary.put("errorRate", messages.isEmpty()
                ? 0.0
                : Math.round(10000.0 * errors / messages.size()) / 100.0);
        summary.put("slaBreaches", breaching);
        summary.put("openAlerts", open.size());
        summary.put("clusters", clusters.size());
        summary.put("queuedReplays", replayService.pending().size());
        summary.put("windowHours", healthService.windowStart() == null
                ? 0 : java.time.Duration.between(healthService.windowStart(),
                        java.time.LocalDateTime.now()).toHours() + 1);
        return summary;
    }
}
