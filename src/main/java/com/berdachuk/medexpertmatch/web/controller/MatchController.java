package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.llm.rest.MedicalAgentController;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Controller for finding specialists (matching doctors to cases).
 * Handles Use Cases 1 & 2: Match Doctors and Match Facilities.
 */
@Slf4j
@Controller
@RequestMapping("/match")
public class MatchController {

    private final MedicalAgentController medicalAgentController;
    private final MedicalCaseRepository medicalCaseRepository;

    public MatchController(
            MedicalAgentController medicalAgentController,
            MedicalCaseRepository medicalCaseRepository) {
        this.medicalAgentController = medicalAgentController;
        this.medicalCaseRepository = medicalCaseRepository;
    }

    @GetMapping
    public String matchPage(
            @RequestParam(required = false) String caseId,
            Model model) {
        model.addAttribute("currentPage", "match");
        model.addAttribute("caseId", caseId);
        if (caseId != null && !caseId.isEmpty()) {
            medicalCaseRepository.findById(caseId).ifPresent(c -> {
                model.addAttribute("case", c);
                model.addAttribute("displayAbstract", formatAbstractForDisplay(normalizeAbstractAge(c)));
            });
        }

        try {
            // Get available cases for selection with full details for display
            // Limit to 50 cases to prevent template rendering issues
            var caseIds = medicalCaseRepository.findAllIds(50);
            if (caseIds != null && !caseIds.isEmpty()) {
                var cases = medicalCaseRepository.findByIds(caseIds);
                model.addAttribute("cases", cases != null ? cases : new ArrayList<>());
            } else {
                model.addAttribute("cases", new ArrayList<>());
            }
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error loading match page for caseId: {}", caseId, e);
            // Truncate error message to prevent template issues
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.length() > 500) {
                errorMessage = errorMessage.substring(0, 500) + "...";
            }
            model.addAttribute("error", "Failed to load page: " + (errorMessage != null ? errorMessage : e.getClass().getSimpleName()));
            model.addAttribute("cases", new ArrayList<>()); // Empty list on error
        }

        return "match";
    }

    @PostMapping("/{caseId}")
    public String matchDoctors(
            @PathVariable String caseId,
            @RequestParam(required = false) String sessionId,
            Model model) {
        model.addAttribute("currentPage", "match");
        model.addAttribute("caseId", caseId);
        medicalCaseRepository.findById(caseId).ifPresent(c -> {
            model.addAttribute("case", c);
            model.addAttribute("displayAbstract", formatAbstractForDisplay(normalizeAbstractAge(c)));
        });

        try {
            Map<String, Object> requestParams = new HashMap<>();
            if (sessionId != null && !sessionId.isEmpty()) {
                requestParams.put("sessionId", sessionId);
            }
            ResponseEntity<MedicalAgentService.AgentResponse> response = medicalAgentController.matchDoctorsSync(caseId, requestParams);
            MedicalAgentService.AgentResponse agentResponse = response.getBody();
            if (agentResponse != null && agentResponse.response() != null && !agentResponse.response().trim().isEmpty()) {
                // Limit response size to prevent template parsing issues (max 50KB)
                String responseText = agentResponse.response().trim();
                if (responseText.length() > 50000) {
                    log.warn("Response too long ({} chars), truncating to 50KB", responseText.length());
                    responseText = responseText.substring(0, 50000) + "\n\n[Response truncated due to size limit]";
                }
                model.addAttribute("matchResult", responseText);
            } else {
                // Show metadata or a message if response is empty
                String fallbackMessage = "Match operation completed. ";
                if (agentResponse != null && agentResponse.metadata() != null) {
                    fallbackMessage += "Case ID: " + agentResponse.metadata().getOrDefault("caseId", caseId);
                    if (agentResponse.metadata().containsKey("skills")) {
                        fallbackMessage += ", Skills used: " + agentResponse.metadata().get("skills");
                    }
                } else {
                    fallbackMessage += "No detailed results available.";
                }
                model.addAttribute("matchResult", fallbackMessage);
            }
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error matching doctors for case: {}", caseId, e);
            // Truncate error message to prevent template issues
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.length() > 500) {
                errorMessage = errorMessage.substring(0, 500) + "...";
            }
            model.addAttribute("error", "Failed to match doctors: " + (errorMessage != null ? errorMessage : e.getClass().getSimpleName()));
            model.addAttribute("matchResult", null);
        }

        // Get available cases for selection (needed for the form)
        var caseIds = medicalCaseRepository.findAllIds(100);
        var cases = medicalCaseRepository.findByIds(caseIds);
        model.addAttribute("cases", cases != null ? cases : new ArrayList<>());

        return "match";
    }

    /**
     * Normalizes age in abstract text to match the case's patientAge when present.
     * Replaces first age phrase (X-year-old, X year old, X years old) with the authoritative age.
     */
    private static String normalizeAbstractAge(MedicalCase c) {
        if (c == null || c.abstractText() == null || c.abstractText().isEmpty()) {
            return c != null ? c.abstractText() : null;
        }
        if (c.patientAge() == null) {
            return c.abstractText();
        }
        String ageStr = c.patientAge().toString();
        String text = c.abstractText();
        // Replace first occurrence of "N-year-old", "N year old", or "N years old" with authoritative age
        text = Pattern.compile("\\d+(-year-old| year old| years old)", Pattern.CASE_INSENSITIVE)
                .matcher(text)
                .replaceFirst(ageStr + "$1");
        return text;
    }

    /**
     * Ensures an empty line before the first list in the text (lines starting with -, *, or N. ).
     */
    private static String formatAbstractForDisplay(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Pattern.compile("(\n)(\\s*[-*] |\\s*\\d+\\. )").matcher(text).replaceFirst("$1\n$2");
    }

}
