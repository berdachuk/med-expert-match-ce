package com.berdachuk.medexpertmatch.retrieval.repository;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.ConsultationMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test for ConsultationMatchRepository.
 */
class ConsultationMatchRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private ConsultationMatchRepository consultationMatchRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @BeforeEach
    void setUp() {
        consultationMatchRepository.deleteAll();
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();
    }

    @Test
    void testInsertBatchAndCount() {
        String doctorId1 = IdGenerator.generateDoctorId();
        String doctorId2 = IdGenerator.generateDoctorId();
        doctorRepository.insert(new Doctor(doctorId1, "Dr. One", "one@test.com", List.of("Cardiology"), List.of(), List.of(), true, "AVAILABLE"));
        doctorRepository.insert(new Doctor(doctorId2, "Dr. Two", "two@test.com", List.of("Cardiology"), List.of(), List.of(), true, "AVAILABLE"));
        String caseId = IdGenerator.generateId().toLowerCase();
        medicalCaseRepository.insert(new MedicalCase(caseId, 45, "Chest pain", "Pain", "MI", List.of("I21.9"), List.of(), UrgencyLevel.HIGH, "Cardiology", CaseType.CONSULT_REQUEST, "Notes", null));

        List<ConsultationMatch> matches = List.of(
                new ConsultationMatch(IdGenerator.generateId(), caseId, doctorId1, 85.5, "Rationale 1", 1, "PENDING"),
                new ConsultationMatch(IdGenerator.generateId(), caseId, doctorId2, 80.0, "Rationale 2", 2, "PENDING")
        );
        List<String> ids = consultationMatchRepository.insertBatch(matches);
        assertEquals(2, ids.size());
        assertEquals(2, consultationMatchRepository.count());
    }

    @Test
    void testDeleteByCaseId() {
        String doctorId = IdGenerator.generateDoctorId();
        doctorRepository.insert(new Doctor(doctorId, "Dr. One", "one@test.com", List.of("Cardiology"), List.of(), List.of(), true, "AVAILABLE"));
        String caseId = IdGenerator.generateId().toLowerCase();
        medicalCaseRepository.insert(new MedicalCase(caseId, 45, "Chest pain", "Pain", "MI", List.of("I21.9"), List.of(), UrgencyLevel.HIGH, "Cardiology", CaseType.CONSULT_REQUEST, "Notes", null));

        consultationMatchRepository.insertBatch(List.of(
                new ConsultationMatch(IdGenerator.generateId(), caseId, doctorId, 90.0, "R1", 1, "PENDING")
        ));
        assertEquals(1, consultationMatchRepository.count());
        consultationMatchRepository.deleteByCaseId(caseId);
        assertEquals(0, consultationMatchRepository.count());
    }

    @Test
    void testDeleteByCaseIdIdempotent() {
        consultationMatchRepository.deleteByCaseId("nonexistent");
        assertEquals(0, consultationMatchRepository.count());
    }

    @Test
    void testDeleteAll() {
        String doctorId = IdGenerator.generateDoctorId();
        doctorRepository.insert(new Doctor(doctorId, "Dr. One", "one@test.com", List.of("Cardiology"), List.of(), List.of(), true, "AVAILABLE"));
        String caseId = IdGenerator.generateId().toLowerCase();
        medicalCaseRepository.insert(new MedicalCase(caseId, 45, "Chest pain", "Pain", "MI", List.of("I21.9"), List.of(), UrgencyLevel.HIGH, "Cardiology", CaseType.CONSULT_REQUEST, "Notes", null));

        consultationMatchRepository.insertBatch(List.of(
                new ConsultationMatch(IdGenerator.generateId(), caseId, doctorId, 75.0, "R", 1, "PENDING")
        ));
        assertEquals(1, consultationMatchRepository.deleteAll());
        assertEquals(0, consultationMatchRepository.count());
    }
}
