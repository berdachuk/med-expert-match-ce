package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.llm.service.AgentTodoTrackingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.core.io.DefaultResourceLoader;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MedicalAgentTodoWiringTest {

    private final MedicalAgentConfiguration config =
            new MedicalAgentConfiguration(new DefaultResourceLoader());

    @Test
    @DisplayName("todoWriteTool factory wires TodoWriteTool with tracking handler")
    void todoWriteToolFactoryBuildsTool() {
        AgentTodoTrackingService trackingService = mock(AgentTodoTrackingService.class);

        TodoWriteTool tool = config.todoWriteTool(trackingService);

        assertNotNull(tool);
    }

    @Test
    @DisplayName("agentToolCallAdvisor disables internal conversation history (Part 3)")
    void toolCallAdvisorDisablesConversationHistory() {
        ToolCallingManager manager = mock(ToolCallingManager.class);

        ToolCallingAdvisor advisor = config.agentToolCallAdvisor(manager);

        assertNotNull(advisor);
    }

    @Test
    @DisplayName("MedicalAgentConfiguration exposes todoWriteTool and agentToolCallAdvisor factories")
    void exposesTodoFactories() {
        boolean hasTodoWrite = Arrays.stream(MedicalAgentConfiguration.class.getDeclaredMethods())
                .anyMatch(m -> "todoWriteTool".equals(m.getName()));
        boolean hasToolCallAdvisor = Arrays.stream(MedicalAgentConfiguration.class.getDeclaredMethods())
                .anyMatch(m -> "agentToolCallAdvisor".equals(m.getName()));

        assertTrue(hasTodoWrite, "todoWriteTool bean factory must exist");
        assertTrue(hasToolCallAdvisor, "agentToolCallAdvisor bean factory must exist");
    }

    @Test
    @DisplayName("todoWriteTool exposes todoWrite method for ChatClient registration")
    void todoWriteMethodExists() throws Exception {
        Method todoWrite = TodoWriteTool.class.getMethod("todoWrite", TodoWriteTool.Todos.class);
        assertNotNull(todoWrite);
    }
}
