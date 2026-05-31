package com.berdachuk.medexpertmatch.llm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springaicommunity.agent.tools.task.TaskTool;
import org.springaicommunity.agent.tools.task.claude.ClaudeSubagentReferences;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MedicalSubagentRoutingTest {

    private final MedicalAgentConfiguration config =
            new MedicalAgentConfiguration(new DefaultResourceLoader());

    @Test
    @DisplayName("ClaudeSubagentReferences loads seven specialist/orchestrator agents")
    void loadsAgentDefinitions() throws Exception {
        var resolver = new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
        var resources = resolver.getResources("classpath:agents/*.md");
        var refs = ClaudeSubagentReferences.fromResources(resources);
        assertTrue(refs.size() >= 7, "expected auto + 6 specialists");
    }

    @Test
    @DisplayName("TaskTool factory builds from classpath agent resources")
    void taskToolBuilds() {
        ToolCallback taskTool = config.taskTool(mock(ChatModel.class), new DefaultResourceLoader());
        assertNotNull(taskTool);
    }

    @Test
    @DisplayName("TaskTool builder exposes task delegation API")
    void taskToolBuilderExists() throws Exception {
        assertNotNull(TaskTool.class.getMethod("builder"));
    }
}
