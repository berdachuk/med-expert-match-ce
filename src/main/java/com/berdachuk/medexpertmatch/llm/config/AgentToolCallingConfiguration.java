package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.llm.automemory.AutoMemoryTools;
import com.berdachuk.medexpertmatch.llm.harness.ToolScopeEnforcingResolver;
import com.berdachuk.medexpertmatch.llm.tool.NormalizingToolCallbackResolver;
import com.berdachuk.medexpertmatch.llm.tools.CaseAnalysisAgentTools;
import com.berdachuk.medexpertmatch.llm.tools.ContextBuilderAgentTools;
import com.berdachuk.medexpertmatch.llm.tools.ClinicalAdvisorAgentTools;
import com.berdachuk.medexpertmatch.llm.tools.DoctorMatchingAgentTools;
import com.berdachuk.medexpertmatch.llm.tools.EvidenceAgentTools;
import com.berdachuk.medexpertmatch.llm.tools.GraphAnalyticsAgentTools;
import com.berdachuk.medexpertmatch.llm.tools.RoutingAgentTools;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hardens function-calling tool resolution for local models that emit non-canonical tool names.
 */
@Configuration
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
            AutoMemoryTools autoMemoryTools,
            TodoWriteTool todoWriteTool,
            AskUserQuestionTool askUserQuestionTool) {
        List<ToolCallback> merged = new ArrayList<>(toolCallbacks);
        toolCallbackProviders.orderedStream()
                .forEach(provider -> merged.addAll(Arrays.asList(provider.getToolCallbacks())));
        merged.addAll(Arrays.asList(MethodToolCallbackProvider.builder()
                .toolObjects(
                        caseAnalysisAgentTools,
                        doctorMatchingAgentTools,
                        evidenceAgentTools,
                        clinicalAdvisorAgentTools,
                        graphAnalyticsAgentTools,
                        routingAgentTools,
                        contextBuilderAgentTools,
                        autoMemoryTools,
                        todoWriteTool,
                        askUserQuestionTool)
                .build()
                .getToolCallbacks()));

        Map<String, ToolCallback> uniqueByName = new LinkedHashMap<>();
        for (ToolCallback callback : merged) {
            uniqueByName.put(callback.getToolDefinition().name(), callback);
        }
        List<ToolCallback> uniqueCallbacks = List.copyOf(uniqueByName.values());
        ToolCallbackResolver delegate = new StaticToolCallbackResolver(uniqueCallbacks);
        ToolCallbackResolver normalizing = new NormalizingToolCallbackResolver(delegate, uniqueCallbacks);
        return new ToolScopeEnforcingResolver(normalizing);
    }
}
