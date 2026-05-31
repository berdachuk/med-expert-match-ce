package com.berdachuk.medexpertmatch.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class ResponseTimeInterceptorTest {

    @Test
    void shouldSetStartTimeAttribute() {
        var interceptor = new ResponseTimeInterceptor();
        var request = new MockHttpServletRequest("GET", "/api/v1/test");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), null);

        assertTrue(result);
        assertNotNull(request.getAttribute("startTime"));
        assertTrue(request.getAttribute("startTime") instanceof Long);
    }

    @Test
    void shouldNotCrashWhenNoStartTimeAttribute() {
        var interceptor = new ResponseTimeInterceptor();
        var request = new MockHttpServletRequest("GET", "/api/v1/test");
        var response = new MockHttpServletResponse();

        interceptor.afterCompletion(request, response, null, null);

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldRecordCompletionWithoutException() {
        var interceptor = new ResponseTimeInterceptor();
        var request = new MockHttpServletRequest("POST", "/api/v1/evaluation/run");
        var response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, null);
        interceptor.afterCompletion(request, response, null, null);

        assertEquals(200, response.getStatus());
    }

    @Test
    void shouldRecordCompletionWithException() {
        var interceptor = new ResponseTimeInterceptor();
        var request = new MockHttpServletRequest("GET", "/api/v1/not-found");
        var response = new MockHttpServletResponse();
        response.setStatus(404);

        interceptor.preHandle(request, response, null);
        interceptor.afterCompletion(request, response, null, new RuntimeException("test error"));

        assertEquals(404, response.getStatus());
    }

    @Test
    void shouldHandleSubsequentCallsOnSameRequest() {
        var interceptor = new ResponseTimeInterceptor();
        var request = new MockHttpServletRequest("GET", "/api/v1/test");
        var response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, null);
        interceptor.afterCompletion(request, response, null, null);

        interceptor.preHandle(request, response, null);
        interceptor.afterCompletion(request, response, null, null);
    }
}
