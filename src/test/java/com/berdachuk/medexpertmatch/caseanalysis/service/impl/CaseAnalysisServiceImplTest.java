package com.berdachuk.medexpertmatch.caseanalysis.service.impl;

import com.berdachuk.medexpertmatch.caseanalysis.domain.CaseAnalysisJson;
import com.berdachuk.medexpertmatch.caseanalysis.domain.CaseAnalysisResult;
import com.berdachuk.medexpertmatch.core.config.LlmStructuredOutputProperties;
import com.berdachuk.medexpertmatch.core.monitoring.StructuredOutputValidationMetrics;
import com.berdachuk.medexpertmatch.core.util.LenientJsonOutputConverter;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseAnalysisServiceImplTest {

    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec callSpec;
    @Mock
    private PromptTemplate caseAnalysisSystemPromptTemplate;
    @Mock
    private PromptTemplate caseAnalysisUserPromptTemplate;
    @Mock
    private PromptTemplate icd10ExtractionSystemPromptTemplate;
    @Mock
    private PromptTemplate icd10ExtractionUserPromptTemplate;
    @Mock
    private PromptTemplate urgencyClassificationSystemPromptTemplate;
    @Mock
    private PromptTemplate urgencyClassificationUserPromptTemplate;
    @Mock
    private PromptTemplate specialtyDeterminationSystemPromptTemplate;
    @Mock
    private PromptTemplate specialtyDeterminationUserPromptTemplate;
    @Mock
    private LlmCallLimiter llmCallLimiter;

    private final LlmStructuredOutputProperties structuredOutputProperties =
            new LlmStructuredOutputProperties(false);
    private final MockEnvironment environment = new MockEnvironment();
    private final StructuredOutputValidationMetrics validationMetrics =
            new StructuredOutputValidationMetrics(new SimpleMeterRegistry());

    private CaseAnalysisServiceImpl createService() {
        return new CaseAnalysisServiceImpl(
                chatClient, caseAnalysisSystemPromptTemplate, caseAnalysisUserPromptTemplate,
                icd10ExtractionSystemPromptTemplate, icd10ExtractionUserPromptTemplate,
                urgencyClassificationSystemPromptTemplate, urgencyClassificationUserPromptTemplate,
                specialtyDeterminationSystemPromptTemplate, specialtyDeterminationUserPromptTemplate,
                "test-model", llmCallLimiter, structuredOutputProperties, environment, validationMetrics);
    }

    private void stubEntityPath() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(llmCallLimiter.execute(eq(LlmClientType.CLINICAL), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());
    }

    @Test
    @DisplayName("throws IllegalArgumentException when medicalCase is null")
    void analyzeCaseNullThrows() {
        var service = createService();
        assertThrows(IllegalArgumentException.class, () -> service.analyzeCase(null));
    }

    @Test
    @DisplayName("returns empty result when structured entity response is null")
    void analyzeCaseEmptyResponseReturnsEmpty() {
        when(caseAnalysisSystemPromptTemplate.render(any())).thenReturn("system");
        when(caseAnalysisUserPromptTemplate.render(any())).thenReturn("user");
        stubEntityPath();
        when(callSpec.entity(any(StructuredOutputConverter.class), any())).thenReturn(null);

        var service = createService();
        MedicalCase mc = new MedicalCase("case-1", 30, "Chest pain", null, null,
                List.of(), List.of(), null, "Cardiology", null, null, null, null, null);

        CaseAnalysisResult result = service.analyzeCase(mc);
        assertTrue(result.clinicalFindings().isEmpty());
    }

    @Test
    @DisplayName("extractICD10Codes throws when medicalCase is null")
    void extractICD10CodesNullThrows() {
        var service = createService();
        assertThrows(IllegalArgumentException.class, () -> service.extractICD10Codes(null));
    }

    @Test
    @DisplayName("classifyUrgency throws when medicalCase is null")
    void classifyUrgencyNullThrows() {
        var service = createService();
        assertThrows(IllegalArgumentException.class, () -> service.classifyUrgency(null));
    }

    @Test
    @DisplayName("determineRequiredSpecialty throws when medicalCase is null")
    void determineRequiredSpecialtyNullThrows() {
        var service = createService();
        assertThrows(IllegalArgumentException.class, () -> service.determineRequiredSpecialty(null));
    }

    @Test
    @DisplayName("REQ-133: LenientJsonOutputConverter parses ultra-compact short-key JSON")
    void parseCaseAnalysisResultShortKeys() {
        String shortJson = "{\"cf\":[\"Chest pain\"],"
                + "\"pd\":[{\"d\":\"Acute MI\",\"c\":0.8}],"
                + "\"rns\":[\"Consult cardiology\"],"
                + "\"uc\":[\"Critical\"]}";
        var converter = new LenientJsonOutputConverter<>(CaseAnalysisJson.class);
        CaseAnalysisJson parsed = converter.convert(shortJson);
        CaseAnalysisResult result = parsed.toResult();

        assertEquals(List.of("Chest pain"), result.clinicalFindings());
        assertEquals(1, result.potentialDiagnoses().size());
        assertEquals("Acute MI", result.potentialDiagnoses().get(0).diagnosis());
        assertEquals(0.8, result.potentialDiagnoses().get(0).confidence());
        assertEquals(List.of("Consult cardiology"), result.recommendedNextSteps());
        assertEquals(List.of("Critical"), result.urgentConcerns());
    }

    @Test
    @DisplayName("REQ-133: LenientJsonOutputConverter with legacy long keys returns null fields")
    void parseCaseAnalysisResultLegacyKeys() {
        String legacyJson = "{\"clinicalFindings\":[\"Chest pain\"],"
                + "\"potentialDiagnoses\":[{\"diagnosis\":\"Acute MI\",\"confidence\":0.8}],"
                + "\"recommendedNextSteps\":[\"Consult cardiology\"],"
                + "\"urgentConcerns\":[\"Critical\"]}";
        var converter = new LenientJsonOutputConverter<>(CaseAnalysisJson.class);
        CaseAnalysisJson parsed = converter.convert(legacyJson);
        assertNull(parsed.cf());
        assertNull(parsed.pd());
        assertNull(parsed.rns());
        assertNull(parsed.uc());
    }

    @Test
    @DisplayName("REQ-133: short-key JSON produces valid CaseAnalysisResult via toResult()")
    void parseCaseAnalysisResultShortAndLegacyParity() {
        String shortJson = "{\"cf\":[\"F1\"],\"pd\":[{\"d\":\"D1\",\"c\":0.5}],\"rns\":[\"S1\"],\"uc\":[\"U1\"]}";
        var converter = new LenientJsonOutputConverter<>(CaseAnalysisJson.class);
        CaseAnalysisResult result = converter.convert(shortJson).toResult();

        assertEquals(List.of("F1"), result.clinicalFindings());
        assertEquals(1, result.potentialDiagnoses().size());
        assertEquals("D1", result.potentialDiagnoses().get(0).diagnosis());
        assertEquals(0.5, result.potentialDiagnoses().get(0).confidence());
        assertEquals(List.of("S1"), result.recommendedNextSteps());
        assertEquals(List.of("U1"), result.urgentConcerns());
    }
}
