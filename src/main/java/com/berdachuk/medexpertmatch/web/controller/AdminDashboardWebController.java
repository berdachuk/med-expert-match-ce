package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.chat.service.ChatRetentionMetrics;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

/**
 * Admin operations hub (M25, M26 retention visibility).
 */
@Controller
@RequestMapping("/admin")
public class AdminDashboardWebController {

    private static final String RUNBOOK_URL =
            "https://github.com/berdachuk/med-expert-match-ce/blob/develop/docs/chat-ops-runbook.md";

    private final ChatRetentionMetrics chatRetentionMetrics;

    public AdminDashboardWebController(ChatRetentionMetrics chatRetentionMetrics) {
        this.chatRetentionMetrics = chatRetentionMetrics;
    }

    @GetMapping
    public String adminDashboard(WebRequest request, Model model) {
        if (!"admin".equals(request.getParameter("user"))) {
            return "redirect:/";
        }
        ChatRetentionMetrics.RetentionRunSnapshot snapshot = chatRetentionMetrics.lastRunSnapshot();
        model.addAttribute("currentPage", "admin");
        model.addAttribute("runbookUrl", RUNBOOK_URL);
        model.addAttribute("retentionEnabled", chatRetentionMetrics.retentionEnabled());
        model.addAttribute("retentionIdleDays", chatRetentionMetrics.retentionIdleDays());
        model.addAttribute("retentionLastRunAt", snapshot.lastRunAt());
        model.addAttribute("retentionChatsPurged", snapshot.chatsPurged());
        model.addAttribute("retentionMessagesPurged", snapshot.messagesPurged());
        return "admin/index";
    }
}
