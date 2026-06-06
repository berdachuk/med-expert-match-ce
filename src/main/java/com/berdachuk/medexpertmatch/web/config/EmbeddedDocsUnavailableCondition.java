package com.berdachuk.medexpertmatch.web.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Active when MkDocs static site is not embedded in the classpath (typical local {@code mvn spring-boot:run}).
 */
public class EmbeddedDocsUnavailableCondition implements Condition {

    private static final String EMBEDDED_DOCS_INDEX = "classpath:static/docs/index.html";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return !context.getResourceLoader().getResource(EMBEDDED_DOCS_INDEX).exists();
    }
}
