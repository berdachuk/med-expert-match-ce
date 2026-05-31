package com.berdachuk.medexpertmatch.llm.automemory;

import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "medexpertmatch.skills.enabled", havingValue = "true", matchIfMissing = true)
public class AutoMemoryTools {

    private final AutoMemoryService autoMemoryService;

    public AutoMemoryTools(AutoMemoryService autoMemoryService) {
        this.autoMemoryService = autoMemoryService;
    }

    @Tool(description = "Persist a fact, preference, or insight to durable cross-session memory. Types: user, feedback, project, reference. The markdownLine should be a single bullet-point line with key information.")
    public void automemory_append(
            @ToolParam(description = "Memory type: user, feedback, project, or reference") String type,
            @ToolParam(description = "Single markdown line summarizing the fact") String markdownLine
    ) {
        String sessionId = OrchestrationContextHolder.sessionIdOrNull();
        log.info("automemory_append() called - type: {}, sessionId: {}", type, sessionId);
        autoMemoryService.appendEntry(type, markdownLine);
    }

    @Tool(description = "Read durable memory entries by type. Use 'all' to read everything. Returns markdown text.")
    public String automemory_read(
            @ToolParam(description = "Memory type: user, feedback, project, reference, or 'all'") String type
    ) {
        log.info("automemory_read() called - type: {}", type);
        return autoMemoryService.readEntries(type);
    }

    @Tool(description = "View the MEMORY.md index of all recorded cross-session memories with timestamps and types.")
    public String automemory_index() {
        log.info("automemory_index() called");
        return autoMemoryService.readIndex();
    }

    @Tool(description = "Convenience method to append a user preference or fact to durable memory. Calls automemory_append with type='user'.")
    public void appendPreference(
            @ToolParam(description = "Single markdown line summarizing the user preference") String markdownLine
    ) {
        log.info("appendPreference() called");
        automemory_append("user", markdownLine);
    }

    @Tool(description = "Convenience method to read user preferences and facts from durable memory. Calls automemory_read with type='user'.")
    public String readPreferences() {
        log.info("readPreferences() called");
        return automemory_read("user");
    }
}
