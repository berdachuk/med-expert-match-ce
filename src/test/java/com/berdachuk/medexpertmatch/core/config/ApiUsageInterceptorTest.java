package com.berdachuk.medexpertmatch.core.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class ApiUsageInterceptorTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final ApiUsageInterceptor interceptor = new ApiUsageInterceptor(registry);

    @Test
    void shouldSetStartTimeAttribute() {
        var request = new MockHttpServletRequest("GET", "/api/v1/agent/match/123");
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), null));
        assertNotNull(request.getAttribute("apiUsageStart"));
    }

    @Test
    void shouldRecordSuccessfulRequestMetrics() {
        var request = new MockHttpServletRequest("POST", "/api/v1/evaluation/run");
        var response = new MockHttpServletResponse();
        interceptor.preHandle(request, response, null);
        interceptor.afterCompletion(request, response, null, null);

        var requestCounter = registry.find("medexpertmatch.api.usage.requests").counter();
        assertNotNull(requestCounter);
        assertTrue(requestCounter.count() >= 1);

        var latencyTimer = registry.find("medexpertmatch.api.usage.latency").timer();
        assertNotNull(latencyTimer);
        assertTrue(latencyTimer.count() >= 1);
    }

    @Test
    void shouldRecordErrorMetricsFor4xx() {
        var request = new MockHttpServletRequest("GET", "/api/v1/agent/match/nonexistent");
        var response = new MockHttpServletResponse();
        response.setStatus(404);
        interceptor.preHandle(request, response, null);
        interceptor.afterCompletion(request, response, null, null);

        var errorCounter = registry.find("medexpertmatch.api.usage.errors").counter();
        assertNotNull(errorCounter);
        assertTrue(errorCounter.count() >= 1);
    }

    @Test
    void shouldNotRecordMetricsWithoutPreHandle() {
        var request = new MockHttpServletRequest("GET", "/api/v1/test");
        var response = new MockHttpServletResponse();
        interceptor.afterCompletion(request, response, null, null);

        assertEquals(0, registry.getMeters().size());
    }
}
