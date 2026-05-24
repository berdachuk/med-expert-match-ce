package com.berdachuk.medexpertmatch.core.config;

import com.berdachuk.medexpertmatch.core.exception.MedExpertMatchException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

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
}
