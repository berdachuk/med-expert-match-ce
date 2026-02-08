package com.berdachuk.medexpertmatch.core.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Monitors MedGemma model for tool calling support.
 * Periodically checks if MedGemma has gained tool calling capabilities.
 * When tool calling is detected, logs the information for future migration.
 */
@Slf4j
@Service
@ConditionalOnProperty(
        name = "medexpertmatch.medgemma.tool-calling-check-enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class MedGemmaToolCallingMonitor {

    private final ChatModel medGemmaModel;
    private final AtomicBoolean toolCallingSupported = new AtomicBoolean(false);
    private final AtomicReference<LocalDateTime> lastCheckTime = new AtomicReference<>(LocalDateTime.now());
    private final AtomicReference<String> lastCheckResult = new AtomicReference<>("Not checked yet");

    public MedGemmaToolCallingMonitor(
            @Qualifier("primaryChatModel") ChatModel primaryChatModel) {
        this.medGemmaModel = primaryChatModel;
        log.info("MedGemmaToolCallingMonitor initialized. Will check for tool calling support periodically.");
    }

    /**
     * Checks if MedGemma supports tool calling by attempting a simple tool call.
     * Runs periodically based on configured interval.
     */
    @Scheduled(fixedDelayString = "${medexpertmatch.medgemma.tool-calling-check-interval:3600000}")
    public void checkMedGemmaToolCallingSupport() {
        log.info("Checking MedGemma tool calling support...");
        lastCheckTime.set(LocalDateTime.now());

        try {
            // Create a simple test tool component
            TestToolComponent testTool = new TestToolComponent();
            ChatClient testClient = ChatClient.builder(medGemmaModel)
                    .defaultTools(testTool)
                    .build();

            // Try a simple tool call
            String response = testClient.prompt()
                    .user("Call the test_tool function with parameter 'test'")
                    .call()
                    .content();

            // Check if response indicates tool calling worked
            // If we get a response mentioning the tool or tool calls, it might be supported
            boolean supportsTools = response != null &&
                    (response.contains("test_tool") || response.contains("Tool called successfully") ||
                            response.contains("tool_calls") || response.contains("function_call"));

            if (supportsTools) {
                toolCallingSupported.set(true);
                lastCheckResult.set("Tool calling appears to be supported! Response: " +
                        (response.length() > 100 ? response.substring(0, 100) + "..." : response));
                log.warn("=== MEDGEMMA TOOL CALLING DETECTED ===");
                log.warn("MedGemma appears to support tool calling. Consider migrating from FunctionGemma.");
                log.warn("Response: {}", response);
            } else {
                toolCallingSupported.set(false);
                lastCheckResult.set("Tool calling not supported. Response: " +
                        (response != null && response.length() > 100 ? response.substring(0, 100) + "..." : response));
                log.debug("MedGemma tool calling check: Not supported yet. Response: {}",
                        response != null && response.length() > 200 ? response.substring(0, 200) + "..." : response);
            }
        } catch (Exception e) {
            toolCallingSupported.set(false);
            lastCheckResult.set("Check failed: " + e.getMessage());
            log.debug("MedGemma tool calling check failed (expected if not supported): {}", e.getMessage());
        }
    }

    /**
     * Gets the current status of MedGemma tool calling support.
     *
     * @return true if tool calling is supported, false otherwise
     */
    public boolean isToolCallingSupported() {
        return toolCallingSupported.get();
    }

    /**
     * Gets the last check time.
     *
     * @return Last check timestamp
     */
    public LocalDateTime getLastCheckTime() {
        return lastCheckTime.get();
    }

    /**
     * Gets the last check result.
     *
     * @return Last check result description
     */
    public String getLastCheckResult() {
        return lastCheckResult.get();
    }

    /**
     * Simple test tool component for checking tool calling support.
     */
    @org.springframework.stereotype.Component
    private static class TestToolComponent {
        @org.springframework.ai.tool.annotation.Tool(description = "Test tool for checking tool calling support")
        public String test_tool(@org.springframework.ai.tool.annotation.ToolParam(description = "Test parameter") String param) {
            return "Tool called successfully with parameter: " + param;
        }
    }
}
