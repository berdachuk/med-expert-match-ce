package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.llm.rest.MedicalAgentController;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * Controller for network analytics.
 * Handles Use Case 4: Network Analytics.
 */
@Slf4j
@Controller
@RequestMapping("/analytics")
public class AnalyticsController {

    private final MedicalAgentController medicalAgentController;

    public AnalyticsController(MedicalAgentController medicalAgentController) {
        this.medicalAgentController = medicalAgentController;
    }

    @GetMapping
    public String analyticsPage(Model model) {
        model.addAttribute("currentPage", "analytics");
        return "analytics";
    }

    @PostMapping("/network")
    public String networkAnalytics(Model model) {
        model.addAttribute("currentPage", "analytics");

        try {
            ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.networkAnalytics(Map.of());
            model.addAttribute("analyticsResult", response.getBody());
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error getting network analytics", e);
            model.addAttribute("error", "Failed to get network analytics: " + e.getMessage());
        }

        return "analytics";
    }
}
