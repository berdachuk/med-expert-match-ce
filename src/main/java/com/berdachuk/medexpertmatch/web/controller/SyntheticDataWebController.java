package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;

/**
 * Web controller for synthetic data generation (admin UI).
 */
@Slf4j
@Controller
@RequestMapping("/admin/synthetic-data")
public class SyntheticDataWebController {

    private final SyntheticDataGenerator syntheticDataGenerator;

    public SyntheticDataWebController(SyntheticDataGenerator syntheticDataGenerator) {
        this.syntheticDataGenerator = syntheticDataGenerator;
    }

    @GetMapping
    public String syntheticDataPage(WebRequest request, Model model) {
        if (!"admin".equals(request.getParameter("user"))) {
            return "redirect:/";
        }
        model.addAttribute("currentPage", "synthetic-data");
        return "admin/synthetic-data";
    }

    @PostMapping("/generate")
    public String generateSyntheticData(
            WebRequest request,
            @RequestParam(defaultValue = "medium") String size,
            @RequestParam(defaultValue = "false") boolean clear,
            Model model) {
        if (!"admin".equals(request.getParameter("user"))) {
            return "redirect:/";
        }
        model.addAttribute("currentPage", "synthetic-data");

        try {
            syntheticDataGenerator.generateTestData(size, clear);
            model.addAttribute("success", "Synthetic data generated successfully - size: " + size);
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error generating synthetic data", e);
            model.addAttribute("error", "Failed to generate synthetic data: " + e.getMessage());
            model.addAttribute("success", null);
        }

        return "admin/synthetic-data";
    }

    @PostMapping("/clear")
    public String clearSyntheticData(WebRequest request, Model model) {
        if (!"admin".equals(request.getParameter("user"))) {
            return "redirect:/";
        }
        model.addAttribute("currentPage", "synthetic-data");

        try {
            syntheticDataGenerator.clearTestData();
            model.addAttribute("success", "Synthetic data cleared successfully");
            model.addAttribute("error", null);
        } catch (Exception e) {
            log.error("Error clearing synthetic data", e);
            model.addAttribute("error", "Failed to clear synthetic data: " + e.getMessage());
            model.addAttribute("success", null);
        }

        return "admin/synthetic-data";
    }
}
