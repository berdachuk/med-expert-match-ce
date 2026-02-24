package com.berdachuk.medexpertmatch.llm.config;

import com.berdachuk.medexpertmatch.llm.tools.MedicalAgentTools;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
     * Creates SkillsTool bean for discovering and loading skills on demand.
     * Loads from classpath resources, then optional extra directory (e.g. mounted volume).
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("skillsTool")
    public ToolCallback skillsTool(
            @org.springframework.beans.factory.annotation.Value("${medexpertmatch.skills.directory:skills}") String skillsDirectory,
            @org.springframework.beans.factory.annotation.Value("${medexpertmatch.skills.extra-directory:}") String extraDirectory) {
        log.info("Creating SkillsTool bean - directory: {}, extra-directory: {}", skillsDirectory, extraDirectory.isEmpty() ? "(none)" : extraDirectory);
        SkillsTool.Builder builder = SkillsTool.builder();
        boolean skillsAdded = false;

        // Main directory: load directly from classpath
        try {
            org.springframework.core.io.Resource skillsResource = resourceLoader.getResource("classpath:" + skillsDirectory);
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
                    "Ensure skills exist in " + skillsDirectory + " (filesystem or classpath) or set medexpertmatch.skills.extra-directory.");
        }

        return builder.build();
    }

    /**
     * Creates FileSystemTools bean for reading reference files and assets.
     * Used by skills to load additional documentation and templates.
     */
    @Bean
    public FileSystemTools fileSystemTools() {
        log.info("Creating FileSystemTools bean for reading skill references");
        return FileSystemTools.builder()
                .build();
    }

    /**
     * Creates a ChatClient with Agent Skills and Medical Tools enabled.
     * This ChatClient includes SkillsTool for skill discovery, FileSystemTools
     * for reading reference materials, and MedicalAgentTools for Java @Tool methods.
     * <p>
     * Uses toolCallingChatModel (FunctionGemma) instead of primaryChatModel (MedGemma)
     * because FunctionGemma supports tool calling while MedGemma doesn't.
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
            MedicalAgentTools medicalAgentTools
    ) {
        log.info("Creating medicalAgentChatClient with Agent Skills and Medical Tools enabled");
        log.info("Using toolCallingChatModel: {} for tool invocations", toolCallingChatModel.getClass().getSimpleName());

        // Build ChatClient with tools
        // Note: SkillsTool is registered as ToolCallback, but it should only handle skill-based tool calls
        // Regular Java @Tool methods are handled by defaultTools registration
        ChatClient.Builder builder = ChatClient.builder(toolCallingChatModel)
                .defaultTools(fileSystemTools)  // File reading tools
                .defaultTools(medicalAgentTools)  // Java @Tool methods
                .defaultAdvisors(new SimpleLoggerAdvisor());

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
