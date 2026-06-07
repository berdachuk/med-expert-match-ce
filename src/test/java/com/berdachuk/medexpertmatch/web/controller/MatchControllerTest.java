package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.llm.rest.MedicalAgentController;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchControllerTest {

    private static final String CASE_ID = "case-test-001";

    @Mock
    private MedicalAgentController medicalAgentController;

    @Mock
    private MedicalCaseRepository medicalCaseRepository;

    private MatchController controller;

    @BeforeEach
    void setUp() {
        controller = new MatchController(medicalAgentController, medicalCaseRepository);
    }

    @Test
    void matchDoctorsForwardsMatchExplainabilityFromMetadata() {
        stubCaseAndCasesList();
        Map<String, Object> row = Map.of(
                "doctorId", "doc-1",
                "doctorName", "Dr. Alpha",
                "specialty", "Cardiology",
                "overallScore", 8.5,
                "rank", 1,
                "vectorPercent", 40,
                "graphPercent", 30,
                "historyPercent", 30);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("matchExplainability", List.of(row));
        when(medicalAgentController.matchDoctorsSync(eq(CASE_ID), any()))
                .thenReturn(ResponseEntity.ok(new MedicalAgentService.AgentResponse("Top matches found.", metadata)));

        Model model = new ConcurrentModel();
        String view = controller.matchDoctors(CASE_ID, null, model);

        assertEquals("match", view);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> explainability = (List<Map<String, Object>>) model.getAttribute("matchExplainability");
        assertNotNull(explainability);
        assertEquals(1, explainability.size());
        assertEquals("Dr. Alpha", explainability.get(0).get("doctorName"));
    }

    @Test
    void matchDoctorsOmitsExplainabilityWhenMetadataAbsent() {
        stubCaseAndCasesList();
        when(medicalAgentController.matchDoctorsSync(eq(CASE_ID), any()))
                .thenReturn(ResponseEntity.ok(new MedicalAgentService.AgentResponse("Results.", Map.of())));

        Model model = new ConcurrentModel();
        controller.matchDoctors(CASE_ID, null, model);

        assertNull(model.getAttribute("matchExplainability"));
    }

    @Test
    void matchDoctorsOmitsExplainabilityWhenListEmpty() {
        stubCaseAndCasesList();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("matchExplainability", List.of());
        when(medicalAgentController.matchDoctorsSync(eq(CASE_ID), any()))
                .thenReturn(ResponseEntity.ok(new MedicalAgentService.AgentResponse("Results.", metadata)));

        Model model = new ConcurrentModel();
        controller.matchDoctors(CASE_ID, null, model);

        assertFalse(model.containsAttribute("matchExplainability"));
    }

    private void stubCaseAndCasesList() {
        MedicalCase medicalCase = new MedicalCase(
                CASE_ID, 45, "Chest pain", "Symptoms", null,
                List.of(), List.of(), UrgencyLevel.HIGH, "Cardiology",
                CaseType.INPATIENT, null, null);
        when(medicalCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(medicalCase));
        when(medicalCaseRepository.findAllIds(100)).thenReturn(List.of(CASE_ID));
        when(medicalCaseRepository.findByIds(List.of(CASE_ID))).thenReturn(List.of(medicalCase));
    }
}
