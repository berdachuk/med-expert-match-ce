package com.berdachuk.medexpertmatch.web.controller;

import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.medicalcase.rest.MedicalCaseRestController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for MedicalCaseRestController.
 * Tests case search endpoint functionality.
 */
class MatchControllerIT extends BaseIntegrationTest {

    @Autowired
    private MedicalCaseRestController medicalCaseRestController;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    private String testCaseId1;
    private String testCaseId2;

    @BeforeEach
    void setUp() {
        // Clear existing data
        medicalCaseRepository.deleteAll();

        // Create test cases
        MedicalCase case1 = new MedicalCase(
                IdGenerator.generateId(),
                45,
                "Chest pain",
                "Severe chest pain radiating to left arm",
                "Acute myocardial infarction",
                List.of("I21.9"),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                "Patient requires immediate attention",
                null
        );
        testCaseId1 = medicalCaseRepository.insert(case1);

        MedicalCase case2 = new MedicalCase(
                IdGenerator.generateId(),
                30,
                "Headache",
                "Persistent headache for 3 days",
                "Migraine",
                List.of("G43.9"),
                List.of(),
                UrgencyLevel.MEDIUM,
                "Neurology",
                CaseType.SECOND_OPINION,
                "Patient has history of migraines",
                null
        );
        testCaseId2 = medicalCaseRepository.insert(case2);
    }

    @Test
    void testSearchCases_ByQuery() {
        // Search by query matching chief complaint
        ResponseEntity<List<MedicalCase>> response = medicalCaseRestController.searchCases(
                "chest", null, null, null, 0, 10
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Chest pain", response.getBody().get(0).chiefComplaint());

        // Search by query matching symptoms
        response = medicalCaseRestController.searchCases("headache", null, null, null, 0, 10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Headache", response.getBody().get(0).chiefComplaint());

        // Search by query matching additional notes
        response = medicalCaseRestController.searchCases("migraines", null, null, null, 0, 10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Headache", response.getBody().get(0).chiefComplaint());
    }

    @Test
    void testSearchCases_BySpecialty() {
        // Search by specialty
        ResponseEntity<List<MedicalCase>> response = medicalCaseRestController.searchCases(
                null, "Cardiology", null, null, 0, 10
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Cardiology", response.getBody().get(0).requiredSpecialty());

        response = medicalCaseRestController.searchCases(null, "Neurology", null, null, 0, 10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Neurology", response.getBody().get(0).requiredSpecialty());
    }

    @Test
    void testSearchCases_ByUrgencyLevel() {
        // Search by urgency level
        ResponseEntity<List<MedicalCase>> response = medicalCaseRestController.searchCases(
                null, null, "HIGH", null, 0, 10
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(UrgencyLevel.HIGH, response.getBody().get(0).urgencyLevel());

        response = medicalCaseRestController.searchCases(null, null, "MEDIUM", null, 0, 10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(UrgencyLevel.MEDIUM, response.getBody().get(0).urgencyLevel());
    }

    @Test
    void testSearchCases_ByCaseId() {
        ResponseEntity<List<MedicalCase>> response = medicalCaseRestController.searchCases(
                null, null, null, testCaseId1, 0, 10);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(testCaseId1, response.getBody().get(0).id());
        assertEquals("Chest pain", response.getBody().get(0).chiefComplaint());

        response = medicalCaseRestController.searchCases(null, null, null, testCaseId2, 0, 10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(testCaseId2, response.getBody().get(0).id());
        assertEquals("Headache", response.getBody().get(0).chiefComplaint());

        response = medicalCaseRestController.searchCases(null, null, null, "nonexistent-id", 0, 10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
    }

    @Test
    void testSearchCases_WithAllFilters() {
        // Search with all filters
        ResponseEntity<List<MedicalCase>> response = medicalCaseRestController.searchCases(
                "chest", "Cardiology", "HIGH", null, 0, 10
        );

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Chest pain", response.getBody().get(0).chiefComplaint());
        assertEquals("Cardiology", response.getBody().get(0).requiredSpecialty());
        assertEquals(UrgencyLevel.HIGH, response.getBody().get(0).urgencyLevel());

        // Search with filters that don't match
        response = medicalCaseRestController.searchCases("chest", "Neurology", "HIGH", null, 0, 10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
    }

    @Test
    void testSearchCases_WithMaxResults() {
        // Create multiple test cases
        for (int i = 0; i < 5; i++) {
            MedicalCase testCase = new MedicalCase(
                    IdGenerator.generateId(),
                    30 + i,
                    "Test case " + i,
                    "Symptoms " + i,
                    null,
                    List.of(),
                    List.of(),
                    UrgencyLevel.MEDIUM,
                    "Cardiology",
                    CaseType.INPATIENT,
                    null,
                    null
            );
            medicalCaseRepository.insert(testCase);
        }

        // Search with maxResults limit
        ResponseEntity<List<MedicalCase>> response = medicalCaseRestController.searchCases(
                "Test", null, null, null, 0, 3
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().size() <= 3,
                "Results should be limited to maxResults");
    }

    @Test
    void testSearchCases_EmptyQuery() {
        // Search with empty/null query should return all cases
        ResponseEntity<List<MedicalCase>> response = medicalCaseRestController.searchCases(
                null, null, null, null, 0, 10
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isEmpty(),
                "Should return cases even with null query");

        response = medicalCaseRestController.searchCases("", null, null, null, 0, 10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isEmpty(),
                "Should return cases even with empty query");
    }

    @Test
    void testSearchCases_CaseInsensitive() {
        // Test case-insensitive search
        ResponseEntity<List<MedicalCase>> response = medicalCaseRestController.searchCases(
                "CHEST", null, null, null, 0, 10
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Chest pain", response.getBody().get(0).chiefComplaint());

        response = medicalCaseRestController.searchCases("ChEsT", null, null, null, 0, 10);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void testSearchCases_DefaultMaxResults() {
        // Test default maxResults (20)
        ResponseEntity<List<MedicalCase>> response = medicalCaseRestController.searchCases(
                null, null, null, null, 0, 20
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Should return at least the 2 test cases we created
        assertTrue(response.getBody().size() >= 2);
    }
}
