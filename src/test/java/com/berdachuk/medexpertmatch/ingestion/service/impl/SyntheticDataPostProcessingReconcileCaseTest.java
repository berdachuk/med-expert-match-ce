package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import com.berdachuk.medexpertmatch.graph.service.MedicalGraphBuilderService;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataCatalogState;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataPostProcessingService;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the M75 case-side reconciliation helper.
 *
 * <p>M75 mirrors M73's {@code reconcileSpecialtyGraph()} for the case
 * side: every {@code medical_cases} row with a non-blank
 * {@code required_specialty} must have a matching
 * {@code (c:MedicalCase)-[:REQUIRES_SPECIALTY]->(s:MedicalSpecialty)}
 * edge in the graph. The M73 helper only touched the doctor side; a
 * case created after the initial build (e.g. via the chat intake
 * harness) had no path to refresh its graph edge, leaving 5,439 SQL
 * cases with only 600 graph edges in production (2026-06-09 audit).
 */
class SyntheticDataPostProcessingReconcileCaseTest {

    private MedicalCaseRepository medicalCaseRepository;
    private MedicalGraphBuilderService graphBuilderService;
    private SyntheticDataPostProcessingServiceImpl service;

    @BeforeEach
    void setUp() {
        medicalCaseRepository = mock(MedicalCaseRepository.class);
        graphBuilderService = mock(MedicalGraphBuilderService.class);
        MedicalSpecialtyRepository medicalSpecialtyRepository = mock(MedicalSpecialtyRepository.class);
        SyntheticDataCatalogState catalogState = mock(SyntheticDataCatalogState.class);
        service = new SyntheticDataPostProcessingServiceImpl(
                null,                       // ClinicalExperienceRepository — not used here
                medicalCaseRepository,
                null,                       // DoctorRepository
                null,                       // FacilityRepository
                medicalSpecialtyRepository, // M75: not used here, but constructor requires it
                null,                       // ICD10CodeRepository
                null,                       // ProcedureRepository
                graphBuilderService,
                null,                       // MedicalCaseDescriptionService
                null,                       // EmbeddingGeneratorService
                catalogState,               // SyntheticDataCatalogState — M75 populates caseSpecialtyCoverage
                null                        // SyntheticDataGenerationRunRepository — M77
        );
    }

    private MedicalCase caseWithSpecialty(String id, String requiredSpecialty) {
        return new MedicalCase(
                id,
                45,
                "Test complaint",
                "Test symptoms",
                null,
                List.of("I20.9"),
                List.of(),
                UrgencyLevel.MEDIUM,
                requiredSpecialty,
                CaseType.INPATIENT,
                null,
                null,
                new BigDecimal("0"),
                new BigDecimal("0"));
    }

    private MedicalCase caseWithoutSpecialty(String id) {
        return caseWithSpecialty(id, null);
    }

    @Test
    @DisplayName("M75: reconcileCaseSpecialtyGraph processes every (case, specialty) pair once")
    void reconcileCaseInvokesBuilderForEveryPair() {
        MedicalCase c1 = caseWithSpecialty("c-1", "Cardiology");
        MedicalCase c2 = caseWithSpecialty("c-2", "Oncology");
        MedicalCase c3 = caseWithSpecialty("c-3", "Cardiology");
        when(medicalCaseRepository.findAllIds(0))
                .thenReturn(List.of("c-1", "c-2", "c-3"));
        when(medicalCaseRepository.findById("c-1")).thenReturn(Optional.of(c1));
        when(medicalCaseRepository.findById("c-2")).thenReturn(Optional.of(c2));
        when(medicalCaseRepository.findById("c-3")).thenReturn(Optional.of(c3));

        SyntheticDataPostProcessingService.ReconcileCaseReport report =
                service.reconcileCaseSpecialtyGraph();

        assertNotNull(report);
        assertEquals(3, report.processed(),
                "3 cases each with one required_specialty = 3 pairs");
        assertEquals(3, report.casesProcessed());
        assertEquals(3, report.cases().size());
        assertTrue(report.cases().containsAll(List.of("c-1", "c-2", "c-3")));
        assertEquals(2, report.specialties().size(),
                "Cardiology and Oncology are the only two distinct specialty names");
        assertTrue(report.specialties().contains("Cardiology"));
        assertTrue(report.specialties().contains("Oncology"));
        verify(graphBuilderService, times(2))
                .createRequiresSpecialtyRelationship(anyString(), org.mockito.ArgumentMatchers.eq("Cardiology"));
        verify(graphBuilderService, times(1))
                .createRequiresSpecialtyRelationship("c-2", "Oncology");
    }

