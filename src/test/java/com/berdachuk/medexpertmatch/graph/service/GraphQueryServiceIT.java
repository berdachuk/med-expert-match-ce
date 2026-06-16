package com.berdachuk.medexpertmatch.graph.service;

import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test for GraphQueryService.
 * Uses Testcontainers PostgreSQL with Apache AGE to verify real graph database operations.
 * SCN-006 (network-analyzer): Covers graph operations for doctor-specialty-case relationships.
 * REQ-004: Network analytics via graph ops.
 */
class GraphQueryServiceIT extends BaseIntegrationTest {

    private static final String TEST_SESSION_ID = "test-session-001";
    @Autowired
    private GraphQueryService graphQueryService;
    @Autowired
    private GraphService graphService; // For setting up test data
    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear existing data to ensure test independence
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.clinical_experiences");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.medical_cases");
        namedJdbcTemplate.getJdbcTemplate().execute("DELETE FROM medexpertmatch.doctors");

        // Ensure graph exists and is clean
        try {
            // Drop and recreate graph for clean state
            String dropGraphSql = "SELECT * FROM ag_catalog.drop_graph('medexpertmatch_graph', true)";
            namedJdbcTemplate.getJdbcTemplate().execute(dropGraphSql);
        } catch (Exception e) {
            // Graph might not exist, ignore
        }

        // Create fresh graph
        try {
            String createGraphSql = "SELECT * FROM ag_catalog.create_graph('medexpertmatch_graph')";
            namedJdbcTemplate.getJdbcTemplate().execute(createGraphSql);
        } catch (Exception e) {
            // Graph might already exist, ignore
        }
    }

    @Test
    void testCalculateDirectRelationshipScore_NoRelationship_ReturnsZero() {
        // Test with non-existent doctor and case
        double score = graphQueryService.calculateDirectRelationshipScore(
                "non-existent-doctor", "non-existent-case", TEST_SESSION_ID);

        assertEquals(0.0, score, 0.01, "Should return 0.0 when no relationship exists");
    }

    @Test
    void testCalculateDirectRelationshipScore_WithTreatmentRelationship_ReturnsOne() {
        // Create doctor vertex
        createDoctorVertex("test-doctor-001", "Test Doctor");

        // Create medical case vertex
        createMedicalCaseVertex("test-case-001", "Test Case");

        // Create relationship
        createTreatmentRelationship("test-doctor-001", "test-case-001");

        // Test direct relationship score
        double score = graphQueryService.calculateDirectRelationshipScore(
                "test-doctor-001", "test-case-001", TEST_SESSION_ID);

        assertEquals(1.0, score, 0.01, "Should return 1.0 when direct treatment relationship exists");
    }

    @Test
    void testCalculateConditionExpertiseScore_NoICDCodes_ReturnsNeutral() {
        // Test with null ICD-10 codes
        double score = graphQueryService.calculateConditionExpertiseScore(
                "test-doctor-001", null, TEST_SESSION_ID);

        assertEquals(0.5, score, 0.01, "Should return 0.5 (neutral) when no ICD-10 codes provided");

        // Test with empty ICD-10 codes
        score = graphQueryService.calculateConditionExpertiseScore(
                "test-doctor-001", Collections.emptyList(), TEST_SESSION_ID);

        assertEquals(0.5, score, 0.01, "Should return 0.5 (neutral) when empty ICD-10 codes provided");
    }

    @Test
    void testCalculateConditionExpertiseScore_NoMatchingConditions_ReturnsZero() {
        // Create doctor vertex
        createDoctorVertex("test-doctor-002", "Test Doctor 2");

        // Test with ICD-10 codes but no matching conditions
        List<String> icd10Codes = Arrays.asList("A01.0", "B02.1");
        double score = graphQueryService.calculateConditionExpertiseScore(
                "test-doctor-002", icd10Codes, TEST_SESSION_ID);

        assertEquals(0.0, score, 0.01, "Should return 0.0 when no conditions match");
    }

    @Test
    void testCalculateConditionExpertiseScore_PartialMatchingConditions_ReturnsPartialScore() {
        // Create doctor vertex
        createDoctorVertex("test-doctor-003", "Test Doctor 3");

        // Create ICD-10 code vertices and relationships
        createICD10CodeVertex("A01.0", "Test Condition A");
        createICD10CodeVertex("B02.1", "Test Condition B");
        createICD10CodeVertex("C03.2", "Test Condition C");

        // Create relationship for only one condition
        createDoctorTreatsConditionRelationship("test-doctor-003", "A01.0");

        // Test with mixed matching conditions
        List<String> icd10Codes = Arrays.asList("A01.0", "B02.1"); // 1 out of 2 match
        double score = graphQueryService.calculateConditionExpertiseScore(
                "test-doctor-003", icd10Codes, TEST_SESSION_ID);

        assertEquals(0.5, score, 0.01, "Should return 0.5 when half conditions match");
    }

    @Test
    void testCalculateSpecializationMatchScore_NoSpecialty_ReturnsNeutral() {
        // Test with null specialty
        double score = graphQueryService.calculateSpecializationMatchScore(
                "test-doctor-004", null, TEST_SESSION_ID);

        assertEquals(0.5, score, 0.01, "Should return 0.5 (neutral) when no specialty provided");

        // Test with empty specialty
        score = graphQueryService.calculateSpecializationMatchScore(
                "test-doctor-004", "", TEST_SESSION_ID);

        assertEquals(0.5, score, 0.01, "Should return 0.5 (neutral) when empty specialty provided");
    }

    @Test
    void testCalculateSpecializationMatchScore_NoMatchingSpecialty_ReturnsZero() {
        // Create doctor vertex
        createDoctorVertex("test-doctor-005", "Test Doctor 5");

        // Test with specialty but no matching relationship
        double score = graphQueryService.calculateSpecializationMatchScore(
                "test-doctor-005", "Cardiology", TEST_SESSION_ID);

        assertEquals(0.0, score, 0.01, "Should return 0.0 when no specialization match");
    }

    @Test
    void testCalculateSpecializationMatchScore_WithMatchingSpecialty_ReturnsOne() {
        // Create doctor vertex
        createDoctorVertex("test-doctor-006", "Test Doctor 6");

        // Create specialty vertex and relationship
        createSpecialtyVertex("Cardiology");
        createDoctorSpecializesInRelationship("test-doctor-006", "Cardiology");

        // Test with matching specialty
        double score = graphQueryService.calculateSpecializationMatchScore(
                "test-doctor-006", "Cardiology", TEST_SESSION_ID);

        assertEquals(1.0, score, 0.01, "Should return 1.0 when specialization matches");
    }

    @Test
    void testCalculateSpecializationMatchScore_CaseNameIsSubstringOfDoctorName_ReturnsOne() {
        // M75: case asks for the simpler name "Cardiology" but the doctor
        // is recorded under the more specific
        // "Advanced Heart Failure and Transplant Cardiology". The
        // pre-M75 exact match returned 0.0, killing the 25% graph
        // component for Find Specialist.
        createDoctorVertex("test-doctor-006a", "Test Doctor 6a");
        createSpecialtyVertex("Advanced Heart Failure and Transplant Cardiology");
        createDoctorSpecializesInRelationship("test-doctor-006a",
                "Advanced Heart Failure and Transplant Cardiology");

        double score = graphQueryService.calculateSpecializationMatchScore(
                "test-doctor-006a", "Cardiology", TEST_SESSION_ID);

        assertEquals(1.0, score, 0.01,
                "M75: doctor specialty contains case specialty → 1.0 (substring match)");
    }

    @Test
    void testCalculateSpecializationMatchScore_DoctorNameIsSubstringOfCaseName_ReturnsOne() {
        // M75: case asks for the longer name
        // "Advanced Heart Failure and Transplant Cardiology" and the
        // doctor is recorded under the simpler "Cardiology". The
        // pre-M75 exact match returned 0.0.
        createDoctorVertex("test-doctor-006b", "Test Doctor 6b");
        createSpecialtyVertex("Cardiology");
        createDoctorSpecializesInRelationship("test-doctor-006b", "Cardiology");

        double score = graphQueryService.calculateSpecializationMatchScore(
                "test-doctor-006b", "Advanced Heart Failure and Transplant Cardiology",
                TEST_SESSION_ID);

        assertEquals(1.0, score, 0.01,
                "M75: case specialty contains doctor specialty → 1.0 (substring match)");
    }

    @Test
    void testCalculateSpecializationMatchScore_NoOverlap_ReturnsZero() {
        // M75: doctor is a Urologist, case asks for Cardiology. The two
        // names share no meaningful substrings (any overlap is a
        // 3-letter common word like "and" / "the") so the result
        // must be 0.0 — substring match must not produce false
        // positives for unrelated specialties.
        createDoctorVertex("test-doctor-006c", "Test Doctor 6c");
        createSpecialtyVertex("Urology");
        createDoctorSpecializesInRelationship("test-doctor-006c", "Urology");

        double score = graphQueryService.calculateSpecializationMatchScore(
                "test-doctor-006c", "Cardiology", TEST_SESSION_ID);

        assertEquals(0.0, score, 0.01,
                "M75: no overlap → 0.0 (no false positives for unrelated specialties)");
    }

    @Test
    void testCalculateSpecializationMatchScore_CaseInsensitive_ReturnsOne() {
        // M75: case-insensitive match. "CARDIOLOGY" / "cardiology" /
        // "Cardiology" all match the same vertex.
        createDoctorVertex("test-doctor-006d", "Test Doctor 6d");
        createSpecialtyVertex("Cardiology");
        createDoctorSpecializesInRelationship("test-doctor-006d", "Cardiology");

        double score = graphQueryService.calculateSpecializationMatchScore(
                "test-doctor-006d", "CARDIOLOGY", TEST_SESSION_ID);

        assertEquals(1.0, score, 0.01,
                "M75: case-insensitive → 1.0");
    }

    @Test
    void testCalculateSimilarCasesScore_NoICDCodes_ReturnsNeutral() {
        // Test with null ICD-10 codes
        double score = graphQueryService.calculateSimilarCasesScore(
                "test-doctor-007", null, TEST_SESSION_ID);

        assertEquals(0.5, score, 0.01, "Should return 0.5 (neutral) when no ICD-10 codes provided");

        // Test with empty ICD-10 codes
        score = graphQueryService.calculateSimilarCasesScore(
                "test-doctor-007", Collections.emptyList(), TEST_SESSION_ID);

        assertEquals(0.5, score, 0.01, "Should return 0.5 (neutral) when empty ICD-10 codes provided");
    }

    @Test
    void testCalculateSimilarCasesScore_NoSimilarCases_ReturnsZero() {
        // Create doctor vertex
        createDoctorVertex("test-doctor-008", "Test Doctor 8");

        // Test with ICD-10 codes but no similar cases
        List<String> icd10Codes = Arrays.asList("A01.0", "B02.1");
        double score = graphQueryService.calculateSimilarCasesScore(
                "test-doctor-008", icd10Codes, TEST_SESSION_ID);

        assertEquals(0.0, score, 0.01, "Should return 0.0 when no similar cases found");
    }

    @Test
    void testCalculateSimilarCasesScore_WithSimilarCases_ReturnsAppropriateScore() {
        // Create doctor vertex
        createDoctorVertex("test-doctor-009", "Test Doctor 9");

        // Create medical case vertex with ICD-10 code
        createMedicalCaseVertex("test-case-002", "Test Case 2");
        createICD10CodeVertex("A01.0", "Test Condition A");

        // Create relationships
        createTreatmentRelationship("test-doctor-009", "test-case-002");
        createCaseHasConditionRelationship("test-case-002", "A01.0");

        // Test with matching ICD-10 code
        List<String> icd10Codes = Arrays.asList("A01.0");
        double score = graphQueryService.calculateSimilarCasesScore(
                "test-doctor-009", icd10Codes, TEST_SESSION_ID);

        // Should return 0.5 for 1 similar case
        assertEquals(0.5, score, 0.01, "Should return 0.5 when one similar case found");
    }

    // Helper methods for creating test data
    private void createDoctorVertex(String doctorId, String name) {
        String cypher = """
                CREATE (d:Doctor {id: $id, name: $name})
                RETURN d
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("id", doctorId);
        params.put("name", name);

        graphService.executeCypher(cypher, params);
    }

    private void createMedicalCaseVertex(String caseId, String title) {
        String cypher = """
                CREATE (c:MedicalCase {id: $id, title: $title})
                RETURN c
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("id", caseId);
        params.put("title", title);

        graphService.executeCypher(cypher, params);
    }

    private void createICD10CodeVertex(String code, String description) {
        String cypher = """
                CREATE (i:ICD10Code {code: $code, description: $description})
                RETURN i
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("code", code);
        params.put("description", description);

        graphService.executeCypher(cypher, params);
    }

    private void createSpecialtyVertex(String name) {
        String cypher = """
                CREATE (s:MedicalSpecialty {name: $name})
                RETURN s
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("name", name);

        graphService.executeCypher(cypher, params);
    }

    private void createTreatmentRelationship(String doctorId, String caseId) {
        String cypher = """
                MATCH (d:Doctor {id: $doctorId}), (c:MedicalCase {id: $caseId})
                CREATE (d)-[:TREATED]->(c)
                RETURN d
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("doctorId", doctorId);
        params.put("caseId", caseId);

        graphService.executeCypher(cypher, params);
    }

    private void createDoctorTreatsConditionRelationship(String doctorId, String icd10Code) {
        String cypher = """
                MATCH (d:Doctor {id: $doctorId}), (i:ICD10Code {code: $icd10Code})
                CREATE (d)-[:TREATS_CONDITION]->(i)
                RETURN d
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("doctorId", doctorId);
        params.put("icd10Code", icd10Code);

        graphService.executeCypher(cypher, params);
    }

    private void createDoctorSpecializesInRelationship(String doctorId, String specialtyName) {
        String cypher = """
                MATCH (d:Doctor {id: $doctorId}), (s:MedicalSpecialty {name: $specialtyName})
                CREATE (d)-[:SPECIALIZES_IN]->(s)
                RETURN d
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("doctorId", doctorId);
        params.put("specialtyName", specialtyName);

        graphService.executeCypher(cypher, params);
    }

    private void createCaseHasConditionRelationship(String caseId, String icd10Code) {
        String cypher = """
                MATCH (c:MedicalCase {id: $caseId}), (i:ICD10Code {code: $icd10Code})
                CREATE (c)-[:HAS_CONDITION]->(i)
                RETURN c
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("caseId", caseId);
        params.put("icd10Code", icd10Code);

        graphService.executeCypher(cypher, params);
    }
}
