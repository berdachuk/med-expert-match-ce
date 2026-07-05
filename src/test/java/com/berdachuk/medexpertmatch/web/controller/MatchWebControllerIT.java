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
 * REQ-125: integration coverage for registered requirement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class MatchWebControllerIT extends BaseIntegrationTest {

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
                testCaseId, 45, "Chest pain",
                "Severe chest pain radiating to left arm",
                "Acute myocardial infarction",
                List.of("I21.9"), List.of(),
                UrgencyLevel.HIGH, "Cardiology",
                CaseType.INPATIENT,
                "Patient requires immediate attention",
                "A 45-year-old male presents with acute chest pain");
        medicalCaseRepository.insert(testCase);
    }

    @Test
    void shouldLoadMatchPage() throws Exception {
        mockMvc.perform(get("/match"))
                .andExpect(status().isOk())
                .andExpect(view().name("match"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attribute("currentPage", "match"))
                .andExpect(model().attributeExists("cases"));
    }

    @Test
    void shouldLoadMatchPageWithCaseId() throws Exception {
        mockMvc.perform(get("/match").param("caseId", testCaseId))
                .andExpect(status().isOk())
                .andExpect(view().name("match"))
                .andExpect(model().attribute("caseId", testCaseId))
                .andExpect(model().attributeExists("case"))
                .andExpect(model().attributeExists("displayAbstract"));
    }

    @Test
    void shouldMatchDoctorsForCase() throws Exception {
        mockMvc.perform(post("/match/{caseId}", testCaseId))
                .andExpect(status().isOk())
                .andExpect(view().name("match"))
                .andExpect(model().attribute("caseId", testCaseId))
                .andExpect(model().attributeExists("matchResult"))
                .andExpect(model().attributeDoesNotExist("matchExplainability"))
                .andExpect(model().attributeDoesNotExist("error"));
    }

    @Test
    void shouldHandleNonexistentCaseId() throws Exception {
        mockMvc.perform(post("/match/{caseId}", "nonexistent-case-id-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("match"))
                .andExpect(model().attribute("caseId", "nonexistent-case-id-01"))
                .andExpect(model().attributeExists("matchResult"));
    }
}
