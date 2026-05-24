package com.berdachuk.medexpertmatch.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class BodySizeLimitFilterTest {

    private static final int MAX_SIZE = 100;

    @Test
    void shouldAllowRequestWithinSizeLimit() throws Exception {
        var filter = new BodySizeLimitFilterConfig.JsonBodySizeFilter(MAX_SIZE);
        var request = new MockHttpServletRequest("POST", "/api/v1/test");
        request.setContentType("application/json");
        request.setContent(new byte[50]);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldRejectRequestExceedingSizeLimit() throws Exception {
        var filter = new BodySizeLimitFilterConfig.JsonBodySizeFilter(MAX_SIZE);
        var request = new MockHttpServletRequest("POST", "/api/v1/test");
        request.setContentType("application/json");
        request.setContent(new byte[200]);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(413, response.getStatus());
        assertTrue(response.getContentAsString().contains("Payload Too Large"));
    }

    @Test
    void shouldAllowEmptyRequestBody() throws Exception {
        var filter = new BodySizeLimitFilterConfig.JsonBodySizeFilter(MAX_SIZE);
        var request = new MockHttpServletRequest("GET", "/api/v1/documents/search");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldReturnRfc7807ProblemJson() throws Exception {
        var filter = new BodySizeLimitFilterConfig.JsonBodySizeFilter(MAX_SIZE);
        var request = new MockHttpServletRequest("POST", "/api/v1/agent/match/123");
        request.setContentType("application/json");
        request.setContent(new byte[500]);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("application/problem+json", response.getContentType());
        assertTrue(response.getContentAsString().contains("413"));
    }
}
