package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * REQ-007: integration coverage for registered requirement.
 * REQ-125: integration coverage for registered requirement.
 * SCN-001: executable scenario coverage.
 * SCN-009: executable scenario coverage.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CaseAnalysisControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    private String testCaseId;

    @BeforeEach
    void setUp() {
        medicalCaseRepository.deleteAll();
        testCaseId = IdGenerator.generateId();
        MedicalCase testCase = new MedicalCase(
                testCaseId, 52, "Shortness of breath",
                "Dyspnea on exertion, fatigue",
                "Heart failure",
                List.of("I50.9"), List.of(),
                UrgencyLevel.MEDIUM, "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Patient needs cardiology evaluation",
                "A 52-year-old female with progressive dyspnea");
        medicalCaseRepository.insert(testCase);
    }

    @Test
    void shouldLoadAnalyzeLandingPage() throws Exception {
        mockMvc.perform(get("/analyze"))
                .andExpect(status().isOk())
                .andExpect(view().name("analyze"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attribute("currentPage", "analyze"));
    }

    @Test
    void shouldLoadAnalyzePageWithCaseId() throws Exception {
        mockMvc.perform(get("/analyze/{caseId}", testCaseId))
                .andExpect(status().isOk())
                .andExpect(view().name("analyze"))
                .andExpect(model().attribute("caseId", testCaseId))
                .andExpect(model().attributeExists("case"))
                .andExpect(model().attributeExists("displayAbstract"));
    }

    @Test
    void shouldAnalyzeCase() throws Exception {
        mockMvc.perform(post("/analyze/{caseId}", testCaseId))
                .andExpect(status().isOk())
                .andExpect(view().name("analyze"))
                .andExpect(model().attribute("caseId", testCaseId))
                .andExpect(model().attributeExists("currentPage"));
    }

    @Test
    void shouldNotCrashOnAnalyzeNonexistentCase() throws Exception {
        mockMvc.perform(post("/analyze/{caseId}", "nonexistent-case"))
                .andExpect(status().isOk())
                .andExpect(view().name("analyze"));
    }
}
