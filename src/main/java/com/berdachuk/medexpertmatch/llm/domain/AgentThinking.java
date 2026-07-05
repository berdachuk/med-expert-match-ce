package com.berdachuk.medexpertmatch.llm.domain;

import org.springframework.ai.tool.annotation.ToolParam;

public record AgentThinking(
        @ToolParam(description = "Your reasoning for calling this tool, including what you expect to learn and how it helps the user")
        String innerThought
) {}
