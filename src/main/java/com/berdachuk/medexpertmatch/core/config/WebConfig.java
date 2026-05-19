package com.berdachuk.medexpertmatch.core.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class WebConfig {

    /**
     * Rewrites directory URLs ending with / under /docs/ to index.html,
     * so MkDocs-generated pages (e.g. /docs/presentations/slide/)
     * resolve to /docs/presentations/slide/index.html in the static JAR.
     */
    @Bean
    public FilterRegistrationBean<Filter> docsIndexHtmlFilter() {
        Filter filter = (request, response, chain) -> {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String path = httpRequest.getRequestURI();
            if (path.startsWith("/docs/") && path.endsWith("/")) {
                String rewritten = path.substring(0, path.length() - 1) + "/index.html";
                chain.doFilter(new PathRewriteWrapper(httpRequest, rewritten), response);
            } else {
                chain.doFilter(request, response);
            }
        };
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(-100);
        registration.addUrlPatterns("/docs/*");
        return registration;
    }

    private static class PathRewriteWrapper extends HttpServletRequestWrapper {
        private final String path;

        PathRewriteWrapper(HttpServletRequest request, String path) {
            super(request);
            this.path = path;
        }

        @Override
        public String getRequestURI() {
            return path;
        }

        @Override
        public String getServletPath() {
            return path;
        }
    }
}
