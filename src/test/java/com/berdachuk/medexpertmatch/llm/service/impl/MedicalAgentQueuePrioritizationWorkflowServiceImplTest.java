package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentPromptSupportService;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import com.berdachuk.medexpertmatch.core.service.LogStreamService;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * REQ-132: MedGemma case-analysis output in ultra-compact JSON must still allow urgency and
 * specialty extraction — both via the Jackson Map path (valid JSON) and the regex fallback
 * (malformed JSON). SCN-132.
 */
class MedicalAgentQueuePrioritizationWorkflowServiceImplTest {

    private MedicalAgentQueuePrioritizationWorkflowServiceImpl createService() {
        return new MedicalAgentQueuePrioritizationWorkflowServiceImpl(
                mock(ChatClient.class),
                "functiongemma",
                mock(MedicalCaseRepository.class),
                mock(MedicalAgentLlmSupportService.class),
                mock(MedicalAgentPromptSupportService.class),
                mock(LogStreamService.class),
                mock(LlmCallLimiter.class),
                new ObjectMapper());
    }

    private Object invokeParse(String caseAnalysis) throws Exception {
        MedicalCase mc = new MedicalCase("case-1", 50, "Chest pain", "Symptoms",
                "Diagnosis", java.util.List.of(), java.util.List.of(), null, null, null, null, null);
        var method = MedicalAgentQueuePrioritizationWorkflowServiceImpl.class
                .getDeclaredMethod("parseUrgencyFromAnalysis", String.class, String.class, MedicalCase.class);
        method.setAccessible(true);
        return method.invoke(createService(), "case-1", caseAnalysis, mc);
    }

    @Test
    @DisplayName("REQ-132: short-key valid JSON extracts urgency and specialty via Map path")
    void shortKeyJsonMapPath() throws Exception {
        String shortJson = "{\"sp\":\"Cardiology\",\"u\":\"CRITICAL\",\"sm\":\"Acute MI\"}";
        Object entry = invokeParse(shortJson);
        var urgencyLevel = entry.getClass().getMethod("urgencyLevel").invoke(entry);
        var specialty = entry.getClass().getMethod("specialty").invoke(entry);
        assertEquals("CRITICAL", urgencyLevel);
        assertEquals("Cardiology", specialty);
    }

    @Test
    @DisplayName("REQ-132: legacy long-key JSON still extracts urgency and specialty (fallback)")
    void legacyKeyJsonMapPath() throws Exception {
        String legacyJson = "{\"requiredSpecialty\":\"Cardiology\",\"urgencyLevel\":\"CRITICAL\",\"caseSummary\":\"Acute MI\"}";
        Object entry = invokeParse(legacyJson);
        var urgencyLevel = entry.getClass().getMethod("urgencyLevel").invoke(entry);
        var specialty = entry.getClass().getMethod("specialty").invoke(entry);
        assertEquals("CRITICAL", urgencyLevel);
        assertEquals("Cardiology", specialty);
    }

    @Test
    @DisplayName("REQ-132: malformed short-key JSON falls back to regex extraction")
    void shortKeyMalformedRegexFallback() throws Exception {
        // Malformed JSON (missing closing brace) forces the catch → regex fallback path
        String malformed = "Analysis: {\"sp\":\"Cardiology\",\"u\":\"HIGH\"";
        Object entry = invokeParse(malformed);
        var urgencyLevel = entry.getClass().getMethod("urgencyLevel").invoke(entry);
        var specialty = entry.getClass().getMethod("specialty").invoke(entry);
        assertEquals("HIGH", urgencyLevel, "URGENCY_PATTERN must match short key u");
        assertEquals("Cardiology", specialty, "SPECIALTY_PATTERN must match short key sp");
    }
}