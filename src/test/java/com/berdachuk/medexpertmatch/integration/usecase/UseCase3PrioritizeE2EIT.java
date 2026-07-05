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

/**
 * REQ-003: integration coverage for registered requirement.
 * REQ-011: integration coverage for registered requirement.
 * REQ-018: integration coverage for registered requirement.
 * SCN-007: executable scenario coverage.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UseCase3PrioritizeE2EIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @BeforeEach
    void setUp() {
        medicalCaseRepository.deleteAll();
        medicalCaseRepository.insert(new MedicalCase(IdGenerator.generateId(), 70,
                "Acute stroke", "Facial droop, Arm weakness", "Acute stroke",
                List.of("I63.9"), List.of(), UrgencyLevel.CRITICAL, "Neurology",
                CaseType.CONSULT_REQUEST, "Time-sensitive", null));
        medicalCaseRepository.insert(new MedicalCase(IdGenerator.generateId(), 65,
                "Chest pain", "Elevated troponin", "Acute MI",
                List.of("I21.9"), List.of(), UrgencyLevel.HIGH, "Cardiology",
                CaseType.CONSULT_REQUEST, "High priority", null));
        medicalCaseRepository.insert(new MedicalCase(IdGenerator.generateId(), 50,
                "Routine follow-up", "Stable", "Stable condition",
                List.of("Z00.00"), List.of(), UrgencyLevel.MEDIUM, "Internal Medicine",
                CaseType.CONSULT_REQUEST, "Routine", null));
        medicalCaseRepository.insert(new MedicalCase(IdGenerator.generateId(), 40,
                "Preventive care", "Annual checkup", "Preventive",
                List.of("Z00.00"), List.of(), UrgencyLevel.LOW, "Primary Care",
                CaseType.CONSULT_REQUEST, "Low priority", null));
    }

    @Test
    void shouldPrioritizeConsultsEndToEnd() throws Exception {
        mockMvc.perform(post("/queue/prioritize"))
                .andExpect(status().isOk())
                .andExpect(view().name("queue"))
                .andExpect(model().attributeExists("prioritizationResult"))
                .andExpect(model().attributeDoesNotExist("error"));
    }
}
