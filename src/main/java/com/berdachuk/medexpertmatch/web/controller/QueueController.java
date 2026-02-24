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
 * Controller for consultation queue management.
 * Handles Use Case 3: Prioritize Consults.
 */
@Slf4j
@Controller
@RequestMapping("/queue")
public class QueueController {

    private final MedicalAgentController medicalAgentController;

    public QueueController(MedicalAgentController medicalAgentController) {
        this.medicalAgentController = medicalAgentController;
    }

    @GetMapping
    public String queuePage(Model model) {
        model.addAttribute("currentPage", "queue");
        return "queue";
    }

    @PostMapping("/prioritize")
    public String prioritizeConsults(Model model) {
        model.addAttribute("currentPage", "queue");

        try {
            ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.prioritizeConsultsSync(Map.of());
            model.addAttribute("prioritizationResult", response.getBody());
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error prioritizing consults", e);
            model.addAttribute("error", "Failed to prioritize consults: " + e.getMessage());
        }

        return "queue";
    }
}
