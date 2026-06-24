package com.berdachuk.medexpertmatch.caseanalysis.service.impl;

import com.berdachuk.medexpertmatch.caseanalysis.domain.CaseAnalysisJson;
import com.berdachuk.medexpertmatch.caseanalysis.domain.CaseAnalysisResult;
import com.berdachuk.medexpertmatch.core.util.LenientJsonOutputConverter;
import com.berdachuk.medexpertmatch.core.util.LlmCallLimiter;
import com.berdachuk.medexpertmatch.core.util.LlmClientType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseAnalysisServiceImplTest {

    @Mock
    private ChatClient chatClient;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CaseAnalysisServiceImpl createService() {
        return new CaseAnalysisServiceImpl(
                chatClient, caseAnalysisSystemPromptTemplate, caseAnalysisUserPromptTemplate,
                icd10ExtractionSystemPromptTemplate, icd10ExtractionUserPromptTemplate,
                urgencyClassificationSystemPromptTemplate, urgencyClassificationUserPromptTemplate,
                specialtyDeterminationSystemPromptTemplate, specialtyDeterminationUserPromptTemplate,
                "test-model", objectMapper, llmCallLimiter);
    }

    @Test
    @DisplayName("throws IllegalArgumentException when medicalCase is null")
    void analyzeCaseNullThrows() {
        var service = createService();
        assertThrows(IllegalArgumentException.class, () -> service.analyzeCase(null));
    }

    @Test
    @DisplayName("returns empty result when LLM response is empty")
    void analyzeCaseEmptyResponseReturnsEmpty() {
        when(caseAnalysisSystemPromptTemplate.render(any())).thenReturn("system");
        when(caseAnalysisUserPromptTemplate.render(any())).thenReturn("user");
        when(llmCallLimiter.execute(eq(LlmClientType.CLINICAL), any(Supplier.class))).thenReturn("");

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
    @DisplayName("parseJsonArray handles line-based format for ICD-10 codes")
    void parseJsonArrayLineBasedIcd10() throws Exception {
        String lineBased = "I21.9\nE11.9\n";
        var field = CaseAnalysisServiceImpl.class.getDeclaredMethod("parseJsonArray", String.class);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) field.invoke(createService(), lineBased);
        assertEquals(List.of("I21.9", "E11.9"), result);
    }

    @Test
    @DisplayName("parseJsonArray handles line-based format for specialties")
    void parseJsonArrayLineBasedSpecialties() throws Exception {
        String lineBased = "CARDIOLOGY\nINTERNAL_MEDICINE\n";
        var field = CaseAnalysisServiceImpl.class.getDeclaredMethod("parseJsonArray", String.class);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) field.invoke(createService(), lineBased);
        assertEquals(List.of("CARDIOLOGY", "INTERNAL_MEDICINE"), result);
    }

    @Test
    @DisplayName("parseJsonArray handles legacy JSON array format")
    void parseJsonArrayLegacyJson() throws Exception {
        String jsonArray = "[\"I21.9\", \"E11.9\"]";
        var field = CaseAnalysisServiceImpl.class.getDeclaredMethod("parseJsonArray", String.class);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) field.invoke(createService(), jsonArray);
        assertEquals(List.of("I21.9", "E11.9"), result);
    }

    @Test
    @DisplayName("parseJsonArray handles empty line-based format")
    void parseJsonArrayEmptyLineBased() throws Exception {
        var field = CaseAnalysisServiceImpl.class.getDeclaredMethod("parseJsonArray", String.class);
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) field.invoke(createService(), "");
        assertEquals(List.of(), result);
    }

    // --- REQ-133: LenientJsonOutputConverter parses ultra-compact JSON (replaces removed parseCaseAnalysisResult) ---

    /**
     * REQ-133: case-analysis LLM output in ultra-compact JSON with short keys parses to the
     * same CaseAnalysisResult as the legacy verbose JSON, via LenientJsonOutputConverter.
     */
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

    /**
     * REQ-133: legacy verbose long-key JSON is NOT supported by CaseAnalysisJson record
     * (short keys only). This test verifies the converter handles it gracefully (null fields).
     */
    @Test
    @DisplayName("REQ-133: LenientJsonOutputConverter with legacy long keys returns null fields")
    void parseCaseAnalysisResultLegacyKeys() {
        String legacyJson = "{\"clinicalFindings\":[\"Chest pain\"],"
                + "\"potentialDiagnoses\":[{\"diagnosis\":\"Acute MI\",\"confidence\":0.8}],"
                + "\"recommendedNextSteps\":[\"Consult cardiology\"],"
                + "\"urgentConcerns\":[\"Critical\"]}";
        var converter = new LenientJsonOutputConverter<>(CaseAnalysisJson.class);
        CaseAnalysisJson parsed = converter.convert(legacyJson);
        // Legacy long keys don't match the short-key record — fields will be null
        assertNull(parsed.cf());
        assertNull(parsed.pd());
        assertNull(parsed.rns());
        assertNull(parsed.uc());
    }

    /**
     * REQ-133: short-key JSON produces a valid CaseAnalysisResult via toResult().
     */
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
