package com.revzz.sap.cockpit.web;

import com.revzz.sap.cockpit.domain.ErrorCluster;
import com.revzz.sap.cockpit.domain.ErrorCode;
import com.revzz.sap.cockpit.service.ErrorTriageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/** The error catalogue and the triage view. */
@Controller
public class ErrorController {

    private final ErrorTriageService triageService;

    public ErrorController(ErrorTriageService triageService) {
        this.triageService = triageService;
    }

    @GetMapping("/errors")
    public String catalogue(Model model) {
        model.addAttribute("codes", triageService.catalogue());
        model.addAttribute("clusters", triageService.clusters());
        return "errors";
    }

    @GetMapping("/errors/{code}")
    public String detail(@PathVariable String code, Model model) {
        ErrorCode known = triageService.lookup(code);
        if (known == null) {
            model.addAttribute("error", "Error code " + code + " is not in the catalogue.");
            return "error";
        }
        model.addAttribute("code", known);

        List<ErrorCluster> clusters = new java.util.ArrayList<ErrorCluster>();
        for (ErrorCluster cluster : triageService.clusters()) {
            if (cluster.getErrorCode().equals(code)) {
                clusters.add(cluster);
            }
        }
        model.addAttribute("clusters", clusters);
        return "error-detail";
    }
}
