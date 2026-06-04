package com.berdachuk.medexpertmatch.core.monitoring;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MedGemmaToolCallingMonitorTest {

    @Test
    void shouldInitializeWithDefaultState() {
        ChatModel chatModel = mock(ChatModel.class);
        MedGemmaToolCallingMonitor monitor = new MedGemmaToolCallingMonitor(chatModel);

        assertFalse(monitor.isToolCallingSupported());
        assertNotNull(monitor.getLastCheckTime());
        assertEquals("Not checked yet", monitor.getLastCheckResult());
    }

    @Test
    void shouldHaveLastCheckTimeBeforeNow() throws Exception {
        ChatModel chatModel = mock(ChatModel.class);
        LocalDateTime before = LocalDateTime.now();
        MedGemmaToolCallingMonitor monitor = new MedGemmaToolCallingMonitor(chatModel);
        Thread.sleep(10);
        LocalDateTime checkTime = monitor.getLastCheckTime();

        assertTrue(!checkTime.isBefore(before), "lastCheckTime should not be before construction");
    }

    @Test
    void shouldHandleExceptionDuringCheck() {
        ChatModel chatModel = mock(ChatModel.class);
        MedGemmaToolCallingMonitor monitor = new MedGemmaToolCallingMonitor(chatModel);

        // Invoke the check method - the mock ChatModel will cause a failure, but
        // the method should not throw and should mark tool calling as not supported
        monitor.checkMedGemmaToolCallingSupport();

        assertFalse(monitor.isToolCallingSupported());
        String result = monitor.getLastCheckResult();
        assertNotNull(result);
        // Result will be either "Tool calling not supported" or "Check failed: ..."
        assertTrue(result.startsWith("Tool calling not supported")
                || result.startsWith("Check failed"));
    }
}
