package com.revzz.sap.cockpit.web;

import com.revzz.sap.cockpit.domain.Alert;
import com.revzz.sap.cockpit.service.AlertService;
import com.revzz.sap.cockpit.service.AuditService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.HttpSession;

/** The alert inbox. */
@Controller
@RequireRole({"OPS_ADMIN", "ANALYST"})
public class AlertController {

    private final AlertService alertService;
    private final AuditService auditService;

    public AlertController(AlertService alertService, AuditService auditService) {
        this.alertService = alertService;
        this.auditService = auditService;
    }

    @GetMapping("/alerts")
    public String alerts(Model model) {
        model.addAttribute("alerts", alertService.list());
        model.addAttribute("open", alertService.open());
        return "alerts";
    }

    @PostMapping("/alerts/{alertId}/ack")
    public String acknowledge(@PathVariable String alertId, HttpSession session, Model model) {
        try {
            alertService.acknowledge(alertId, CurrentUser.username(session));
            auditService.record(CurrentUser.username(session), "ALERT_ACKNOWLEDGED",
                    "Alert " + alertId + " acknowledged");
        } catch (IllegalArgumentException exception) {
            model.addAttribute("error", exception.getMessage());
        }
        return "redirect:/alerts";
    }
}
