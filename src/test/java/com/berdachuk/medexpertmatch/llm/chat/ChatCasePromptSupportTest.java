package com.berdachuk.medexpertmatch.llm.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatCasePromptSupportTest {

    private final PromptTemplate caseIdHintTemplate = mock(PromptTemplate.class);
    private final PromptTemplate noCaseIdHintTemplate = mock(PromptTemplate.class);
    private final ChatCasePromptSupport support =
            new ChatCasePromptSupport(caseIdHintTemplate, noCaseIdHintTemplate);

    @Test
    @DisplayName("buildCaseToolHints guides text-based matching when no case ID")
    void textBasedHintWithoutCaseId() {
        when(noCaseIdHintTemplate.render(Collections.emptyMap()))
                .thenReturn("No medical case ID. Use match_doctors_from_text.");

        String hints = support.buildCaseToolHints(
                "90-year-old with chest pain and double vision, heart failure, need cardiologist");

        assertTrue(hints.contains("match_doctors_from_text"));
        assertTrue(hints.contains("No medical case ID"));
        verify(noCaseIdHintTemplate).render(Collections.emptyMap());
    }

    @Test
    @DisplayName("buildCaseToolHints injects case ID when present")
    void caseIdHintWhenPresent() {
        String caseId = "6a1c68963a08e800010de68e";
        when(caseIdHintTemplate.render(eq(Map.of("caseId", caseId))))
                .thenReturn("Case ID " + caseId + " for match_doctors_to_case");

        String hints = support.buildCaseToolHints("Case ID: " + caseId);

        assertTrue(hints.contains(caseId));
        assertTrue(hints.contains("match_doctors_to_case"));
        verify(caseIdHintTemplate).render(Map.of("caseId", caseId));
    }
}
