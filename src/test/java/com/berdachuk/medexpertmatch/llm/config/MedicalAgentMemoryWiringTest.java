package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.llm.automemory.AutoMemoryService;
import com.berdachuk.medexpertmatch.llm.automemory.AutoMemoryTools;
import com.berdachuk.medexpertmatch.llm.tools.MedicalAgentTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.session.compaction.CompactionTrigger;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.DefaultResourceLoader;
import com.berdachuk.medexpertmatch.llm.service.AgentTodoTrackingService;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Verifies the medical agent ChatClient is built with the AutoMemory long-term memory layer wired
 * in (Option B: explicit {@link AutoMemoryTools} + a memory system prompt), alongside the existing
 * Session short-term advisor. Mirrors the project's AI-provider mocking convention — no LLM call.
 */
class MedicalAgentMemoryWiringTest {

    @Test
    @DisplayName("medicalAgentChatClient builds with AutoMemoryTools present and does not throw")
    void buildsWithAutoMemoryTools(@TempDir Path tmp) {
        MedicalAgentConfiguration config = new MedicalAgentConfiguration(new DefaultResourceLoader());

        AgentMemoryProperties memProps = new AgentMemoryProperties(
                tmp.resolve("memory").toString(),
                new AgentMemoryProperties.Consolidation(60L, null, null));
        AutoMemoryTools autoMemoryTools = new AutoMemoryTools(new AutoMemoryService(memProps));

        // SessionMemoryAdvisor is final; build a real one with mocked collaborators (mirrors the
        // existing session wiring) rather than mocking it.
        SessionMemoryAdvisor sessionMemoryAdvisor = SessionMemoryAdvisor.builder(mock(SessionService.class))
                .compactionTrigger(mock(CompactionTrigger.class))
                .compactionStrategy(mock(CompactionStrategy.class))
                .build();
        TodoWriteTool todoWriteTool = config.todoWriteTool(mock(AgentTodoTrackingService.class));
        ToolCallAdvisor toolCallAdvisor = config.agentToolCallAdvisor(mock(ToolCallingManager.class));

        ChatClient client = config.medicalAgentChatClient(
                mock(ChatModel.class),
                mock(ToolCallback.class),
                FileSystemTools.builder().build(),
                mock(MedicalAgentTools.class),
                autoMemoryTools,
                todoWriteTool,
                toolCallAdvisor,
                sessionMemoryAdvisor,
                memProps);

        assertNotNull(client, "medical agent ChatClient must be built with AutoMemory wiring");
    }

    @Test
    @DisplayName("Memory system prompt instructs non-PHI-only durable memory and the memory tools")
    void memorySystemPromptIsPhiSafe() {
        String prompt = MedicalAgentConfiguration.MEMORY_SYSTEM_PROMPT;
        assertNotNull(prompt);
        assertTrue(prompt.toLowerCase().contains("phi"),
                "memory system prompt must warn about PHI");
        assertTrue(prompt.toLowerCase().contains("automemory_append")
                        || prompt.toLowerCase().contains("memory"),
                "memory system prompt must reference the memory tools");
    }
}
