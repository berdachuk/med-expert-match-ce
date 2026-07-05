package com.berdachuk.medexpertmatch.retrieval.repository;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcome;
import com.berdachuk.medexpertmatch.retrieval.domain.MatchOutcomeLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * REQ-001: integration coverage for registered requirement.
 * REQ-008: integration coverage for registered requirement.
 * REQ-018: integration coverage for registered requirement.
 */
class MatchOutcomeRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private MatchOutcomeRepository matchOutcomeRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    private String caseId;
    private String doctorId;

    @BeforeEach
    void setUp() {
        matchOutcomeRepository.deleteAll();
        matchOutcomeRepository.deleteAllAffinities();
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();

        doctorId = IdGenerator.generateDoctorId();
        caseId = IdGenerator.generateId().toLowerCase();
        doctorRepository.insert(new Doctor(doctorId, "Dr. Outcome", "outcome@test.com",
                List.of("Neurology"), List.of(), List.of(), true, "AVAILABLE"));
        medicalCaseRepository.insert(new MedicalCase(caseId, 40, "Seizure", "Seizure", "Epilepsy",
                List.of("G40.9"), List.of(), UrgencyLevel.MEDIUM, "Neurology", CaseType.CONSULT_REQUEST, "Notes", null));
    }

    @Test
    void insertAndFindLatestForPair() {
        matchOutcomeRepository.insert(new MatchOutcome(
                IdGenerator.generateId(), caseId, doctorId, MatchOutcomeLabel.ACCEPTED, Instant.now()));

        var latest = matchOutcomeRepository.findLatestForPair(caseId, doctorId);
        assertTrue(latest.isPresent());
        assertEquals(MatchOutcomeLabel.ACCEPTED, latest.get().label());
        assertEquals(1, matchOutcomeRepository.count());
    }

    @Test
    void aggregateAndUpsertAffinity() {
        matchOutcomeRepository.insert(new MatchOutcome(
                IdGenerator.generateId(), caseId, doctorId, MatchOutcomeLabel.ACCEPTED, Instant.now()));
        matchOutcomeRepository.insert(new MatchOutcome(
                IdGenerator.generateId(), caseId, doctorId, MatchOutcomeLabel.REJECTED, Instant.now()));

        var aggregates = matchOutcomeRepository.aggregateByDoctor();
        assertEquals(1, aggregates.size());
        assertEquals(2, aggregates.getFirst().sampleCount());
        matchOutcomeRepository.upsertAffinity(aggregates.getFirst());

        var affinity = matchOutcomeRepository.findAffinityByDoctorId(doctorId);
        assertTrue(affinity.isPresent());
        assertEquals(2, affinity.get().sampleCount());
    }
}
