package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.core.advisor.DateTimeContextAdvisor;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.automemory.AutoMemoryTools;
import com.berdachuk.medexpertmatch.llm.automemory.MemoryConsolidationTrigger;
import com.berdachuk.medexpertmatch.llm.automemory.TimeGapConsolidationTrigger;
import com.berdachuk.medexpertmatch.llm.domain.AgentThinking;
import com.berdachuk.medexpertmatch.llm.service.AgentQuestionService;
import com.berdachuk.medexpertmatch.llm.service.AgentTodoTrackingService;
import com.berdachuk.medexpertmatch.llm.tools.*;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.agent.common.task.subagent.SubagentReference;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springaicommunity.agent.tools.task.TaskTool;
import org.springaicommunity.agent.tools.task.claude.ClaudeSubagentType;
import org.springaicommunity.agent.tools.task.repository.DefaultTaskRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.*;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.augment.AugmentedToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.Collections;

/**
 * Configuration for Medical Agent with Spring AI Agent Skills integration.
 * <p>
 * Agent Skills provide modular knowledge management through Markdown-based skills
 * that complement existing Java @Tool methods. This configuration enables skills
 * discovery and loading on demand.
 * <p>
 * Skills are loaded from skills directory in classpath resources.
 * This configuration is only active when medexpertmatch.skills.enabled=true.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({AgentSessionProperties.class, AgentMemoryProperties.class, AgentSkillsProperties.class})
@ConditionalOnProperty(
        name = "medexpertmatch.skills.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MedicalAgentConfiguration {

    private final ResourceLoader resourceLoader;

    public MedicalAgentConfiguration(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Creates the {@link SkillsTool} {@link ToolCallback} for discovering and loading Agent Skills on
     * demand with progressive disclosure. Skills are loaded JAR-safely from the classpath
     * ({@code agent.skills.dir}, default {@code skills}); an optional filesystem
     * {@code agent.skills.extra-directory} (e.g. a mounted volume) is layered on top when present.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("skillsTool")
    public ToolCallback skillsTool(AgentSkillsProperties skillsProperties) {
        String skillsDirectory = skillsProperties.dir();
        String extraDirectory = skillsProperties.extraDirectory();
        log.info("Creating SkillsTool bean - directory: {}, extra-directory: {}",
                skillsDirectory, extraDirectory.isEmpty() ? "(none)" : extraDirectory);
        SkillsTool.Builder builder = SkillsTool.builder();
        boolean skillsAdded = false;

        // Main directory: load directly from the classpath (JAR-safe)
        try {
            org.springframework.core.io.Resource skillsResource =
                    resourceLoader.getResource("classpath:" + skillsDirectory);
            if (skillsResource.exists()) {
                builder.addSkillsResource(skillsResource);
                log.info("Added classpath skills: classpath:{}", skillsDirectory);
                skillsAdded = true;
            } else {
                log.warn("Classpath skills directory not found: classpath:{}", skillsDirectory);
            }
        } catch (Exception e) {
            log.warn("Failed to load classpath skills: {}", e.getMessage());
        }

        // Optional extra directory (e.g. Docker volume or mounted skills)
        if (!extraDirectory.isEmpty()) {
            try {
                java.io.File extraDir = new java.io.File(extraDirectory);
                if (extraDir.exists() && extraDir.isDirectory()) {
                    builder.addSkillsDirectory(extraDirectory);
                    log.info("Added extra skills directory: {}", extraDirectory);
                    skillsAdded = true;
                } else {
                    log.debug("Extra skills directory not found or not a directory: {}", extraDirectory);
                }
            } catch (Exception e) {
                log.warn("Failed to add extra skills directory: {}", e.getMessage());
            }
        }

        if (!skillsAdded) {
            throw new IllegalStateException("At least one skill must be configured. " +
                    "Ensure skills exist in " + skillsDirectory + " (filesystem or classpath) or set agent.skills.extra-directory.");
        }

        return builder.build();
    }

    /**
     * NOTE: {@link FileSystemTools} from spring-ai-agent-utils is intentionally NOT registered.
     * The Read tool would give the LLM unrestricted filesystem access — when asked for case
     * information the agent tries to read case IDs as file paths instead of using the proper
     * domain tools ({@code analyze_case}, {@code match_doctors_to_case}, etc.).
     * <p>
     * {@code ShellTools} is also excluded: under HIPAA and the project's AGENTS.md constraints,
     * the agent must not execute unsandboxed shell scripts; exposing {@code bash}/{@code killShell}
     * tools would create an arbitrary code-execution surface and a PHI-exfiltration risk.
     */

    /**
     * Non-LLM token estimator shared by the token-count trigger and the window strategy. Keeps
     * compaction cheap (no extra model call) and avoids sending PHI to any external tokenizer.
     */
    @Bean
    @ConditionalOnMissingBean(TokenCountEstimator.class)
    TokenCountEstimator sessionTokenCountEstimator() {
        return new JTokkitTokenCountEstimator();
    }

    /**
     * Turn-safe compaction trigger: fires when EITHER the turn count OR the estimated token
     * count crosses its configured threshold. Both legs are non-LLM, so the trigger itself
     * never makes an extra model call.
     */
    @Bean
    CompactionTrigger sessionCompactionTrigger(AgentSessionProperties properties,
                                               TokenCountEstimator tokenCountEstimator) {
        return CompositeCompactionTrigger.anyOf(
                new TurnCountTrigger(properties.maxTurns()),
                TokenCountTrigger.builder()
                        .threshold(properties.maxTokens())
                        .tokenCountEstimator(tokenCountEstimator)
                        .build());
    }

    /**
     * Non-LLM compaction strategy. {@link TurnWindowCompactionStrategy} keeps the most recent
     * turns and always starts the retained window on a USER message, so a partial turn is never
     * left dangling. Chosen over summarization strategies to avoid extra model calls and to keep
     * PHI out of any LLM-generated summary.
     */
    @Bean
    CompactionStrategy sessionCompactionStrategy(AgentSessionProperties properties,
                                                 TokenCountEstimator tokenCountEstimator,
                                                 SessionCompactionObservability observability) {
        CompactionStrategy delegate = TurnWindowCompactionStrategy.builder()
                .maxTurns(properties.maxWindowTurns())
                .tokenCountEstimator(tokenCountEstimator)
                .build();
        return new ObservingCompactionStrategy(delegate, observability);
    }

    @Bean
    MemoryConsolidationTrigger timeGapConsolidationTrigger(AgentMemoryProperties agentMemoryProperties) {
        return new TimeGapConsolidationTrigger(agentMemoryProperties);
    }

    /**
     * Explicit {@link SessionService} bean backed by the JDBC {@link SessionRepository}.
     * Declared with {@link ConditionalOnMissingBean} so it coexists with the session
     * auto-configuration; if a repository is unavailable the auto-config's bean (or none) is used.
     */
    @Bean
    @ConditionalOnMissingBean(SessionService.class)
    @ConditionalOnBean(SessionRepository.class)
    SessionService sessionService(SessionRepository sessionRepository) {
        log.info("Creating DefaultSessionService for turn-safe short-term memory");
        return DefaultSessionService.builder()
                .sessionRepository(sessionRepository)
                .build();
    }

    /**
     * Wires turn-safe short-term memory into the medical agent's advisor chain. Both a trigger
     * and a strategy MUST be supplied together — {@link SessionMemoryAdvisor.Builder#build()}
     * throws {@link IllegalArgumentException} otherwise (the turn-safety guard).
     * <p>
     * Order is set to {@link ToolCallingAdvisor#DEFAULT_ORDER} + 1 so the memory advisor sits
     * <b>inside</b> the tool loop, capturing the full tool request/response transcript in session
     * memory. {@link ToolCallingAdvisor} has {@code conversationHistoryEnabled(false)} to avoid
     * duplicate writes.
     */
    @Bean
    SessionMemoryAdvisor sessionMemoryAdvisor(SessionService sessionService,
                                              CompactionTrigger sessionCompactionTrigger,
                                              CompactionStrategy sessionCompactionStrategy) {
        return SessionMemoryAdvisor.builder(sessionService)
                .compactionTrigger(sessionCompactionTrigger)
                .compactionStrategy(sessionCompactionStrategy)
                .order(ToolCallingAdvisor.DEFAULT_ORDER + 1)
                .build();
    }

    @Bean
    TodoWriteTool todoWriteTool(AgentTodoTrackingService todoTrackingService) {
        log.info("Creating TodoWriteTool for multi-step plan tracking (Part 3 agentic pattern)");
        return TodoWriteTool.builder()
                .todoEventHandler(todoTrackingService::handleTodos)
                .build();
    }

    @Bean
    AskUserQuestionTool askUserQuestionTool(AgentQuestionService agentQuestionService) {
        log.info("Creating AskUserQuestionTool for interactive intake clarification (Part 2 agentic pattern)");
        return AskUserQuestionTool.builder()
                .questionHandler(questions -> {
                    String sessionId = OrchestrationContextHolder.sessionIdOrNull();
                    if (sessionId == null || sessionId.isBlank()) {
                        sessionId = "default";
                    }
                    return agentQuestionService.resolveQuestions(sessionId, questions);
                })
                .build();
    }

    /**
     * Task tool for Auto orchestrator subagent delegation (M08 Step 6 / M14).
     * Subagents get the same medical @Tool components so they can call
     * match_doctors_to_case, analyze_case, etc. directly.
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("taskTool")
    ToolCallback taskTool(
            @Qualifier("toolCallingChatModel") ChatModel toolCallingChatModel,
            ResourceLoader resourceLoader,
            CaseAnalysisAgentTools caseAnalysisAgentTools,
            DoctorMatchingAgentTools doctorMatchingAgentTools,
            EvidenceAgentTools evidenceAgentTools,
            ClinicalAdvisorAgentTools clinicalAdvisorAgentTools,
            GraphAnalyticsAgentTools graphAnalyticsAgentTools,
            RoutingAgentTools routingAgentTools,
            TodoWriteTool todoWriteTool,
            AskUserQuestionTool askUserQuestionTool,
            DateTimeAgentTools dateTimeAgentTools,
            DateTimeContextAdvisor dateTimeContextAdvisor) {
        log.info("Creating TaskTool with specialist subagents from classpath:agents");
        try {
            var subagentReferences = loadSubagentReferences(resourceLoader);
            ChatClient.Builder subagentChatClientBuilder = ChatClient.builder(toolCallingChatModel)
                    .defaultAdvisors(dateTimeContextAdvisor)
                    .defaultTools(augmentTool(caseAnalysisAgentTools))
                    .defaultTools(augmentTool(doctorMatchingAgentTools))
                    .defaultTools(augmentTool(evidenceAgentTools))
                    .defaultTools(augmentTool(clinicalAdvisorAgentTools))
                    .defaultTools(augmentTool(graphAnalyticsAgentTools))
                    .defaultTools(augmentTool(routingAgentTools))
                    .defaultTools(augmentTool(dateTimeAgentTools))
                    .defaultTools(augmentTool(todoWriteTool))
                    .defaultTools(augmentTool(askUserQuestionTool));
            return TaskTool.builder()
                    .subagentReferences(subagentReferences)
                    .subagentTypes(ClaudeSubagentType.builder()
                            .chatClientBuilder("default", subagentChatClientBuilder)
                            .build())
                    .taskRepository(new DefaultTaskRepository())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load TaskTool subagents from classpath:agents", e);
        }
    }

    private static java.util.List<SubagentReference> loadSubagentReferences(
            ResourceLoader resourceLoader) throws Exception {
        Resource[] agentResources = new PathMatchingResourcePatternResolver(resourceLoader)
                .getResources("classpath:agents/*.md");
        if (agentResources.length == 0) {
            throw new IllegalStateException("No subagent definitions found under classpath:agents/");
        }
        java.util.List<SubagentReference> references = new java.util.ArrayList<>();
        for (Resource resource : agentResources) {
            String fileName = resource.getFilename();
            if (fileName == null || fileName.isBlank()) {
                continue;
            }
            references.add(new SubagentReference("classpath:/agents/" + fileName, "CLAUDE"));
        }
        if (references.isEmpty()) {
            throw new IllegalStateException("No subagent definitions found under classpath:agents/");
        }
        return references;
    }

    /**
     * Tool-call advisor with conversation history disabled so todo updates from {@link TodoWriteTool}
     * persist via the session advisor without duplicate internal history (Part 3 guidance).
     * <p>
     * Internal conversation history is disabled because {@link SessionMemoryAdvisor} sits inside
     * the tool loop (order = DEFAULT_ORDER + 1) and captures the full tool transcript.
     */
    @Bean
    ToolCallingAdvisor agentToolCallAdvisor(ToolCallingManager toolCallingManager) {
        return ToolCallingAdvisor.builder()
                .toolCallingManager(toolCallingManager)
                .conversationHistoryEnabled(false)
                .build();
    }

    /**
     * Wraps a tool object with {@link AugmentedToolCallbackProvider} to add an {@code innerThought}
     * parameter to every tool's input schema. The model must articulate its reasoning before calling
     * each tool, improving traceability for medical compliance.
     */
    private static AugmentedToolCallbackProvider<AgentThinking> augmentTool(Object toolObject) {
        return AugmentedToolCallbackProvider.<AgentThinking>builder()
                .toolObject(toolObject)
                .argumentType(AgentThinking.class)
                .argumentConsumer(event -> log.debug("Tool: {} | Reasoning: {}",
                        event.toolDefinition().name(), event.arguments().innerThought()))
                .build();
    }

    /**
     * Creates a ChatClient with Agent Skills and Medical Tools enabled.
     * This ChatClient includes SkillsTool for skill discovery and agent tool
     * components for Java @Tool methods (no filesystem tools exposed).
     * <p>
     * Uses toolCallingChatModel (FunctionGemma) instead of primaryChatModel (LLM)
     * because FunctionGemma supports tool calling while the primary chat model often does not.
     * <p>
     * This bean is marked as @Primary when Agent Skills are enabled,
     * ensuring that MedicalAgentService uses the ChatClient with tools.
     */
    @Bean("medicalAgentChatClient")
    @Primary
    public ChatClient medicalAgentChatClient(
            @Qualifier("toolCallingChatModel") ChatModel toolCallingChatModel,
            @org.springframework.beans.factory.annotation.Qualifier("skillsTool") ToolCallback skillsTool,
            @org.springframework.beans.factory.annotation.Qualifier("taskTool") ToolCallback taskTool,
            CaseAnalysisAgentTools caseAnalysisAgentTools,
            DoctorMatchingAgentTools doctorMatchingAgentTools,
            EvidenceAgentTools evidenceAgentTools,
            ClinicalAdvisorAgentTools clinicalAdvisorAgentTools,
            GraphAnalyticsAgentTools graphAnalyticsAgentTools,
            RoutingAgentTools routingAgentTools,
            AutoMemoryTools autoMemoryTools,
            TodoWriteTool todoWriteTool,
            AskUserQuestionTool askUserQuestionTool,
            DateTimeAgentTools dateTimeAgentTools,
            ToolCallingAdvisor agentToolCallAdvisor,
            SessionMemoryAdvisor sessionMemoryAdvisor,
            DateTimeContextAdvisor dateTimeContextAdvisor,
            AgentMemoryProperties agentMemoryProperties,
            @Qualifier("autoMemorySystemPromptTemplate") PromptTemplate autoMemorySystemPromptTemplate
    ) {
        log.info("Creating medicalAgentChatClient with Agent Skills and Medical Tools enabled");
        log.info("Using toolCallingChatModel: {} for tool invocations", toolCallingChatModel.getClass().getSimpleName());
        agentMemoryProperties.ensureDirectoryExists();
        log.info("AutoMemory long-term memory directory: {}", agentMemoryProperties.dir());

        ChatClient.Builder builder = ChatClient.builder(toolCallingChatModel)
                .defaultSystem(autoMemorySystemPromptTemplate.render(Collections.emptyMap()))
                .defaultTools(augmentTool(caseAnalysisAgentTools))
                .defaultTools(augmentTool(doctorMatchingAgentTools))
                .defaultTools(augmentTool(evidenceAgentTools))
                .defaultTools(augmentTool(clinicalAdvisorAgentTools))
                .defaultTools(augmentTool(graphAnalyticsAgentTools))
                .defaultTools(augmentTool(routingAgentTools))
                .defaultTools(augmentTool(autoMemoryTools))
                .defaultTools(augmentTool(dateTimeAgentTools))
                .defaultTools(augmentTool(todoWriteTool))
                .defaultTools(augmentTool(askUserQuestionTool))
                .defaultAdvisors(dateTimeContextAdvisor, agentToolCallAdvisor, sessionMemoryAdvisor, new SimpleLoggerAdvisor());

        builder.defaultToolCallbacks(skillsTool, taskTool);
        log.info("SkillsTool and TaskTool registered successfully");

        return builder.build();
    }
}
