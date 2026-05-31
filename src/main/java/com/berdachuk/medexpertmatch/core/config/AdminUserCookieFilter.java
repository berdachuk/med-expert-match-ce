package com.berdachuk.medexpertmatch.core.config;

import com.berdachuk.medexpertmatch.core.security.AdminAccessGuard;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Persists simulated admin identity in a cookie when {@code ?user=admin} is present (M23).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AdminUserCookieFilter extends OncePerRequestFilter {

    static final String USER_ID_COOKIE = "medexpertmatch-user-id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (AdminAccessGuard.ADMIN_USER_ID.equals(request.getParameter("user"))) {
            Cookie cookie = new Cookie(USER_ID_COOKIE, AdminAccessGuard.ADMIN_USER_ID);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            response.addCookie(cookie);
        }
        filterChain.doFilter(request, response);
    }
}
