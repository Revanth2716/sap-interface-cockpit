package com.revzz.sap.cockpit.web;

import com.revzz.sap.cockpit.domain.Alert;
import com.revzz.sap.cockpit.domain.ErrorCluster;
import com.revzz.sap.cockpit.domain.HourlyBucket;
import com.revzz.sap.cockpit.domain.SlaReport;
import com.revzz.sap.cockpit.service.AlertService;
import com.revzz.sap.cockpit.service.AuditService;
import com.revzz.sap.cockpit.service.DashboardService;
import com.revzz.sap.cockpit.service.ErrorTriageService;
import com.revzz.sap.cockpit.service.InterfaceHealthService;
import com.revzz.sap.cockpit.support.TrendChartRenderer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpSession;
import java.util.List;

/** The landing page: what is broken right now. */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final InterfaceHealthService healthService;
    private final ErrorTriageService triageService;
    private final AlertService alertService;
    private final AuditService auditService;
    private final TrendChartRenderer trendRenderer;

    public DashboardController(DashboardService dashboardService,
                               InterfaceHealthService healthService,
                               ErrorTriageService triageService,
                               AlertService alertService,
                               AuditService auditService,
                               TrendChartRenderer trendRenderer) {
        this.dashboardService = dashboardService;
        this.healthService = healthService;
        this.triageService = triageService;
        this.alertService = alertService;
        this.auditService = auditService;
        this.trendRenderer = trendRenderer;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("summary", dashboardService.summary());

        List<SlaReport> reports = healthService.slaReports();
        model.addAttribute("reports", reports.size() > 8 ? reports.subList(0, 8) : reports);

        List<ErrorCluster> clusters = triageService.clusters();
        model.addAttribute("clusters", clusters.size() > 6 ? clusters.subList(0, 6) : clusters);

        List<Alert> open = alertService.open();
        model.addAttribute("alerts", open.size() > 6 ? open.subList(0, 6) : open);

        // The trend of the worst interface - that is the one to look at.
        String worst = reports.isEmpty() ? null : reports.get(0).getInterfaceName();
        if (worst != null) {
            List<HourlyBucket> buckets = healthService.hourlyTrend(worst, 24);
            model.addAttribute("trendInterface", worst);
            model.addAttribute("trend", trendRenderer.render(buckets, 820, 260));
            model.addAttribute("buckets", buckets);
        }

        model.addAttribute("audit", auditService.recent(6));
        return "dashboard";
    }

    /** Evaluate the alerting rules on demand. */
    @PostMapping("/scan")
    public String scan(HttpSession session, Model model) {
        AlertService.ScanResult result = alertService.evaluate();
        auditService.record(CurrentUser.username(session), "ALERT_SCAN",
                result.getRaised() + " raised, " + result.getRefreshed() + " refreshed");
        return "redirect:/alerts";
    }
}
