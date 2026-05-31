package com.berdachuk.medexpertmatch.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

/**
 * Admin operations hub (M25).
 */
@Controller
@RequestMapping("/admin")
public class AdminDashboardWebController {

    @GetMapping
    public String adminDashboard(WebRequest request, Model model) {
        if (!"admin".equals(request.getParameter("user"))) {
            return "redirect:/";
        }
        model.addAttribute("currentPage", "admin");
        return "admin/index";
    }
}
