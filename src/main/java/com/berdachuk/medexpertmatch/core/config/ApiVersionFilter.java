package com.berdachuk.medexpertmatch.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiVersionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiVersionFilter.class);
    private static final String ACCEPT = "Accept";
    private static final String V2_PREFIX = "/api/v2/";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        if (!path.startsWith(V2_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        response.setHeader("X-Api-Version", "2.0");
        chain.doFilter(request, response);
    }
}
