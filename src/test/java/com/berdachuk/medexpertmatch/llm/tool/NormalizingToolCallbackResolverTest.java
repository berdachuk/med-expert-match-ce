package com.berdachuk.medexpertmatch.llm.tool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NormalizingToolCallbackResolverTest {

    @Test
    @DisplayName("resolve maps PascalCase tool names via snake_case alias")
    void resolvesPascalCaseAlias() {
        ToolCallback callback = mock(ToolCallback.class);
        ToolCallbackResolver delegate = name -> "match_doctors_to_case".equals(name) ? callback : null;

        NormalizingToolCallbackResolver resolver =
                new NormalizingToolCallbackResolver(delegate, List.of());

        assertNotNull(resolver.resolve("Match_doctors_to_case"));
    }

    @Test
    @DisplayName("resolve maps tool descriptions to registered callbacks")
    void resolvesDescriptionAlias() {
        ToolCallback callback = mock(ToolCallback.class);
        ToolDefinition definition = mock(ToolDefinition.class);
        when(callback.getToolDefinition()).thenReturn(definition);
        when(definition.description()).thenReturn(
                "Search clinical practice guidelines for a medical condition. Returns relevant guidelines with citations.");
        when(definition.name()).thenReturn("search_clinical_guidelines");

        NormalizingToolCallbackResolver resolver =
                new NormalizingToolCallbackResolver(name -> null, List.of(callback));

        assertNotNull(resolver.resolve(
                "Search clinical practice guidelines for a medical condition. Returns relevant guidelines with citations."));
    }

    @Test
    @DisplayName("resolve returns null when no alias matches")
    void returnsNullWhenUnknown() {
        NormalizingToolCallbackResolver resolver =
                new NormalizingToolCallbackResolver(name -> null, List.of());
        assertNull(resolver.resolve("totally_unknown_tool"));
    }
}
