package com.berdachuk.medexpertmatch.ingestion.syntheticdata.service.impl;

import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataCatalogState;
import com.berdachuk.medexpertmatch.ingestion.service.impl.SyntheticDataPostProcessingServiceImpl;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.domain.SyntheticDataGenerationRun;
import com.berdachuk.medexpertmatch.ingestion.syntheticdata.repository.SyntheticDataGenerationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SyntheticDataPostProcessingServiceImplRunTrackingTest {

    private SyntheticDataGenerationRunRepository runRepository;
    private SyntheticDataPostProcessingServiceImpl service;

    @BeforeEach
    void setUp() {
        runRepository = mock(SyntheticDataGenerationRunRepository.class);
        service = new SyntheticDataPostProcessingServiceImpl(
                mock(com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository.class),
                mock(com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository.class),
                mock(com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository.class),
                mock(com.berdachuk.medexpertmatch.facility.repository.FacilityRepository.class),
                mock(com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository.class),
                mock(com.berdachuk.medexpertmatch.medicalcoding.repository.ICD10CodeRepository.class),
                mock(com.berdachuk.medexpertmatch.medicalcoding.repository.ProcedureRepository.class),
                mock(com.berdachuk.medexpertmatch.graph.service.MedicalGraphBuilderService.class),
                mock(com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService.class),
                mock(com.berdachuk.medexpertmatch.ingestion.service.EmbeddingGeneratorService.class),
                new SyntheticDataCatalogState(),
                runRepository
        );
    }

    @Test
    void startRunTracking_insertsRunRow() {
        service.startRunTracking("large", 500, 10000);

        ArgumentCaptor<SyntheticDataGenerationRun> captor = ArgumentCaptor.forClass(SyntheticDataGenerationRun.class);
        verify(runRepository).insert(captor.capture());
        SyntheticDataGenerationRun run = captor.getValue();
        assertEquals("large", run.size());
        assertEquals(500, run.doctorCount());
        assertEquals(10000, run.caseCount());
        assertNotNull(run.startTime());
        assertNull(run.endTime());
    }

    @Test
    void completeRunTracking_updatesWithTotalDuration() {
        service.startRunTracking("large", 500, 10000);
        service.completeRunTracking(null);

        ArgumentCaptor<SyntheticDataGenerationRun> captor = ArgumentCaptor.forClass(SyntheticDataGenerationRun.class);
        verify(runRepository).update(captor.capture());
        SyntheticDataGenerationRun run = captor.getValue();
        assertNotNull(run.totalDurationMs());
        assertTrue(run.totalDurationMs() >= 0);
        assertNotNull(run.endTime());
        assertNull(run.errorMessage());
    }

    @Test
    void completeRunTracking_withError_savesErrorMessage() {
        service.startRunTracking("large", 500, 10000);
        service.completeRunTracking("test error");

        ArgumentCaptor<SyntheticDataGenerationRun> captor = ArgumentCaptor.forClass(SyntheticDataGenerationRun.class);
        verify(runRepository).update(captor.capture());
        assertEquals("test error", captor.getValue().errorMessage());
    }
}