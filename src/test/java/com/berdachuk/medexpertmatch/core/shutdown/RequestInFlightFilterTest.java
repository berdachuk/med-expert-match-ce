package com.berdachuk.medexpertmatch.core.shutdown;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequestInFlightFilterTest {

    @Test
    void shouldIncrementAndDecrementInFlightCount() throws Exception {
        int before = RequestInFlightFilter.getInFlightCount();
        RequestInFlightFilter filter = new RequestInFlightFilter();

        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        assertEquals(before, RequestInFlightFilter.getInFlightCount());
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldDecrementEvenOnException() throws Exception {
        int before = RequestInFlightFilter.getInFlightCount();
        RequestInFlightFilter filter = new RequestInFlightFilter();

        FilterChain chain = mock(FilterChain.class);
        doThrow(new RuntimeException("boom")).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(request, response, chain);
        } catch (RuntimeException expected) {
            // expected
        }

        assertEquals(before, RequestInFlightFilter.getInFlightCount());
    }

    @Test
    void shouldTrackMultipleInFlightRequests() throws Exception {
        int before = RequestInFlightFilter.getInFlightCount();
        RequestInFlightFilter filter = new RequestInFlightFilter();

        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            // While chain is invoked, in-flight should be > baseline
            assertTrue(RequestInFlightFilter.getInFlightCount() > before);
            return null;
        }).when(chain).doFilter(any(ServletRequest.class), any(ServletResponse.class));

        filter.doFilter(new MockHttpServletRequest("GET", "/api/a"), new MockHttpServletResponse(), chain);
        assertEquals(before, RequestInFlightFilter.getInFlightCount());
    }

    @Test
    void shouldHandleNonHttpRequest() throws Exception {
        int before = RequestInFlightFilter.getInFlightCount();
        RequestInFlightFilter filter = new RequestInFlightFilter();

        FilterChain chain = mock(FilterChain.class);
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);

        filter.doFilter(request, response, chain);

        assertEquals(before, RequestInFlightFilter.getInFlightCount());
        verify(chain).doFilter(request, response);
    }

    private static void assertTrue(boolean condition) {
        org.junit.jupiter.api.Assertions.assertTrue(condition);
    }
}
