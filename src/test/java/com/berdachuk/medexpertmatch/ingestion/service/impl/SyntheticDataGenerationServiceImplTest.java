package com.berdachuk.medexpertmatch.ingestion.service.impl;

import com.berdachuk.medexpertmatch.ingestion.service.ClinicalExperienceGeneratorService;
import com.berdachuk.medexpertmatch.ingestion.service.DoctorGeneratorService;
import com.berdachuk.medexpertmatch.ingestion.service.FacilityGeneratorService;
import com.berdachuk.medexpertmatch.ingestion.service.MedicalCaseGeneratorService;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataCatalogState;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationProgress;
import com.berdachuk.medexpertmatch.ingestion.service.SyntheticDataGenerationService;
import com.berdachuk.medexpertmatch.doctor.repository.MedicalSpecialtyRepository;
import com.berdachuk.medexpertmatch.medicalcoding.domain.Procedure;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ICD10CodeRepository;
import com.berdachuk.medexpertmatch.medicalcoding.repository.ProcedureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyntheticDataGenerationServiceImplTest {

    @Mock
    private FacilityGeneratorService facilityGeneratorService;
    @Mock
    private DoctorGeneratorService doctorGeneratorService;
    @Mock
    private MedicalCaseGeneratorService medicalCaseGeneratorService;
    @Mock
    private ClinicalExperienceGeneratorService clinicalExperienceGeneratorService;
    @Mock
    private MedicalSpecialtyRepository medicalSpecialtyRepository;
    @Mock
    private ICD10CodeRepository icd10CodeRepository;
    @Mock
    private ProcedureRepository procedureRepository;

    private final SyntheticDataCatalogState catalogState = new SyntheticDataCatalogState();
    private SyntheticDataGenerationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SyntheticDataGenerationServiceImpl(
                facilityGeneratorService,
                doctorGeneratorService,
                medicalCaseGeneratorService,
                clinicalExperienceGeneratorService,
                medicalSpecialtyRepository,
                icd10CodeRepository,
                procedureRepository,
                catalogState);
    }

    @Test
    @DisplayName("generateAdditionalProcedures does not fail when loaded catalog exceeds tiny target")
    void loadsExistingProceduresWhenCatalogLargerThanTarget() {
        List<String> loaded = IntStream.range(0, 145)
                .mapToObj(i -> "Procedure " + i)
                .toList();
        catalogState.setLoadedProcedures(new ArrayList<>(loaded));
        catalogState.getDataSizeConfigs().put("tiny",
                new SyntheticDataGenerationService.DataSizeConfig("tiny", 3, 15, "tiny", 1));

        List<Procedure> existing = IntStream.range(0, 145)
                .mapToObj(i -> new Procedure("p" + i, "Procedure " + i, "procedure-" + i, null, "Diagnostic"))
                .toList();
        when(procedureRepository.findAll()).thenReturn(existing);

        assertDoesNotThrow(() -> service.generateAdditionalProcedures("tiny", new SyntheticDataGenerationProgress("test-job")));
        assertFalse(catalogState.getExtendedProcedures().isEmpty());
    }
}
