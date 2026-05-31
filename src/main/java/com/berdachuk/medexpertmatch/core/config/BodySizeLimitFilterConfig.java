package com.berdachuk.medexpertmatch.core.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class BodySizeLimitFilterConfig {

    private static final int MAX_JSON_BODY_SIZE = 1_048_576;

    @Bean
    public FilterRegistrationBean<Filter> bodySizeLimitFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new JsonBodySizeFilter(MAX_JSON_BODY_SIZE));
        registration.setOrder(-40);
        registration.addUrlPatterns("/api/*");
        return registration;
    }

    static class JsonBodySizeFilter implements Filter {

        private final int maxSize;

        JsonBodySizeFilter(int maxSize) {
            this.maxSize = maxSize;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            if (httpRequest.getContentLengthLong() > maxSize) {
                httpResponse.setStatus(413);
                httpResponse.setContentType("application/problem+json");
                httpResponse.getWriter().write("""
                        {"type":"about:blank","title":"Payload Too Large","status":413,\
                        "detail":"Request body must not exceed %d bytes","instance":"%s"}\
                        """.formatted(maxSize, httpRequest.getRequestURI()));
                return;
            }

            chain.doFilter(request, response);
        }
    }
}
