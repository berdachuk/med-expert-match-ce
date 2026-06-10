package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.graph.service.MedicalGraphBuilderService;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataCatalogState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the M73 specialty-graph reconciliation helper.
 *
 * <p>Pre-M73, the {@code buildGraph()} path created
 * {@code (d:Doctor)-[:SPECIALIZES_IN]->(s:MedicalSpecialty)} edges
 * by iterating the SQL doctor table. The pre-existing data set
 * (Kory Terry) shows that this path can drop edges when the synthetic
 * generator and the graph build run in the wrong order, or when a
 * specialty is added to a doctor after the initial build. M73 adds
 * a {@code reconcileSpecialtyGraph()} helper that walks the doctor
 * table, diffs against the graph, and (idempotently) creates any
 * missing edges via {@code MERGE}. This test covers the diff and the
 * idempotency contract.
 */
class SyntheticDataPostProcessingReconcileTest {

    private DoctorRepository doctorRepository;
    private MedicalGraphBuilderService graphBuilderService;
    private SyntheticDataPostProcessingServiceImpl service;

    @BeforeEach
    void setUp() {
        doctorRepository = mock(DoctorRepository.class);
        graphBuilderService = mock(MedicalGraphBuilderService.class);
        SyntheticDataCatalogState catalogState = mock(SyntheticDataCatalogState.class);
        service = new SyntheticDataPostProcessingServiceImpl(
                null,                       // ClinicalExperienceRepository — not used here
                null,                       // MedicalCaseRepository
                doctorRepository,
                null,                       // FacilityRepository
                null,                       // MedicalSpecialtyRepository
                null,                       // ICD10CodeRepository
                null,                       // ProcedureRepository
                graphBuilderService,
                null,                       // MedicalCaseDescriptionService
                null,                       // EmbeddingGeneratorService
                catalogState,               // SyntheticDataCatalogState — now required by reconcile
                null                        // SyntheticDataGenerationRunRepository — M77
        );
    }

    @Test
    @DisplayName("M73: reconcileSpecialtyGraph processes every (doctor, specialty) pair once")
    void reconcileInvokesBuilderForEveryPair() {
        Doctor d1 = new Doctor("d-1", "Dr. Alpha", "a@x",
                List.of("Cardiology", "Internal Medicine"), List.of(),
                List.of(), true, "AVAILABLE");
        Doctor d2 = new Doctor("d-2", "Dr. Beta", "b@x",
                List.of("Oncology"), List.of(), List.of(), true, "AVAILABLE");
        Doctor d3 = new Doctor("d-3", "Dr. Gamma", "g@x",
                List.of("Pediatrics", "Cardiology", "Neurology"), List.of(),
                List.of(), true, "AVAILABLE");
        when(doctorRepository.findAllIds(0)).thenReturn(List.of("d-1", "d-2", "d-3"));
        when(doctorRepository.findById("d-1")).thenReturn(Optional.of(d1));
        when(doctorRepository.findById("d-2")).thenReturn(Optional.of(d2));
        when(doctorRepository.findById("d-3")).thenReturn(Optional.of(d3));

        SyntheticDataPostProcessingServiceImpl.ReconcileReport report =
                service.reconcileSpecialtyGraph();

        assertNotNull(report);
        assertEquals(6, report.processed(),
                "Cardiology+IM + Oncology + Pediatrics+Cardiology+Neurology = 6 pairs");
        assertEquals(3, report.doctorsProcessed());
        assertEquals(3, report.doctors().size(),
                "3 distinct doctors should be reported");
        assertTrue(report.doctors().containsAll(List.of("d-1", "d-2", "d-3")));
        assertEquals(5, report.specialties().size(),
                "Cardiology, Internal Medicine, Oncology, Pediatrics, Neurology = 5 distinct");
        assertTrue(report.specialties().contains("Cardiology"));
        assertTrue(report.specialties().contains("Internal Medicine"));
        assertTrue(report.specialties().contains("Oncology"));
        assertTrue(report.specialties().contains("Pediatrics"));
        assertTrue(report.specialties().contains("Neurology"));
        // Each pair must reach the graph builder exactly once.
        verify(graphBuilderService, times(6)).createSpecializesInRelationship(anyString(), anyString());
    }

    @Test
    @DisplayName("M73: reconcileSpecialtyGraph skips doctors with null/empty specialties")
    void reconcileSkipsDoctorsWithNoSpecialties() {
        Doctor d1 = new Doctor("d-1", "Dr. Alpha", "a@x",
                null, List.of(), List.of(), true, "AVAILABLE");
        Doctor d2 = new Doctor("d-2", "Dr. Beta", "b@x",
                List.of(), List.of(), List.of(), true, "AVAILABLE");
        Doctor d3 = new Doctor("d-3", "Dr. Gamma", "g@x",
                List.of("Cardiology"), List.of(), List.of(), true, "AVAILABLE");
        when(doctorRepository.findAllIds(0)).thenReturn(List.of("d-1", "d-2", "d-3"));
        when(doctorRepository.findById("d-1")).thenReturn(Optional.of(d1));
        when(doctorRepository.findById("d-2")).thenReturn(Optional.of(d2));
        when(doctorRepository.findById("d-3")).thenReturn(Optional.of(d3));

        SyntheticDataPostProcessingServiceImpl.ReconcileReport report =
                service.reconcileSpecialtyGraph();

        assertEquals(1, report.processed());
        assertEquals(3, report.doctorsProcessed(),
                "all three doctors are scanned; only the empty-specialty ones are skipped at the pair level");
        verify(graphBuilderService, times(1)).createSpecializesInRelationship("d-3", "Cardiology");
    }

    @Test
    @DisplayName("M73: reconcileSpecialtyGraph is idempotent — running it twice produces the same effect")
    void reconcileIsIdempotent() {
        Doctor d1 = new Doctor("d-1", "Dr. Alpha", "a@x",
                List.of("Oncology"), List.of(), List.of(), true, "AVAILABLE");
        when(doctorRepository.findAllIds(0)).thenReturn(List.of("d-1"));
        when(doctorRepository.findById("d-1")).thenReturn(Optional.of(d1));

        SyntheticDataPostProcessingServiceImpl.ReconcileReport first =
                service.reconcileSpecialtyGraph();
        SyntheticDataPostProcessingServiceImpl.ReconcileReport second =
                service.reconcileSpecialtyGraph();

        // The first run reports 1 pair processed. The second run also
        // reports 1 pair processed (the helper re-issues the MERGE for
        // every doctor, and the graph builder's MERGE is idempotent).
        // The contract is: running it twice never throws and never
        // claims "nothing happened".
        assertEquals(1, first.processed());
        assertEquals(1, second.processed());
        verify(graphBuilderService, times(2)).createSpecializesInRelationship("d-1", "Oncology");
    }

    @Test
    @DisplayName("M73: reconcileSpecialtyGraph handles empty database")
    void reconcileHandlesEmptyDatabase() {
        when(doctorRepository.findAllIds(0)).thenReturn(List.of());

        SyntheticDataPostProcessingServiceImpl.ReconcileReport report =
                service.reconcileSpecialtyGraph();

        assertEquals(0, report.processed());
        assertEquals(0, report.doctorsProcessed());
        verify(graphBuilderService, never()).createSpecializesInRelationship(anyString(), anyString());
    }
}
