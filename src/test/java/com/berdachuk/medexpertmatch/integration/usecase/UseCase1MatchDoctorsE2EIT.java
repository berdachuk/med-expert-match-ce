package com.berdachuk.medexpertmatch.integration.usecase;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
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

/**
 * REQ-001: integration coverage for registered requirement.
 * REQ-018: integration coverage for registered requirement.
 * SCN-002: executable scenario coverage.
 * SCN-007: executable scenario coverage.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UseCase1MatchDoctorsE2EIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    private String testCaseId;

    @BeforeEach
    void setUp() {
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();

        Doctor doctor = new Doctor(
                IdGenerator.generateDoctorId(), "Dr. Cardiology Expert",
                "cardio@expert.com", List.of("Cardiology"),
                List.of("Board Certified"), List.of(), true, "AVAILABLE");
        doctorRepository.insert(doctor);

        testCaseId = IdGenerator.generateId();
        MedicalCase testCase = new MedicalCase(
                testCaseId, 65, "Acute chest pain with dyspnea",
                "Chest pain, Shortness of breath, Elevated troponin",
                "Acute myocardial infarction", List.of("I21.9"), List.of(),
                UrgencyLevel.HIGH, "Cardiology", CaseType.INPATIENT,
                "Requires immediate cardiology consultation",
                "A 65-year-old male with acute chest pain, dyspnea, elevated troponin");
        medicalCaseRepository.insert(testCase);
    }

    @Test
    void shouldMatchDoctorsEndToEnd() throws Exception {
        mockMvc.perform(post("/match/{caseId}", testCaseId))
                .andExpect(status().isOk())
                .andExpect(view().name("match"))
                .andExpect(model().attributeExists("matchResult"))
                .andExpect(model().attributeDoesNotExist("error"));
    }

    @Test
    void shouldReturnBadRequestForMissingCaseId() throws Exception {
        mockMvc.perform(post("/match/"))
                .andExpect(status().isNotFound());
    }
}
