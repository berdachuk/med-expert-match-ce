package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.core.domain.ApiSessionTokenView;
import com.berdachuk.medexpertmatch.core.service.ApiSessionTokenAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

/**
 * Admin UI for API session token management (M22).
 */
@Controller
@RequestMapping("/admin/session-tokens")
public class SessionTokensWebController {

    private final ApiSessionTokenAdminService sessionTokenAdminService;

    public SessionTokensWebController(ApiSessionTokenAdminService sessionTokenAdminService) {
        this.sessionTokenAdminService = sessionTokenAdminService;
    }

    @GetMapping
    public String sessionTokensPage(WebRequest request, Model model) {
        if (!"admin".equals(request.getParameter("user"))) {
            return "redirect:/";
        }
        List<ApiSessionTokenView> tokens = sessionTokenAdminService.listTokens();
        model.addAttribute("currentPage", "session-tokens");
        model.addAttribute("tokens", tokens);
        model.addAttribute("expiryWarningDays", 7);
        return "admin/session-tokens";
    }
}
