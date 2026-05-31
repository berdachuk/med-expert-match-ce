package com.berdachuk.medexpertmatch.core.config;

import com.berdachuk.medexpertmatch.core.exception.MedExpertMatchException;
import com.berdachuk.medexpertmatch.core.exception.RateLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturn400ForIllegalArgumentException() {
        var request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/v1/test"));
        var problem = handler.handleIllegalArgument(
                new IllegalArgumentException("caseText is required"), request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("Invalid Request", problem.getTitle());
        assertEquals("caseText is required", problem.getDetail());
        assertEquals("VALIDATION_FAILED", problem.getProperties().get("errorCode"));
    }

    @Test
    void shouldReturn500ForMedExpertMatchException() {
        var request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/v1/agent/match/123"));
        var problem = handler.handleMedExpertMatch(
                new MedExpertMatchException("GRAPH_QUERY_FAILED", "Failed to query AGE graph"),
                request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
        assertEquals("Application Error", problem.getTitle());
        assertEquals("GRAPH_QUERY_FAILED", problem.getProperties().get("errorCode"));
    }

    @Test
    void shouldReturn404ForMissingFaviconWithout500() {
        var request = new ServletWebRequest(new MockHttpServletRequest("GET", "/favicon.ico"));
        var problem = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/favicon.ico", "static"),
                request);

        assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
        assertEquals("Not Found", problem.getTitle());
        assertEquals("NOT_FOUND", problem.getProperties().get("errorCode"));
    }

    @Test
    void shouldReturn500ForGenericException() {
        var request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/v1/agent/prioritize-consults"));
        var problem = handler.handleGeneric(
                new RuntimeException("Something broke"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
        assertEquals("Internal Server Error", problem.getTitle());
        assertEquals("INTERNAL_ERROR", problem.getProperties().get("errorCode"));
        assertNotNull(problem.getInstance());
    }

    @Test
    void shouldNotExposeStackTraceInResponse() {
        var request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/v1/test"));
        var problem = handler.handleGeneric(
                new RuntimeException("Database connection failed"), request);

        String detail = problem.getDetail();
        assertNotNull(detail);
        assertFalse(detail.contains("stacktrace"), "Should not contain stacktrace");
        assertFalse(detail.contains("\tat "), "Should not contain stack frames");
        assertEquals("An unexpected error occurred", detail);
    }

    @Test
    void shouldSetProblemInstanceUri() {
        var request = new ServletWebRequest(new MockHttpServletRequest("POST", "/api/v1/evaluation/run"));
        var problem = handler.handleIllegalArgument(
                new IllegalArgumentException("Invalid input"), request);

        assertNotNull(problem.getInstance());
        assertTrue(problem.getInstance().toString().contains("evaluation"));
    }

    @Test
    void shouldHandleMedExpertMatchWithErrorCode() {
        var request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/v1/test"));
        var problem = handler.handleMedExpertMatch(
                new MedExpertMatchException("DATABASE_CONNECTION_FAILED", "Connection lost"),
                request);

        assertEquals("DATABASE_CONNECTION_FAILED", problem.getProperties().get("errorCode"));
        assertTrue(problem.getDetail().contains("internal application error"));
    }

    @Test
    @DisplayName("RateLimitExceededException includes Retry-After header")
    void rateLimitIncludesRetryAfter() {
        var request = new ServletWebRequest(
                new MockHttpServletRequest("POST", "/api/v1/chats/x/messages/stream"));
        var response = handler.handleRateLimitExceeded(
                new RateLimitExceededException("Chat rate limit exceeded", 60),
                request);

        assertEquals(429, response.getStatusCode().value());
        assertEquals("60", response.getHeaders().getFirst("Retry-After"));
        assertEquals("Chat rate limit exceeded", response.getBody().getDetail());
    }

    @Test
    @DisplayName("ResponseStatusException preserves HTTP status (e.g. 429)")
    void preservesResponseStatus() {
        var request = new ServletWebRequest(
                new MockHttpServletRequest("POST", "/api/v1/chats/x/messages/stream"));
        var problem = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Chat rate limit exceeded"),
                request);

        assertEquals(429, problem.getStatus());
        assertEquals("Chat rate limit exceeded", problem.getDetail());
    }
}
