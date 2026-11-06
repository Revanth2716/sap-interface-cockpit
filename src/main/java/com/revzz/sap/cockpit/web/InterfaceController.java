package com.revzz.sap.cockpit.web;

import com.revzz.sap.cockpit.domain.ErrorCluster;
import com.revzz.sap.cockpit.domain.HourlyBucket;
import com.revzz.sap.cockpit.domain.MessageRecord;
import com.revzz.sap.cockpit.domain.SlaReport;
import com.revzz.sap.cockpit.service.ErrorTriageService;
import com.revzz.sap.cockpit.service.InterfaceHealthService;
import com.revzz.sap.cockpit.support.TrendChartRenderer;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** The interface catalogue and the detail screen of one interface. */
@Controller
public class InterfaceController {

    private final InterfaceHealthService healthService;
    private final ErrorTriageService triageService;
    private final TrendChartRenderer trendRenderer;

    public InterfaceController(InterfaceHealthService healthService,
                               ErrorTriageService triageService,
                               TrendChartRenderer trendRenderer) {
        this.healthService = healthService;
        this.triageService = triageService;
        this.trendRenderer = trendRenderer;
    }

    @GetMapping("/interfaces")
    public String interfaces(Model model) {
        model.addAttribute("reports", healthService.slaReports());
        model.addAttribute("windowHours", healthService.windowStart() == null
                ? 0
                : java.time.Duration.between(healthService.windowStart(),
                        java.time.LocalDateTime.now()).toHours() + 1);
        return "interfaces";
    }

    @GetMapping("/interfaces/{name}")
    public String detail(@PathVariable String name, Model model) {
        SlaReport report = healthService.slaFor(name);
        if (report == null) {
            model.addAttribute("error", "Interface " + name + " was not found.");
            return "error";
        }

        model.addAttribute("report", report);

        List<HourlyBucket> buckets = healthService.hourlyTrend(name, 24);
        model.addAttribute("buckets", buckets);
        model.addAttribute("trend", trendRenderer.render(buckets, 820, 260));

        List<MessageRecord> messages = healthService.recentMessages(name, 25);
        model.addAttribute("messages", messages);

        List<ErrorCluster> clusters = new java.util.ArrayList<ErrorCluster>();
        for (ErrorCluster cluster : triageService.clusters()) {
            if (cluster.getInterfaceName().equals(name)) {
                clusters.add(cluster);
            }
        }
        model.addAttribute("clusters", clusters);
        return "interface-detail";
    }
}
