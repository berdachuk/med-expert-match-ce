package com.berdachuk.medexpertmatch.core.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyAuthFilterTest {

    private ApiKeyAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthFilter("key-a,key-b,key-c");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    @Test
    void shouldAllowRequestWithValidApiKey() throws ServletException, IOException {
        request.addHeader("X-API-Key", "key-a");
        request.setRequestURI("/api/v1/agent/match/123");

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldAllowRequestWithSecondValidKey() throws ServletException, IOException {
        request.addHeader("X-API-Key", "key-b");
        request.setRequestURI("/api/v1/documents/search");

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldRejectRequestWithInvalidApiKey() throws ServletException, IOException {
        request.addHeader("X-API-Key", "wrong-key");
        request.setRequestURI("/api/v1/agent/match/123");

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Unauthorized"));
    }

    @Test
    void shouldRejectRequestWithNoApiKey() throws ServletException, IOException {
        request.setRequestURI("/api/v1/agent/match/123");

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldAllowHealthEndpointWithoutKey() throws ServletException, IOException {
        request.setRequestURI("/actuator/health");

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldAllowActuatorEndpointsWithoutKey() throws ServletException, IOException {
        request.setRequestURI("/actuator/prometheus");

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldAllowOpenApiDocsWithoutKey() throws ServletException, IOException {
        request.setRequestURI("/api/v1/openapi.json");

        filter.doFilterInternal(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldReturnProblemJsonOnRejection() throws ServletException, IOException {
        request.setRequestURI("/api/v1/agent/match/123");

        filter.doFilterInternal(request, response, chain);

        assertEquals("application/problem+json", response.getContentType());
        String body = response.getContentAsString();
        assertTrue(body.contains("401"));
        assertTrue(body.contains("X-API-Key"));
    }

    @Test
    void emptyApiKeysShouldRejectAll() throws ServletException, IOException {
        var emptyFilter = new ApiKeyAuthFilter("");
        request.addHeader("X-API-Key", "anything");
        request.setRequestURI("/api/v1/test");

        emptyFilter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
    }
}
