package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Controller for streaming execution traces and logs to UI via Server-Sent Events (SSE).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/logs")
public class LogStreamController {

    private final LogStreamService logStreamService;

    public LogStreamController(LogStreamService logStreamService) {
        this.logStreamService = logStreamService;
    }

    /**
     * Creates an SSE connection for streaming execution traces.
     *
     * @param sessionId Optional session ID, generates new one if not provided
     * @return SseEmitter for streaming logs
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(@RequestParam(required = false) String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }
        log.info("Creating SSE log stream for session: {} (total active sessions: {})",
                sessionId, logStreamService.getActiveSessionCount());
        return logStreamService.createEmitter(sessionId);
    }
}