    @Test
    @DisplayName("M75: reconcileCaseSpecialtyGraph skips cases with null/blank required_specialty")
    void reconcileCaseSkipsCasesWithoutSpecialty() {
        MedicalCase c1 = caseWithoutSpecialty("c-1");
        MedicalCase c2 = caseWithSpecialty("c-1", "");
        MedicalCase c3 = caseWithSpecialty("c-1", "   ");
        MedicalCase c4 = caseWithSpecialty("c-2", "Pediatrics");
        when(medicalCaseRepository.findAllIds(0))
                .thenReturn(List.of("c-1", "c-1", "c-1", "c-2"));
        when(medicalCaseRepository.findById("c-1")).thenReturn(Optional.of(c1), Optional.of(c2), Optional.of(c3));
        when(medicalCaseRepository.findById("c-2")).thenReturn(Optional.of(c4));

        SyntheticDataPostProcessingService.ReconcileCaseReport report =
                service.reconcileCaseSpecialtyGraph();

        assertEquals(1, report.processed(),
                "Only the Pediatrics case should reach the graph builder");
        assertEquals(4, report.casesProcessed(),
                "All four scans are counted; only those with a non-blank specialty are processed at the pair level");
        verify(graphBuilderService, times(1))
                .createRequiresSpecialtyRelationship("c-2", "Pediatrics");
    }

    @Test
    @DisplayName("M75: reconcileCaseSpecialtyGraph is idempotent — running twice is safe")
    void reconcileCaseIsIdempotent() {
        MedicalCase c1 = caseWithSpecialty("c-1", "Cardiology");
        when(medicalCaseRepository.findAllIds(0)).thenReturn(List.of("c-1"));
        when(medicalCaseRepository.findById("c-1")).thenReturn(Optional.of(c1));

        SyntheticDataPostProcessingService.ReconcileCaseReport first =
                service.reconcileCaseSpecialtyGraph();
        SyntheticDataPostProcessingService.ReconcileCaseReport second =
                service.reconcileCaseSpecialtyGraph();

        assertEquals(1, first.processed());
        assertEquals(1, second.processed(),
                "Re-running the helper should still report 1 pair (the MERGE on the graph side is idempotent)");
        verify(graphBuilderService, times(2))
                .createRequiresSpecialtyRelationship("c-1", "Cardiology");
    }

    @Test
    @DisplayName("M75: reconcileCaseSpecialtyGraph handles an empty case database")
    void reconcileCaseHandlesEmptyDatabase() {
        when(medicalCaseRepository.findAllIds(0)).thenReturn(List.of());

        SyntheticDataPostProcessingService.ReconcileCaseReport report =
                service.reconcileCaseSpecialtyGraph();

        assertEquals(0, report.processed());
        assertEquals(0, report.casesProcessed());
        verify(graphBuilderService, never())
                .createRequiresSpecialtyRelationship(anyString(), anyString());
    }

    @Test
    @DisplayName("M75: reconcileCaseSpecialtyGraph swallows graph errors and continues")
    void reconcileCaseSwallowsGraphErrors() {
        MedicalCase c1 = caseWithSpecialty("c-1", "Cardiology");
        MedicalCase c2 = caseWithSpecialty("c-2", "Oncology");
        when(medicalCaseRepository.findAllIds(0)).thenReturn(List.of("c-1", "c-2"));
        when(medicalCaseRepository.findById("c-1")).thenReturn(Optional.of(c1));
        when(medicalCaseRepository.findById("c-2")).thenReturn(Optional.of(c2));
        org.mockito.Mockito.doThrow(new RuntimeException("graph timeout"))
                .when(graphBuilderService).createRequiresSpecialtyRelationship("c-1", "Cardiology");

        SyntheticDataPostProcessingService.ReconcileCaseReport report =
                service.reconcileCaseSpecialtyGraph();

        // The first call raised and was caught, the second still ran.
        assertEquals(1, report.processed(),
                "Only the Oncology case should be reported as processed; the failed one is logged at WARN");
        verify(graphBuilderService, times(1))
                .createRequiresSpecialtyRelationship("c-1", "Cardiology");
        verify(graphBuilderService, times(1))
                .createRequiresSpecialtyRelationship("c-2", "Oncology");
    }
}
