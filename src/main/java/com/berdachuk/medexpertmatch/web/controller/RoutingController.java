package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.llm.rest.MedicalAgentController;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for regional routing and facility matching.
 * Handles Use Case 6: Regional Routing.
 */
@Slf4j
@Controller
@RequestMapping("/routing")
public class RoutingController {

    private final MedicalAgentController medicalAgentController;

    public RoutingController(MedicalAgentController medicalAgentController) {
        this.medicalAgentController = medicalAgentController;
    }

    @GetMapping
    public String routingPage(
            @RequestParam(required = false) String caseId,
            Model model) {
        model.addAttribute("currentPage", "routing");
        model.addAttribute("caseId", caseId);
        return "routing";
    }

    @PostMapping("/{caseId}")
    public String routeCase(
            @PathVariable String caseId,
            Model model) {
        model.addAttribute("currentPage", "routing");
        model.addAttribute("caseId", caseId);

        try {
            ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.routeCase(caseId, Map.of());
            model.addAttribute("routingResult", response.getBody());
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error routing case: {}", caseId, e);
            model.addAttribute("error", "Failed to route case: " + e.getMessage());
        }

        return "routing";
    }
}
