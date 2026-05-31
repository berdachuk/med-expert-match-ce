package com.berdachuk.medexpertmatch.llm.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves tool callbacks when function-calling models emit alternate names
 * (PascalCase, descriptions, etc.).
 */
@Slf4j
public class NormalizingToolCallbackResolver implements ToolCallbackResolver {

    private final ToolCallbackResolver delegate;
    private final Map<String, ToolCallback> descriptionIndex;

    public NormalizingToolCallbackResolver(
            ToolCallbackResolver delegate,
            List<ToolCallback> knownCallbacks) {
        this.delegate = delegate;
        this.descriptionIndex = buildDescriptionIndex(knownCallbacks);
    }

    @Override
    public ToolCallback resolve(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return delegate.resolve(toolName);
        }

        ToolCallback direct = delegate.resolve(toolName);
        if (direct != null) {
            return direct;
        }

        String snakeCase = AgentToolNameNormalizer.toSnakeCase(toolName);
        if (!snakeCase.equals(toolName)) {
            ToolCallback normalized = delegate.resolve(snakeCase);
            if (normalized != null) {
                log.debug("Resolved tool '{}' via snake_case alias '{}'", toolName, snakeCase);
                return normalized;
            }
        }

        String lower = toolName.trim().toLowerCase(Locale.ROOT);
        ToolCallback lowerMatch = delegate.resolve(lower);
        if (lowerMatch != null) {
            log.debug("Resolved tool '{}' via lowercase alias", toolName);
            return lowerMatch;
        }

        if (AgentToolNameNormalizer.looksLikeDescription(toolName)) {
            ToolCallback byDescription = resolveByDescription(lower);
            if (byDescription != null) {
                log.warn("Resolved tool description alias to '{}'", byDescription.getToolDefinition().name());
                return byDescription;
            }
        }

        return null;
    }

    private ToolCallback resolveByDescription(String lowerDescription) {
        ToolCallback exact = descriptionIndex.get(lowerDescription);
        if (exact != null) {
            return exact;
        }
        String prefix = lowerDescription.length() > 80 ? lowerDescription.substring(0, 80) : lowerDescription;
        for (Map.Entry<String, ToolCallback> entry : descriptionIndex.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(prefix) || prefix.startsWith(key.substring(0, Math.min(key.length(), prefix.length())))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Map<String, ToolCallback> buildDescriptionIndex(List<ToolCallback> callbacks) {
        Map<String, ToolCallback> index = new LinkedHashMap<>();
        if (callbacks == null) {
            return index;
        }
        for (ToolCallback callback : callbacks) {
            ToolDefinition definition = callback.getToolDefinition();
            if (definition == null || definition.description() == null || definition.description().isBlank()) {
                continue;
            }
            index.putIfAbsent(definition.description().trim().toLowerCase(Locale.ROOT), callback);
        }
        return index;
    }
}
