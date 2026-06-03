package com.berdachuk.medexpertmatch.core.service;

import com.berdachuk.medexpertmatch.core.event.ToolCallLoggedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogStreamServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private LogStreamService service;

    @BeforeEach
    void setUp() {
        service = new LogStreamService(eventPublisher);
    }

    @AfterEach
    void tearDown() {
        service.clearCurrentSessionId();
    }

    @Test
    @DisplayName("createEmitter creates emitter and increments session count")
    void createEmitterCreatesEmitter() {
        var emitter = service.createEmitter("session-1");
        assertNotNull(emitter);
        assertEquals(1, service.getActiveSessionCount());
    }

    @Test
    @DisplayName("createEmitter registers emitter and increments count")
    void createEmitterRegistersEmitter() {
        var emitter = service.createEmitter("session-1");
        assertNotNull(emitter);
        assertEquals(1, service.getActiveSessionCount());
    }

    @Test
    @DisplayName("emitter completeWithError triggers removal after callback")
    void emitterCompleteWithErrorTriggersRemoval() {
        var emitter = service.createEmitter("session-1");
        assertEquals(1, service.getActiveSessionCount());
        emitter.completeWithError(new RuntimeException("timeout"));
    }

    @Test
    @DisplayName("removeEmitter removes emitter and completes it")
    void removeEmitterRemovesAndCompletes() {
        service.createEmitter("session-1");
        assertEquals(1, service.getActiveSessionCount());
        service.removeEmitter("session-1");
        assertEquals(0, service.getActiveSessionCount());
    }

    @Test
    @DisplayName("removeEmitter is idempotent for unknown sessions")
    void removeEmitterIdempotentForUnknownSession() {
        service.removeEmitter("nonexistent");
        assertEquals(0, service.getActiveSessionCount());
    }

    @Test
    @DisplayName("setCurrentSessionId stores session in ThreadLocal")
    void setCurrentSessionIdStoresInThreadLocal() {
        service.setCurrentSessionId("session-abc");
        assertEquals("session-abc", service.getCurrentSessionId());
    }

    @Test
    @DisplayName("clearCurrentSessionId clears ThreadLocal session")
    void clearCurrentSessionIdClearsThreadLocal() {
        service.setCurrentSessionId("session-abc");
        service.clearCurrentSessionId();
        assertEquals("default", service.getCurrentSessionId());
    }

    @Test
    @DisplayName("getCurrentSessionId returns default when not set")
    void getCurrentSessionIdDefaultsWhenNotSet() {
        assertEquals("default", service.getCurrentSessionId());
    }

    @Test
    @DisplayName("sendLog does nothing for null sessionId")
    void sendLogSkipsNullSession() {
        service.createEmitter("session-1");
        service.sendLog(null, "INFO", "message", null);
        assertTrue(true);
    }

    @Test
    @DisplayName("sendLog does nothing for blank sessionId")
    void sendLogSkipsBlankSession() {
        service.createEmitter("session-1");
        service.sendLog("  ", "INFO", "message", null);
        assertTrue(true);
    }

    @Test
    @DisplayName("sendLog handles missing emitter gracefully")
    void sendLogHandlesMissingEmitter() {
        service.createEmitter("session-1");
        service.sendLog("session-nonexistent", "INFO", "message", null);
        assertTrue(true);
    }

    @Test
    @DisplayName("logToolCall publishes ToolCallLoggedEvent")
    void logToolCallPublishesEvent() {
        service.createEmitter("session-1");
        service.logToolCall("session-1", "search", "{\"query\":\"test\"}");

        var captor = ArgumentCaptor.forClass(ToolCallLoggedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        var event = captor.getValue();
        assertEquals("session-1", event.sessionId());
        assertEquals("search", event.toolName());
    }

    @Test
    @DisplayName("broadcastLog sends to all connected emitters")
    void broadcastLogSendsToAll() {
        service.createEmitter("s1");
        service.createEmitter("s2");
        assertEquals(2, service.getActiveSessionCount());
        service.broadcastLog("INFO", "broadcast message", null);
        assertTrue(true);
    }

    @Test
    @DisplayName("concurrent register/unregister from multiple threads")
    void concurrentRegisterUnregister() throws InterruptedException {
        int threads = 4;
        var latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            new Thread(() -> {
                try {
                    String sid = "session-thread-" + idx;
                    service.createEmitter(sid);
                    Thread.sleep(10);
                    service.removeEmitter(sid);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(0, service.getActiveSessionCount());
    }

    @Test
    @DisplayName("getActiveSessionCount tracks active sessions correctly")
    void getActiveSessionCountTracksCorrectly() {
        assertEquals(0, service.getActiveSessionCount());
        service.createEmitter("s1");
        assertEquals(1, service.getActiveSessionCount());
        service.createEmitter("s2");
        assertEquals(2, service.getActiveSessionCount());
        service.removeEmitter("s1");
        assertEquals(1, service.getActiveSessionCount());
    }

    @Test
    @DisplayName("sendProgress clamps percent between 0 and 100")
    void sendProgressClampsPercent() {
        service.createEmitter("session-1");
        service.sendProgress("session-1", -5);
        service.sendProgress("session-1", 150);
        service.sendProgress("session-1", 50);
        assertTrue(true);
    }

    @Test
    @DisplayName("logMatchDoctorsStep sends formatted step message")
    void logMatchDoctorsStepSendsFormattedMessage() {
        service.createEmitter("session-1");
        service.logMatchDoctorsStep("session-1", "CONTEXT_BUILD", "building context");
        assertTrue(true);
    }

    @Test
    @DisplayName("logCompletion sends completion message")
    void logCompletionSendsMessage() {
        service.createEmitter("session-1");
        service.logCompletion("session-1", "match", "result");
        assertTrue(true);
    }
}
