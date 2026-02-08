package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.llm.rest.MedicalAgentController;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * Controller for case analysis and decision support.
 * Handles Use Case 5: Decision Support.
 */
@Slf4j
@Controller
@RequestMapping("/analyze")
public class CaseAnalysisController {

    private final MedicalAgentController medicalAgentController;
    private final MedicalCaseRepository medicalCaseRepository;

    public CaseAnalysisController(
            MedicalAgentController medicalAgentController,
            MedicalCaseRepository medicalCaseRepository) {
        this.medicalAgentController = medicalAgentController;
        this.medicalCaseRepository = medicalCaseRepository;
    }

    /**
     * Case Analysis landing page: select a case to analyze.
     */
    @GetMapping({"", "/"})
    public String analyzeIndex(Model model) {
        model.addAttribute("currentPage", "analyze");
        model.addAttribute("caseId", null);
        return "analyze";
    }

    @GetMapping("/{caseId}")
    public String analyzePage(
            @PathVariable String caseId,
            Model model) {
        model.addAttribute("currentPage", "analyze");
        model.addAttribute("caseId", caseId);

        // Get case details if available
        medicalCaseRepository.findById(caseId).ifPresent(caseEntity -> {
            model.addAttribute("case", caseEntity);
        });

        return "analyze";
    }

    @PostMapping("/{caseId}")
    public String analyzeCase(
            @PathVariable String caseId,
            Model model) {
        model.addAttribute("currentPage", "analyze");
        model.addAttribute("caseId", caseId);

        try {
            ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.analyzeCase(caseId, Map.of());
            model.addAttribute("analysisResult", response.getBody());
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error analyzing case: {}", caseId, e);
            model.addAttribute("error", "Failed to analyze case: " + e.getMessage());
        }

        return "analyze";
    }

    @PostMapping("/recommendations/{matchId}")
    public String generateRecommendations(
            @PathVariable String matchId,
            Model model) {
        model.addAttribute("currentPage", "analyze");
        model.addAttribute("matchId", matchId);

        try {
            ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.generateRecommendations(matchId, Map.of());
            model.addAttribute("recommendationsResult", response.getBody());
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error generating recommendations for match: {}", matchId, e);
            model.addAttribute("error", "Failed to generate recommendations: " + e.getMessage());
        }

        return "analyze";
    }
}
