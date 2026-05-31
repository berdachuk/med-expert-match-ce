package com.berdachuk.medexpertmatch.core.config;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingConfigTest {

    private static final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void shouldAllowRequestsUnderLimit() throws Exception {
        var filter = new RateLimitingConfig.TokenBucketFilter(new ConcurrentHashMap<>(), 5, 60, new SimpleMeterRegistry());

        for (int i = 0; i < 5; i++) {
            var request = new MockHttpServletRequest("GET", "/api/v1/test");
            var response = new MockHttpServletResponse();
            var chain = new MockFilterChain();
            filter.doFilter(request, response, chain);
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void shouldBlockRequestsOverLimit() throws Exception {
        var filter = new RateLimitingConfig.TokenBucketFilter(new ConcurrentHashMap<>(), 3, 60, new SimpleMeterRegistry());

        for (int i = 0; i < 3; i++) {
            var request = new MockHttpServletRequest("GET", "/api/v1/test");
            var response = new MockHttpServletResponse();
            var chain = new MockFilterChain();
            filter.doFilter(request, response, chain);
            assertEquals(200, response.getStatus(), "Request " + i + " should pass");
        }

        var blockedRequest = new MockHttpServletRequest("GET", "/api/v1/test");
        var blockedResponse = new MockHttpServletResponse();
        var blockedChain = new MockFilterChain();
        filter.doFilter(blockedRequest, blockedResponse, blockedChain);
        assertEquals(429, blockedResponse.getStatus());
        assertNotNull(blockedResponse.getHeader("Retry-After"));
    }

    @Test
    void shouldExcludeHealthEndpointsFromRateLimiting() throws Exception {
        var filter = new RateLimitingConfig.TokenBucketFilter(new ConcurrentHashMap<>(), 1, 60, new SimpleMeterRegistry());

        for (int i = 0; i < 10; i++) {
            var request = new MockHttpServletRequest("GET", "/api/v1/documents/search/health");
            var response = new MockHttpServletResponse();
            var chain = new MockFilterChain();
            filter.doFilter(request, response, chain);
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void shouldTrackSeparateIpsIndependently() throws Exception {
        var filter = new RateLimitingConfig.TokenBucketFilter(new ConcurrentHashMap<>(), 1, 60, new SimpleMeterRegistry());

        var request1 = new MockHttpServletRequest("GET", "/api/v1/test");
        request1.setRemoteAddr("192.168.1.1");
        var response1 = new MockHttpServletResponse();
        filter.doFilter(request1, response1, new MockFilterChain());
        assertEquals(200, response1.getStatus());

        var blockedRequest = new MockHttpServletRequest("GET", "/api/v1/test");
        blockedRequest.setRemoteAddr("192.168.1.1");
        var blockedResponse = new MockHttpServletResponse();
        filter.doFilter(blockedRequest, blockedResponse, new MockFilterChain());
        assertEquals(429, blockedResponse.getStatus());

        var request2 = new MockHttpServletRequest("GET", "/api/v1/test");
        request2.setRemoteAddr("192.168.1.2");
        var response2 = new MockHttpServletResponse();
        filter.doFilter(request2, response2, new MockFilterChain());
        assertEquals(200, response2.getStatus());
    }

    @Test
    void shouldExcludeSyntheticDataProgressFromRateLimiting() throws Exception {
        var filter = new RateLimitingConfig.TokenBucketFilter(new ConcurrentHashMap<>(), 1, 60, new SimpleMeterRegistry());

        for (int i = 0; i < 10; i++) {
            var request = new MockHttpServletRequest("GET",
                    "/api/v1/synthetic-data/progress/885871a0-29f6-4dd5-aad2-e5c398dd67f2");
            var response = new MockHttpServletResponse();
            var chain = new MockFilterChain();
            filter.doFilter(request, response, chain);
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void shouldExcludeAdminEndpointsFromRateLimiting() throws Exception {
        var filter = new RateLimitingConfig.TokenBucketFilter(new ConcurrentHashMap<>(), 1, 60, registry);

        for (int i = 0; i < 5; i++) {
            var request = new MockHttpServletRequest("GET", "/api/v1/admin/session-tokens");
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void shouldExcludeChatAndA2aEndpointsFromRateLimiting() throws Exception {
        var filter = new RateLimitingConfig.TokenBucketFilter(new ConcurrentHashMap<>(), 1, 60, registry);

        for (int i = 0; i < 10; i++) {
            var chatRequest = new MockHttpServletRequest("DELETE", "/api/v1/chats/chat-id");
            var chatResponse = new MockHttpServletResponse();
            filter.doFilter(chatRequest, chatResponse, new MockFilterChain());
            assertEquals(200, chatResponse.getStatus());

            var a2aRequest = new MockHttpServletRequest("POST", "/a2a/v1/jsonrpc");
            var a2aResponse = new MockHttpServletResponse();
            filter.doFilter(a2aRequest, a2aResponse, new MockFilterChain());
            assertEquals(200, a2aResponse.getStatus());
        }
    }

    @Test
    void shouldRespectXForwardedForHeader() throws Exception {
        var filter = new RateLimitingConfig.TokenBucketFilter(new ConcurrentHashMap<>(), 1, 60, new SimpleMeterRegistry());

        var request = new MockHttpServletRequest("GET", "/api/v1/test");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
        request.setRemoteAddr("127.0.0.1");
        var response1 = new MockHttpServletResponse();
        filter.doFilter(request, response1, new MockFilterChain());
        assertEquals(200, response1.getStatus());

        var request2 = new MockHttpServletRequest("GET", "/api/v1/test");
        request2.addHeader("X-Forwarded-For", "10.0.0.1, 192.168.1.1");
        request2.setRemoteAddr("127.0.0.1");
        var response2 = new MockHttpServletResponse();
        filter.doFilter(request2, response2, new MockFilterChain());
        assertEquals(429, response2.getStatus());
    }
}
