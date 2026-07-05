package com.berdachuk.medexpertmatch.core.util;

import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;

import java.util.regex.Pattern;

public class LenientJsonOutputConverter<T> implements StructuredOutputConverter<T> {

    private static final Pattern FENCE = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```");

    private final BeanOutputConverter<T> delegate;

    public LenientJsonOutputConverter(Class<T> targetType) {
        this.delegate = new BeanOutputConverter<>(targetType);
    }

    @Override
    public String getFormat() {
        return delegate.getFormat();
    }

    @Override
    public String getJsonSchema() {
        return delegate.getJsonSchema();
    }

    @Override
    public T convert(String source) {
        String cleaned = cleanResponse(source);
        return delegate.convert(cleaned);
    }

    static String cleanResponse(String source) {
        if (source == null) {
            return null;
        }
        String result = source.trim();
        if (result.isEmpty()) {
            return "";
        }
        var matcher = FENCE.matcher(result);
        if (matcher.find()) {
            result = matcher.group(1).trim();
        }
        int lastBrace = result.lastIndexOf('}');
        int lastBracket = result.lastIndexOf(']');
        int lastClose = Math.max(lastBrace, lastBracket);
        if (lastClose > 0 && lastClose < result.length() - 1) {
            String after = result.substring(lastClose + 1).trim();
            if (!after.isEmpty() && !after.startsWith(",") && !after.startsWith("}")) {
                result = result.substring(0, lastClose + 1);
            }
        }
        return result;
    }
}
