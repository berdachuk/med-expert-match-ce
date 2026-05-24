package com.berdachuk.medexpertmatch.integration.usecase;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UseCase5DecisionSupportE2EIT extends BaseIntegrationTest {

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
                "Dyspnea on exertion, fatigue", "Heart failure",
                List.of("I50.9"), List.of(),
                UrgencyLevel.MEDIUM, "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Cardiology evaluation needed",
                "A 52-year-old female with progressive dyspnea and fatigue");
        medicalCaseRepository.insert(testCase);
    }

    @Test
    void shouldAnalyzeCaseEndToEnd() throws Exception {
        mockMvc.perform(post("/analyze/{caseId}", testCaseId))
                .andExpect(status().isOk())
                .andExpect(view().name("analyze"))
                .andExpect(model().attributeExists("currentPage"));
    }
}
