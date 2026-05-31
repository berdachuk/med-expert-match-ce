package com.berdachuk.medexpertmatch.core.config;

import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/**
 * Adds simulated user role to all Thymeleaf views.
 * isAdmin is true when request has ?user=admin or admin user cookie (no real authentication).
 */
@ControllerAdvice
public class SimulatedUserControllerAdvice {

    @ModelAttribute("isAdmin")
    public boolean isAdmin(WebRequest request) {
        if (AdminAccessGuard.ADMIN_USER_ID.equals(request.getParameter("user"))) {
            return true;
        }
        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletRequest httpRequest = servletWebRequest.getRequest();
            Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (AdminUserCookieFilter.USER_ID_COOKIE.equals(cookie.getName())
                            && AdminAccessGuard.ADMIN_USER_ID.equals(cookie.getValue())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
