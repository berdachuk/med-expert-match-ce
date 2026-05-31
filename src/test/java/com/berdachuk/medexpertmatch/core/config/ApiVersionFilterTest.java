package com.berdachuk.medexpertmatch.core.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ApiVersionFilterTest {

    private final ApiVersionFilter filter = new ApiVersionFilter();

    @Test
    void shouldAddVersionHeaderForV2Requests() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/api/v2/documents/search");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertEquals("2.0", response.getHeader("X-Api-Version"));
    }

    @Test
    void shouldNotAddVersionHeaderForV1Requests() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/api/v1/documents/search");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertNull(response.getHeader("X-Api-Version"));
    }

    @Test
    void shouldNotAddVersionHeaderForNonApiRequests() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/match");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertNull(response.getHeader("X-Api-Version"));
    }

    @Test
    void shouldNotAddVersionHeaderForActuator() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/actuator/health");
        var response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertNull(response.getHeader("X-Api-Version"));
    }
}
