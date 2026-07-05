package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.llm.automemory.AutoMemoryTools;
import com.berdachuk.medexpertmatch.llm.domain.AgentThinking;
import com.berdachuk.medexpertmatch.llm.harness.ToolScopeEnforcingResolver;
import com.berdachuk.medexpertmatch.llm.tool.NormalizingToolCallbackResolver;
import com.berdachuk.medexpertmatch.llm.tool.ToolSelectionGuardingResolver;
import com.berdachuk.medexpertmatch.llm.tools.*;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.augment.AugmentedToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.*;

/**
 * Hardens function-calling tool resolution for local models that emit non-canonical tool names.
 */
@Configuration
@ConditionalOnProperty(
        name = "medexpertmatch.skills.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AgentToolCallingConfiguration {

    @Bean
    @Primary
    ToolCallbackResolver toolCallbackResolver(
            List<ToolCallback> toolCallbacks,
            ObjectProvider<ToolCallbackProvider> toolCallbackProviders,
            CaseAnalysisAgentTools caseAnalysisAgentTools,
            DoctorMatchingAgentTools doctorMatchingAgentTools,
            EvidenceAgentTools evidenceAgentTools,
            ClinicalAdvisorAgentTools clinicalAdvisorAgentTools,
            GraphAnalyticsAgentTools graphAnalyticsAgentTools,
            RoutingAgentTools routingAgentTools,
            ContextBuilderAgentTools contextBuilderAgentTools,
            DateTimeAgentTools dateTimeAgentTools,
            AutoMemoryTools autoMemoryTools,
            TodoWriteTool todoWriteTool,
            AskUserQuestionTool askUserQuestionTool) {
        List<ToolCallback> merged = new ArrayList<>(toolCallbacks);
        toolCallbackProviders.orderedStream()
                .forEach(provider -> merged.addAll(Arrays.asList(provider.getToolCallbacks())));

        List<Object> toolObjects = Arrays.asList(
                caseAnalysisAgentTools, doctorMatchingAgentTools, evidenceAgentTools,
                clinicalAdvisorAgentTools, graphAnalyticsAgentTools, routingAgentTools,
                contextBuilderAgentTools, dateTimeAgentTools, autoMemoryTools,
                todoWriteTool, askUserQuestionTool);

        for (Object toolObject : toolObjects) {
            AugmentedToolCallbackProvider<AgentThinking> augmented =
                    AugmentedToolCallbackProvider.<AgentThinking>builder()
                            .toolObject(toolObject)
                            .argumentType(AgentThinking.class)
                            .argumentConsumer(event -> {})
                            .build();
            merged.addAll(Arrays.asList(augmented.getToolCallbacks()));
        }

        Map<String, ToolCallback> uniqueByName = new LinkedHashMap<>();
        for (ToolCallback callback : merged) {
            uniqueByName.put(callback.getToolDefinition().name(), callback);
        }
        List<ToolCallback> uniqueCallbacks = List.copyOf(uniqueByName.values());
        ToolCallbackResolver delegate = new StaticToolCallbackResolver(uniqueCallbacks);
        ToolCallbackResolver normalizing = new NormalizingToolCallbackResolver(delegate, uniqueCallbacks);
        ToolCallbackResolver guarded = new ToolSelectionGuardingResolver(normalizing);
        return new ToolScopeEnforcingResolver(guarded);
    }
}
