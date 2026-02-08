package com.berdachuk.medexpertmatch.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

/**
 * Web controller for graph visualization (admin UI).
 */
@Slf4j
@Controller
@RequestMapping("/admin/graph-visualization")
public class GraphVisualizationWebController {

    @GetMapping
    public String graphVisualizationPage(WebRequest request, Model model) {
        if (!"admin".equals(request.getParameter("user"))) {
            return "redirect:/";
        }
        model.addAttribute("currentPage", "graph-visualization");
        return "admin/graph-visualization";
    }
}
