package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.llm.automemory.AutoMemoryTools;
import com.berdachuk.medexpertmatch.llm.tools.MedicalAgentTools;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.CompactionStrategy;
import org.springframework.ai.session.compaction.CompactionTrigger;
import org.springframework.ai.session.compaction.CompositeCompactionTrigger;
import org.springframework.ai.session.compaction.TokenCountTrigger;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.ai.session.compaction.TurnWindowCompactionStrategy;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;

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

    /**
     * System guidance that turns the AutoMemory tools into a usable long-term memory layer
     * (Option B: explicit tools + prompt, since this spring-ai-agent-utils version exposes no
     * {@code AutoMemoryToolsAdvisor}). It instructs the agent to curate durable cross-session
     * facts and — critically — to store ONLY non-PHI (preferences, routing policies, model config),
     * never patient data. A defense-in-depth complement to the {@code PhiGuard} hard reject.
     */
    public static final String MEMORY_SYSTEM_PROMPT = """
            You have a durable long-term memory across sessions via the AutoMemory tools \
            (automemory_append, automemory_read, automemory_index). Use automemory_read at the \
            start of relevant tasks to recall clinician preferences, routing policies, and model \
            configuration, and automemory_append to persist new durable, non-patient facts. \
            CRITICAL HIPAA RULE: never write PHI (patient names, SSN, MRN, DOB, contact details, \
            or any patient-identifying data) to memory. Store ONLY non-PHI operational knowledge.""";

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
     * Creates the {@link FileSystemTools} bean (Read) so skills can pull in reference files and
     * assets on demand.
     * <p>
     * NOTE: {@code ShellTools} from spring-ai-agent-utils is intentionally NOT registered. Under
     * HIPAA and the project's AGENTS.md constraints, the agent must not execute unsandboxed shell
     * scripts; exposing {@code bash}/{@code killShell} tools would create an arbitrary
     * code-execution surface and a PHI-exfiltration risk. File access is limited to the read-only
     * {@link FileSystemTools} surface.
     */
    @Bean
    public FileSystemTools fileSystemTools() {
        log.info("Creating FileSystemTools bean for reading skill references (ShellTools intentionally disabled per HIPAA)");
        return FileSystemTools.builder()
                .build();
    }

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
                                                 TokenCountEstimator tokenCountEstimator) {
        return TurnWindowCompactionStrategy.builder()
                .maxTurns(properties.maxWindowTurns())
                .tokenCountEstimator(tokenCountEstimator)
                .build();
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
     */
    @Bean
    SessionMemoryAdvisor sessionMemoryAdvisor(SessionService sessionService,
                                              CompactionTrigger sessionCompactionTrigger,
                                              CompactionStrategy sessionCompactionStrategy) {
        return SessionMemoryAdvisor.builder(sessionService)
                .compactionTrigger(sessionCompactionTrigger)
                .compactionStrategy(sessionCompactionStrategy)
                .build();
    }

    /**
     * Creates a ChatClient with Agent Skills and Medical Tools enabled.
     * This ChatClient includes SkillsTool for skill discovery, FileSystemTools
     * for reading reference materials, and MedicalAgentTools for Java @Tool methods.
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
            FileSystemTools fileSystemTools,
            MedicalAgentTools medicalAgentTools,
            AutoMemoryTools autoMemoryTools,
            SessionMemoryAdvisor sessionMemoryAdvisor,
            AgentMemoryProperties agentMemoryProperties
    ) {
        log.info("Creating medicalAgentChatClient with Agent Skills and Medical Tools enabled");
        log.info("Using toolCallingChatModel: {} for tool invocations", toolCallingChatModel.getClass().getSimpleName());
        agentMemoryProperties.ensureDirectoryExists();
        log.info("AutoMemory long-term memory directory: {}", agentMemoryProperties.dir());

        ChatClient.Builder builder = ChatClient.builder(toolCallingChatModel)
                .defaultSystem(MEMORY_SYSTEM_PROMPT)
                .defaultTools(fileSystemTools)
                .defaultTools(medicalAgentTools)
                .defaultTools(autoMemoryTools)
                .defaultAdvisors(sessionMemoryAdvisor, new SimpleLoggerAdvisor());

        // Only add SkillsTool if it's properly configured
        try {
            builder.defaultToolCallbacks(skillsTool);  // Agent Skills discovery (ToolCallback)
            log.info("SkillsTool registered successfully");
        } catch (Exception e) {
            log.warn("Failed to register SkillsTool, continuing without it: {}", e.getMessage());
        }

        return builder.build();
    }
}
