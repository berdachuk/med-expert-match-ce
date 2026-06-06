package com.berdachuk.medexpertmatch.retrieval.service;

import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.DoctorMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOptions;
import com.berdachuk.medexpertmatch.retrieval.domain.ScoreResult;
import com.berdachuk.medexpertmatch.retrieval.repository.ConsultationMatchRepository;
import com.berdachuk.medexpertmatch.retrieval.service.impl.MatchingServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceExcludeDoctorsTest {

    private static final String CASE_ID = "6a23f05200155d711484cf69";

    @Mock
    private MedicalCaseRepository medicalCaseRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private SemanticGraphRetrievalService semanticGraphRetrievalService;
    @Mock
    private ConsultationMatchRepository consultationMatchRepository;
    @Mock
    private NamedParameterJdbcTemplate namedJdbcTemplate;
    @Mock
    private RerankingService rerankingService;

    @InjectMocks
    private MatchingServiceImpl matchingService;

    @Test
    @DisplayName("excluded doctor IDs are omitted from match results")
    void excludesPreviouslyMatchedDoctors() {
        MedicalCase medicalCase = new MedicalCase(
                CASE_ID, 30, "Chest pain", null, null, List.of(), List.of(),
                null, "Cardiology", null, null, null, null, null);
        Doctor excluded = new Doctor("doc-excluded", "Dr. Excluded", null, List.of("Cardiology"),
                List.of(), List.of(), false, null);
        Doctor alternate = new Doctor("doc-alternate", "Dr. Alternate", null, List.of("Cardiology"),
                List.of(), List.of(), false, null);

        when(medicalCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(medicalCase));
        when(doctorRepository.findAllIds(100)).thenReturn(List.of("doc-excluded", "doc-alternate"));
        when(doctorRepository.findByIds(List.of("doc-excluded", "doc-alternate")))
                .thenReturn(List.of(excluded, alternate));
        when(semanticGraphRetrievalService.score(eq(medicalCase), any(Doctor.class)))
                .thenReturn(new ScoreResult(80.0, 90.0, 70.0, 60.0, "fit"));
        when(rerankingService.rerank(eq(CASE_ID), any(), eq(10)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(consultationMatchRepository.findMaxRankByCaseId(CASE_ID)).thenReturn(1);

        MatchOptions options = MatchOptions.builder()
                .maxResults(10)
                .excludedDoctorIds(List.of("doc-excluded"))
                .build();

        List<DoctorMatch> matches = matchingService.matchDoctorsToCase(CASE_ID, options);

        assertEquals(1, matches.size());
        assertEquals("doc-alternate", matches.getFirst().doctor().id());
        verify(consultationMatchRepository, never()).deleteByCaseId(CASE_ID);
    }

    @Test
    @DisplayName("follow-up broadens candidate pool beyond required specialty")
    void broadensCandidatePoolWhenExcludingPriorMatches() {
        MedicalCase medicalCase = new MedicalCase(
                CASE_ID, 30, "Peripheral vascular disease", null, null, List.of(), List.of(),
                null, "Cardiology", null, null, null, null, null);
        Doctor excluded = new Doctor("doc-excluded", "Dr. Excluded", null, List.of("Cardiology"),
                List.of(), List.of(), false, null);
        Doctor surgeon = new Doctor("doc-surgery", "Dr. Surgery", null, List.of("Surgery"),
                List.of(), List.of(), false, null);

        when(medicalCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(medicalCase));
        when(doctorRepository.findAllIds(100)).thenReturn(List.of("doc-excluded", "doc-surgery"));
        when(doctorRepository.findByIds(List.of("doc-excluded", "doc-surgery")))
                .thenReturn(List.of(excluded, surgeon));
        when(semanticGraphRetrievalService.score(eq(medicalCase), any(Doctor.class)))
                .thenReturn(new ScoreResult(70.0, 80.0, 60.0, 50.0, "fit"));
        when(rerankingService.rerank(eq(CASE_ID), any(), eq(10)))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(consultationMatchRepository.findMaxRankByCaseId(CASE_ID)).thenReturn(1);

        MatchOptions options = MatchOptions.builder()
                .maxResults(10)
                .excludedDoctorIds(List.of("doc-excluded"))
                .build();

        List<DoctorMatch> matches = matchingService.matchDoctorsToCase(CASE_ID, options);

        assertEquals(1, matches.size());
        assertEquals("doc-surgery", matches.getFirst().doctor().id());
        verify(doctorRepository).findAllIds(100);
        verify(doctorRepository, never()).findBySpecialty(any(), anyInt());
    }

    @Test
    @DisplayName("first match without exclusions replaces persisted consultation matches")
    void replacesConsultationMatchesOnFirstMatch() {
        MedicalCase medicalCase = new MedicalCase(
                CASE_ID, 30, "Chest pain", null, null, List.of(), List.of(),
                null, "Cardiology", null, null, null, null, null);
        Doctor doctor = new Doctor("doc-1", "Dr. One", null, List.of("Cardiology"),
                List.of(), List.of(), false, null);

        when(medicalCaseRepository.findById(CASE_ID)).thenReturn(Optional.of(medicalCase));
        when(doctorRepository.findBySpecialty("Cardiology", 20)).thenReturn(List.of(doctor));
        when(semanticGraphRetrievalService.score(medicalCase, doctor))
                .thenReturn(new ScoreResult(80.0, 90.0, 70.0, 60.0, "fit"));
        when(rerankingService.rerank(eq(CASE_ID), any(), eq(10)))
                .thenAnswer(invocation -> invocation.getArgument(1));

        matchingService.matchDoctorsToCase(CASE_ID, MatchOptions.defaultOptions());

        verify(consultationMatchRepository).deleteByCaseId(CASE_ID);
        verify(consultationMatchRepository).insertBatch(any());
    }
}
