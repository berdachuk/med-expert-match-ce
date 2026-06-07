package com.berdachuk.medexpertmatch.retrieval.service.impl;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchSignalBreakdown;
import com.berdachuk.medexpertmatch.retrieval.domain.ScoreResult;
import com.berdachuk.medexpertmatch.retrieval.service.SemanticGraphRetrievalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchExplainabilityServiceImplTest {

    @Test
    @DisplayName("explainMatches returns non-PHI signal breakdown for top matches")
    void explainMatches() {
        MedicalCaseRepository caseRepository = mock(MedicalCaseRepository.class);
        SemanticGraphRetrievalService semanticGraphRetrievalService = mock(SemanticGraphRetrievalService.class);
        String caseId = "6a1c68963a08e800010de68e";
        MedicalCase medicalCase = new MedicalCase(
                caseId, 40, "Seizure", "Seizure", "Epilepsy", List.of("G40.9"),
                List.of(), UrgencyLevel.MEDIUM, "Neurology", CaseType.CONSULT_REQUEST, "Notes", null);
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(medicalCase));

        Doctor doctor = new Doctor("d1", "Dr. Lee", null, List.of("Neurology"), List.of(), List.of(), false, null);
        DoctorMatch match = new DoctorMatch(doctor, 72.0, 1, "fit");
        when(semanticGraphRetrievalService.score(any(), any())).thenReturn(
                new ScoreResult(72.0, 0.8, 0.6, 0.5, "rationale"));

        MatchExplainabilityServiceImpl service = new MatchExplainabilityServiceImpl(
                caseRepository, semanticGraphRetrievalService);
        List<MatchSignalBreakdown> breakdowns = service.explainMatches(caseId, List.of(match), 3);

        assertEquals(1, breakdowns.size());
        assertEquals("Dr. Lee", breakdowns.getFirst().doctorName());
        assertEquals(80, breakdowns.getFirst().vectorPercent());
        assertTrue(breakdowns.getFirst().toView().containsKey("graphPercent"));
    }
}
