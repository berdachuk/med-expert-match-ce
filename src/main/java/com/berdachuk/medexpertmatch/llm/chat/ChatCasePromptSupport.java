package com.berdachuk.medexpertmatch.llm.chat;

import com.berdachuk.medexpertmatch.core.util.CaseIdExtractor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * Builds chat prompt hints for case-ID vs free-text clinical requests from external templates.
 */
@Component
public class ChatCasePromptSupport {

    private final PromptTemplate caseIdHintTemplate;
    private final PromptTemplate noCaseIdHintTemplate;

    public ChatCasePromptSupport(
            @Qualifier("chatCaseIdHintPromptTemplate") PromptTemplate caseIdHintTemplate,
            @Qualifier("chatNoCaseIdHintPromptTemplate") PromptTemplate noCaseIdHintTemplate) {
        this.caseIdHintTemplate = caseIdHintTemplate;
        this.noCaseIdHintTemplate = noCaseIdHintTemplate;
    }

    public String buildCaseToolHints(String content) {
        return CaseIdExtractor.extractFromText(content)
                .map(caseId -> caseIdHintTemplate.render(Map.of("caseId", caseId)))
                .orElseGet(() -> noCaseIdHintTemplate.render(Collections.emptyMap()));
    }
}
