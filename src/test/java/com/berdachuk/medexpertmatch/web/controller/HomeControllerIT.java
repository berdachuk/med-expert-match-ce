package com.berdachuk.medexpertmatch.web.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for HomeController.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class HomeControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @BeforeEach
    void setUp() {
        doctorRepository.deleteAll();
        medicalCaseRepository.deleteAll();
    }

    @Test
    void testIndexPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("currentPage"))
                .andExpect(model().attribute("currentPage", "index"))
                .andExpect(model().attributeExists("doctorCount"))
                .andExpect(model().attributeExists("caseCount"))
                .andExpect(model().attributeExists("pendingConsultsCount"))
                .andExpect(model().attributeExists("matchesCount"));
    }

    @Test
    void testIndexPageWithData() throws Exception {
        // Create test doctors
        Doctor doctor1 = new Doctor(
                IdGenerator.generateDoctorId(),
                "Dr. John Smith",
                "john.smith@example.com",
                List.of("Cardiology"),
                List.of(),
                List.of(),
                false,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor1);

        Doctor doctor2 = new Doctor(
                IdGenerator.generateDoctorId(),
                "Dr. Jane Doe",
                "jane.doe@example.com",
                List.of("Oncology"),
                List.of(),
                List.of(),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor2);

        // Create test cases - mix of types
        MedicalCase consultCase1 = new MedicalCase(
                "696d4041ee50c1cfdb2e27ae",
                45,
                "Chest pain",
                "Acute chest pain",
                "Acute MI",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Needs cardiology consultation",
                null
        );
        medicalCaseRepository.insert(consultCase1);

        MedicalCase consultCase2 = new MedicalCase(
                "696d4041ee50c1cfdb2e28bf",
                60,
                "Shortness of breath",
                "Dyspnea on exertion",
                "Heart failure",
                List.of("I50.9"),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Cardiology",
                CaseType.CONSULT_REQUEST,
                "Cardiology consult needed",
                null
        );
        medicalCaseRepository.insert(consultCase2);

        MedicalCase inpatientCase = new MedicalCase(
                "696d4041ee50c1cfdb2e29cf",
                35,
                "Fever",
                "High fever, cough",
                "Pneumonia",
                List.of("J18.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Pulmonology",
                CaseType.INPATIENT,
                "Inpatient treatment",
                null
        );
        medicalCaseRepository.insert(inpatientCase);

        // Verify counts are correct
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("doctorCount", 2))
                .andExpect(model().attribute("caseCount", 3))
                .andExpect(model().attribute("pendingConsultsCount", 2)) // Only CONSULT_REQUEST cases
                .andExpect(model().attribute("matchesCount", 0)); // Matches not tracked
    }
}
