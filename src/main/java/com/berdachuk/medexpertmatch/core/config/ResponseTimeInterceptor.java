package com.berdachuk.medexpertmatch.core.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ResponseTimeInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ResponseTimeInterceptor.class);
    private static final long SLOW_THRESHOLD_MS = 1000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute("startTime");
        if (startTime == null) {
            return;
        }
        long durationMs = System.currentTimeMillis() - startTime;
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();

        if (durationMs > SLOW_THRESHOLD_MS) {
            log.warn("SLOW REQUEST: {} {} completed in {}ms with status {}", method, uri, durationMs, status);
        } else {
            log.debug("{} {} completed in {}ms with status {}", method, uri, durationMs, status);
        }
    }
}
