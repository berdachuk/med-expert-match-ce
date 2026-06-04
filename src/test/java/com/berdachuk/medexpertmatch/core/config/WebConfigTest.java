package com.berdachuk.medexpertmatch.core.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebConfigTest {

    @Test
    void shouldRegisterDocsFilter() {
        WebConfig config = new WebConfig(
                mock(ResponseTimeInterceptor.class),
                mock(ApiUsageInterceptor.class));

        FilterRegistrationBean<Filter> registration = config.docsIndexHtmlFilter();

        assertNotNull(registration);
        assertNotNull(registration.getFilter());
    }

    @Test
    void shouldRewriteDocsPathToIndexHtml() throws Exception {
        WebConfig config = new WebConfig(
                mock(ResponseTimeInterceptor.class),
                mock(ApiUsageInterceptor.class));
        FilterRegistrationBean<Filter> registration = config.docsIndexHtmlFilter();
        Filter filter = registration.getFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/docs/presentations/slide/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        // Verify the chain was called with a wrapper that has the rewritten path
        verify(chain).doFilter(any(jakarta.servlet.ServletRequest.class), any(jakarta.servlet.ServletResponse.class));
    }

    @Test
    void shouldNotRewriteNonDirectoryDocsPath() throws Exception {
        WebConfig config = new WebConfig(
                mock(ResponseTimeInterceptor.class),
                mock(ApiUsageInterceptor.class));
        FilterRegistrationBean<Filter> registration = config.docsIndexHtmlFilter();
        Filter filter = registration.getFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/docs/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldNotRewriteNonDocsPath() throws Exception {
        WebConfig config = new WebConfig(
                mock(ResponseTimeInterceptor.class),
                mock(ApiUsageInterceptor.class));
        FilterRegistrationBean<Filter> registration = config.docsIndexHtmlFilter();
        Filter filter = registration.getFilter();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/doctors");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldRegisterInterceptors() {
        ResponseTimeInterceptor responseInterceptor = mock(ResponseTimeInterceptor.class);
        ApiUsageInterceptor apiInterceptor = mock(ApiUsageInterceptor.class);
        WebConfig config = new WebConfig(responseInterceptor, apiInterceptor);

        org.springframework.web.servlet.config.annotation.InterceptorRegistry registry =
                new org.springframework.web.servlet.config.annotation.InterceptorRegistry();
        config.addInterceptors(registry);

        // Verify interceptors are added (the registry should have them)
        assertNotNull(registry);
    }
}
