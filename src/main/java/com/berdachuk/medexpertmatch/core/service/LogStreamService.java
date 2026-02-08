package com.berdachuk.medexpertmatch.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Thread-local storage for session ID to allow tools to access it.
 */
class SessionContext {
    private static final ThreadLocal<String> SESSION_ID = new ThreadLocal<>();

    static String getSessionId() {
        String sessionId = SESSION_ID.get();
        return sessionId != null ? sessionId : "default";
    }

    static void setSessionId(String sessionId) {
        SESSION_ID.set(sessionId);
    }

    static void clear() {
        SESSION_ID.remove();
    }
}

/**
 * Service for streaming execution traces and logs to UI clients via Server-Sent Events (SSE).
 */
@Slf4j
@Service
public class LogStreamService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);

    /**
     * Creates a new SSE emitter for a client session.
     *
     * @param sessionId Unique session identifier
     * @return SseEmitter for streaming logs
     */
    public SseEmitter createEmitter(String sessionId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(sessionId, emitter);

        // Handle completion and timeout
        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed for session: {}", sessionId);
            emitters.remove(sessionId);
        });
        emitter.onTimeout(() -> {
            log.debug("SSE emitter timeout for session: {}", sessionId);
            emitters.remove(sessionId);
        });
        emitter.onError((ex) -> {
            log.error("SSE emitter error for session: {}", sessionId, ex);
            emitters.remove(sessionId);
        });

        // Send initial connection message
        sendLog(sessionId, "INFO", "Execution trace connected", null);

        return emitter;
    }

    /**
     * Gets the current thread's session ID.
     *
     * @return Session identifier
     */
    public String getCurrentSessionId() {
        return SessionContext.getSessionId();
    }

    /**
     * Sets the current thread's session ID for log streaming.
     *
     * @param sessionId Session identifier
     */
    public void setCurrentSessionId(String sessionId) {
        SessionContext.setSessionId(sessionId);
    }

    /**
     * Clears the current thread's session ID.
     */
    public void clearCurrentSessionId() {
        SessionContext.clear();
    }

    /**
     * Sends a log message to all connected clients.
     *
     * @param level   Log level (INFO, DEBUG, WARN, ERROR)
     * @param message Log message
     * @param details Additional details (optional)
     */
    public void broadcastLog(String level, String message, String details) {
        emitters.forEach((sessionId, emitter) -> {
            sendLog(sessionId, level, message, details);
        });
    }

    /**
     * Sends a log message to a specific client session.
     *
     * @param sessionId Session identifier
     * @param level     Log level
     * @param message   Log message
     * @param details   Additional details (optional)
     */
    public void sendLog(String sessionId, String level, String message, String details) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter == null) {
            log.warn("No emitter found for session: {} (available sessions: {})", sessionId, emitters.keySet());
            // Try to send to all sessions as fallback
            if (!emitters.isEmpty()) {
                log.info("Broadcasting log to all available sessions instead");
                broadcastLog(level, message, details);
            }
            return;
        }

        try {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);
            if (details != null && !details.isEmpty()) {
                logEntry += "\n" + details;
            }

            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .name("log")
                    .data(logEntry);

            emitter.send(event);
            log.debug("Sent log to session {}: {}", sessionId, message);
        } catch (IOException e) {
            log.warn("Failed to send log to session {}: {}", sessionId, e.getMessage());
            emitters.remove(sessionId);
        }
    }

    /**
     * Logs execution step for match doctors operation.
     */
    public void logMatchDoctorsStep(String sessionId, String step, String details) {
        sendLog(sessionId, "INFO", "Step: " + step, details);
    }

    /**
     * Logs tool invocation.
     */
    public void logToolCall(String sessionId, String toolName, String parameters) {
        sendLog(sessionId, "DEBUG", "Tool called: " + toolName, parameters);
    }

    /**
     * Logs tool result.
     */
    public void logToolResult(String sessionId, String toolName, String result) {
        sendLog(sessionId, "DEBUG", "Tool result: " + toolName, result != null && result.length() > 200
                ? result.substring(0, 200) + "..." : result);
    }

    /**
     * Logs error.
     */
    public void logError(String sessionId, String error, String stackTrace) {
        sendLog(sessionId, "ERROR", "Error: " + error, stackTrace);
    }

    /**
     * Logs completion of operation.
     */
    public void logCompletion(String sessionId, String operation, String result) {
        sendLog(sessionId, "INFO", "Completed: " + operation, result);
    }

    /**
     * Sends progress percentage (0-100) for UI progress bar. Message format [PROGRESS:nn] for frontend parsing.
     */
    public void sendProgress(String sessionId, int percent) {
        sendLog(sessionId, "INFO", "[PROGRESS:" + Math.min(100, Math.max(0, percent)) + "]", null);
    }

    /**
     * Removes emitter for a session.
     */
    public void removeEmitter(String sessionId) {
        SseEmitter emitter = emitters.remove(sessionId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("Error completing emitter for session {}: {}", sessionId, e.getMessage());
            }
        }
    }

    /**
     * Gets the count of active sessions.
     *
     * @return Number of active SSE connections
     */
    public int getActiveSessionCount() {
        return emitters.size();
    }
}
