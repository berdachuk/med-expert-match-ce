package com.berdachuk.medexpertmatch.core.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.WebRequest;

/**
 * Adds simulated user role to all Thymeleaf views.
 * isAdmin is true when request has ?user=admin (no real authentication).
 */
@ControllerAdvice
public class SimulatedUserControllerAdvice {

    @ModelAttribute("isAdmin")
    public boolean isAdmin(WebRequest request) {
        String user = request.getParameter("user");
        return "admin".equals(user);
    }
}
