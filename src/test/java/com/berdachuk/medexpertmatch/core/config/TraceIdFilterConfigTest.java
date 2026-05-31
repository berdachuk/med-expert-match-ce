package com.berdachuk.medexpertmatch.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class TraceIdFilterConfigTest {

    @Test
    void shouldGenerateTraceIdWhenHeaderMissing() throws Exception {
        var filter = new TraceIdFilterConfig.TraceIdFilter();
        var request = new MockHttpServletRequest("GET", "/api/v1/test");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(response.getHeader("X-Trace-Id"));
        assertEquals(16, response.getHeader("X-Trace-Id").length());
    }

    @Test
    void shouldReuseExistingTraceIdFromHeader() throws Exception {
        var filter = new TraceIdFilterConfig.TraceIdFilter();
        var request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.addHeader("X-Trace-Id", "abc123def456ghi7");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals("abc123def456ghi7", response.getHeader("X-Trace-Id"));
    }

    @Test
    void shouldCleanupMdcAfterRequest() throws Exception {
        var filter = new TraceIdFilterConfig.TraceIdFilter();
        var request = new MockHttpServletRequest("GET", "/api/v1/test");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNull(MDC.get("traceId"));
    }

    @Test
    void shouldPropagateTraceIdToDownstreamRequests() throws Exception {
        var filter = new TraceIdFilterConfig.TraceIdFilter();
        var request = new MockHttpServletRequest("GET", "/api/v1/test");
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String traceId = response.getHeader("X-Trace-Id");
        assertNotNull(traceId);

        var downstreamRequest = new MockHttpServletRequest("GET", "/api/v1/other");
        downstreamRequest.addHeader("X-Trace-Id", traceId);
        var downstreamResponse = new MockHttpServletResponse();
        filter.doFilter(downstreamRequest, downstreamResponse, new MockFilterChain());

        assertEquals(traceId, downstreamResponse.getHeader("X-Trace-Id"));
    }
}
