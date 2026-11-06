package com.revzz.sap.cockpit.web;

import com.revzz.sap.cockpit.domain.Alert;
import com.revzz.sap.cockpit.domain.ErrorCluster;
import com.revzz.sap.cockpit.domain.ErrorCode;
import com.revzz.sap.cockpit.domain.MessageRecord;
import com.revzz.sap.cockpit.domain.SlaReport;
import com.revzz.sap.cockpit.service.AlertService;
import com.revzz.sap.cockpit.service.DashboardService;
import com.revzz.sap.cockpit.service.ErrorTriageService;
import com.revzz.sap.cockpit.service.InterfaceHealthService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for monitoring and integrations.
 *
 * <p>Read-only on purpose: the API exposes the same findings the screens
 * show, so a monitoring system can consume them without scraping HTML.</p>
 */
@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final InterfaceHealthService healthService;
    private final ErrorTriageService triageService;
    private final AlertService alertService;
    private final DashboardService dashboardService;

    public ApiController(InterfaceHealthService healthService,
                         ErrorTriageService triageService,
                         AlertService alertService,
                         DashboardService dashboardService) {
        this.healthService = healthService;
        this.triageService = triageService;
        this.alertService = alertService;
        this.dashboardService = dashboardService;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> health() {
        Map<String, Object> health = new LinkedHashMap<String, Object>();
        health.put("status", "UP");
        health.put("interfaces", healthService.interfaces().size());
        health.put("messagesInWindow", healthService.messagesInWindow().size());
        return health;
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> summary() {
        return dashboardService.summary();
    }

    @GetMapping(value = "/interfaces", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SlaReport> interfaces() {
        return healthService.slaReports();
    }

    @GetMapping(value = "/interfaces/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    public SlaReport interfaceDetail(@PathVariable String name) {
        return healthService.slaFor(name);
    }

    @GetMapping(value = "/messages", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MessageRecord> messages() {
        return healthService.messagesInWindow();
    }

    @GetMapping(value = "/clusters", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ErrorCluster> clusters() {
        return triageService.clusters();
    }

    @GetMapping(value = "/errors", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ErrorCode> errors() {
        return triageService.catalogue();
    }

    @GetMapping(value = "/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Alert> alerts() {
        return alertService.list();
    }
}
