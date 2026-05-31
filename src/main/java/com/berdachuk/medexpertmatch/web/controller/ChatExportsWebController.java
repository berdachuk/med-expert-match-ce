package com.berdachuk.medexpertmatch.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

/**
 * Admin UI for chat export audit log (M24).
 */
@Controller
@RequestMapping("/admin/chat-exports")
public class ChatExportsWebController {

    @GetMapping
    public String chatExportsPage(WebRequest request, Model model) {
        if (!"admin".equals(request.getParameter("user"))) {
            return "redirect:/";
        }
        model.addAttribute("currentPage", "chat-exports");
        return "admin/chat-exports";
    }
}
