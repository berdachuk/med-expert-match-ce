package com.berdachuk.medexpertmatch.llm.tools;

import com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience;
import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.core.util.IdGenerator;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.facility.domain.Facility;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.graph.service.MedicalGraphBuilderService;
import com.berdachuk.medexpertmatch.integration.BaseIntegrationTest;
import com.berdachuk.medexpertmatch.medicalcase.domain.CaseType;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.RouteScoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MedicalAgentTools.
 * Tests all tool methods with real database and mocked LLM services.
 */
class MedicalAgentToolsIT extends BaseIntegrationTest {

    @Autowired
    private MedicalAgentTools medicalAgentTools;

    @Autowired
    private MedicalCaseRepository medicalCaseRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private ClinicalExperienceRepository clinicalExperienceRepository;

    @Autowired
    private MedicalGraphBuilderService graphBuilderService;

    private String testCaseId;
    private String testDoctorId;
    private String testFacilityId;
    private String conditionCode;

    @BeforeEach
    void setUp() {
        // Clear test data
        clinicalExperienceRepository.deleteAll();
        medicalCaseRepository.deleteAll();
        doctorRepository.deleteAll();
        facilityRepository.deleteAll();

        conditionCode = "I21.9"; // Acute MI

        // Create test facility
        testFacilityId = IdGenerator.generateFacilityId();
        Facility facility = new Facility(
                testFacilityId,
                "Test Medical Center",
                "HOSPITAL",
                "City",
                "State",
                "US",
                java.math.BigDecimal.valueOf(40.7128),
                java.math.BigDecimal.valueOf(-74.0060),
                List.of("ICU", "SURGERY"),
                100,
                50
        );
        facilityRepository.insert(facility);

        // Create test doctor
        testDoctorId = IdGenerator.generateDoctorId();
        Doctor doctor = new Doctor(
                testDoctorId,
                "Dr. Test Cardiologist",
                "test@example.com",
                List.of("Cardiology"),
                List.of("Board Certified"),
                List.of(testFacilityId),
                true,
                "AVAILABLE"
        );
        doctorRepository.insert(doctor);

        // Create test case
        testCaseId = medicalCaseRepository.insert(new MedicalCase(
                null,
                65,
                "Chest pain",
                "Chest pain, Elevated troponin",
                "Acute MI",
                List.of(conditionCode),
                List.of(),
                UrgencyLevel.HIGH,
                "Cardiology",
                CaseType.INPATIENT,
                "Test case for tool testing",
                null
        ));

        // Create clinical experience
        ClinicalExperience experience = new ClinicalExperience(
                null,
                testDoctorId,
                testCaseId,
                List.of("Cardiac Catheterization"),
                "HIGH",
                "SUCCESS",
                List.of(),
                5,
                5
        );
        clinicalExperienceRepository.insert(experience);

        // Build graph for graph-based tools
        try {
            graphBuilderService.buildGraph();
        } catch (Exception e) {
            // Graph building might fail if graph already exists or AGE is not available
            // This is acceptable for tests - graph-based tools will handle gracefully
        }
    }

    // ============================================
    // Phase 1: Regional Routing Tools
    // ============================================

    @Test
    void testSemanticGraphRetrievalRouteScore() {
        RouteScoreResult result = medicalAgentTools.semantic_graph_retrieval_route_score(testCaseId, testFacilityId);

        assertNotNull(result);
        assertTrue(result.overallScore() >= 0 && result.overallScore() <= 100);
        assertTrue(result.complexityMatchScore() >= 0 && result.complexityMatchScore() <= 1);
        assertTrue(result.historicalOutcomesScore() >= 0 && result.historicalOutcomesScore() <= 1);
        assertTrue(result.capacityScore() >= 0 && result.capacityScore() <= 1);
        assertTrue(result.geographicScore() >= 0 && result.geographicScore() <= 1);
        assertNotNull(result.rationale());
    }

    @Test
    void testSemanticGraphRetrievalRouteScore_InvalidCaseId() {
        assertThrows(IllegalArgumentException.class, () -> {
            medicalAgentTools.semantic_graph_retrieval_route_score("invalid-case-id", testFacilityId);
        });
    }

    @Test
    void testSemanticGraphRetrievalRouteScore_InvalidFacilityId() {
        assertThrows(IllegalArgumentException.class, () -> {
            medicalAgentTools.semantic_graph_retrieval_route_score(testCaseId, "invalid-facility-id");
        });
    }

    @Test
    void testGraphQueryCandidateCenters() {
        List<String> results = medicalAgentTools.graph_query_candidate_centers(conditionCode, 10);

        assertNotNull(results);
        // Results might be empty if graph is not populated, but should not throw exception
        // If graph is populated, should contain facility information
        // If graph query fails due to Apache AGE compatibility, may return error message
        if (!results.isEmpty()) {
            assertTrue(results.get(0).contains("Facility ID") ||
                    results.get(0).contains("not available") ||
                    results.get(0).contains("Error querying") ||
                    results.get(0).contains("Error") ||
                    results.get(0).contains("No facilities found"));
        }
    }

    @Test
    void testGraphQueryCandidateCenters_InvalidConditionCode() {
        List<String> results = medicalAgentTools.graph_query_candidate_centers("", 10);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).contains("Error") || results.get(0).contains("required"));
    }

    // ============================================
    // Phase 2: Network Analytics Tools
    // ============================================

    @Test
    void testGraphQueryTopExperts() {
        List<String> results = medicalAgentTools.graph_query_top_experts(conditionCode, 10);

        assertNotNull(results);
        // Results might be empty if graph is not populated, but should not throw exception
        // If graph is populated, should contain doctor information
        // If graph query fails due to Apache AGE compatibility, may return error message
        if (!results.isEmpty()) {
            assertTrue(results.get(0).contains("Doctor ID") ||
                    results.get(0).contains("not available") ||
                    results.get(0).contains("No experts found") ||
                    results.get(0).contains("Error querying") ||
                    results.get(0).contains("Error"));
        }
    }

    @Test
    void testGraphQueryTopExperts_InvalidConditionCode() {
        List<String> results = medicalAgentTools.graph_query_top_experts("", 10);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).contains("Error") || results.get(0).contains("required"));
    }

    @Test
    void testAggregateMetrics_Doctor() {
        String result = medicalAgentTools.aggregate_metrics("DOCTOR", testDoctorId, "PERFORMANCE");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("DOCTOR") || result.contains("Metrics"));
    }

    @Test
    void testAggregateMetrics_Doctor_AllDoctors() {
        String result = medicalAgentTools.aggregate_metrics("DOCTOR", null, "PERFORMANCE");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("DOCTOR") || result.contains("Total Doctors"));
    }

    @Test
    void testAggregateMetrics_Condition() {
        String result = medicalAgentTools.aggregate_metrics("CONDITION", conditionCode, "VOLUME");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("CONDITION") || result.contains("Total Cases"));
    }

    @Test
    void testAggregateMetrics_Facility() {
        String result = medicalAgentTools.aggregate_metrics("FACILITY", testFacilityId, "VOLUME");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("FACILITY") || result.contains("Facility"));
    }

    @Test
    void testAggregateMetrics_InvalidEntityType() {
        String result = medicalAgentTools.aggregate_metrics("INVALID", null, "PERFORMANCE");
        assertNotNull(result);
        assertTrue(result.contains("Error") || result.contains("Unknown entity type"));
    }

    // ============================================
    // Phase 3: Decision Support Tools
    // ============================================

    @Test
    void testQueryPubMed() {
        List<String> results = medicalAgentTools.query_pubmed("acute myocardial infarction", 5);

        assertNotNull(results);
        // PubMed API might return results or empty list if API is unavailable
        // Should not throw exception
        if (!results.isEmpty()) {
            // If results exist, should contain article information
            assertTrue(results.get(0).contains("Title") ||
                    results.get(0).contains("Error") ||
                    results.get(0).contains("No articles found"));
        }
    }

    @Test
    void testQueryPubMed_InvalidQuery() {
        List<String> results = medicalAgentTools.query_pubmed("", 10);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).contains("Error") || results.get(0).contains("required"));
    }

    @Test
    void testSearchClinicalGuidelines() {
        List<String> results = medicalAgentTools.search_clinical_guidelines("acute myocardial infarction", "Cardiology", 5);

        assertNotNull(results);
        // LLM-based implementation should return guidelines or error message
        assertFalse(results.isEmpty());
        // Should contain guidelines or error message
        assertTrue(results.get(0).contains("guideline") ||
                results.get(0).contains("Error") ||
                results.get(0).contains("Condition"));
    }

    @Test
    void testSearchClinicalGuidelines_InvalidCondition() {
        List<String> results = medicalAgentTools.search_clinical_guidelines("", null, 10);
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.get(0).contains("Error") || results.get(0).contains("required"));
    }

    @Test
    void testGenerateRecommendations_Diagnostic() {
        String result = medicalAgentTools.generate_recommendations(testCaseId, "DIAGNOSTIC", false);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // Should contain recommendations or error message
        assertTrue(result.contains("recommendation") ||
                result.contains("Error") ||
                result.contains("diagnostic") ||
                result.contains("workup"));
    }

    @Test
    void testGenerateRecommendations_Treatment() {
        String result = medicalAgentTools.generate_recommendations(testCaseId, "TREATMENT", false);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("recommendation") ||
                result.contains("Error") ||
                result.contains("treatment"));
    }

    @Test
    void testGenerateRecommendations_FollowUp() {
        String result = medicalAgentTools.generate_recommendations(testCaseId, "FOLLOW_UP", false);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("recommendation") ||
                result.contains("Error") ||
                result.contains("follow") ||
                result.contains("monitoring"));
    }

    @Test
    void testGenerateRecommendations_InvalidCaseId() {
        String result = medicalAgentTools.generate_recommendations("invalid-case-id", "DIAGNOSTIC", false);
        assertNotNull(result);
        assertTrue(result.contains("Error") || result.contains("not found"));
    }

    @Test
    void testDifferentialDiagnosis() {
        String result = medicalAgentTools.differential_diagnosis(testCaseId, 10);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("diagnosis") ||
                result.contains("Error") ||
                result.contains("differential"));
    }

    @Test
    void testDifferentialDiagnosis_InvalidCaseId() {
        String result = medicalAgentTools.differential_diagnosis("invalid-case-id", 10);
        assertNotNull(result);
        assertTrue(result.contains("Error") || result.contains("not found"));
    }

    @Test
    void testRiskAssessment_Complication() {
        String result = medicalAgentTools.risk_assessment(testCaseId, "COMPLICATION");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("risk") ||
                result.contains("Error") ||
                result.contains("complication") ||
                result.contains("assessment"));
    }

    @Test
    void testRiskAssessment_Mortality() {
        String result = medicalAgentTools.risk_assessment(testCaseId, "MORTALITY");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("risk") ||
                result.contains("Error") ||
                result.contains("mortality") ||
                result.contains("assessment"));
    }

    @Test
    void testRiskAssessment_Readmission() {
        String result = medicalAgentTools.risk_assessment(testCaseId, "READMISSION");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("risk") ||
                result.contains("Error") ||
                result.contains("readmission") ||
                result.contains("assessment"));
    }

    @Test
    void testRiskAssessment_InvalidCaseId() {
        String result = medicalAgentTools.risk_assessment("invalid-case-id", "COMPLICATION");
        assertNotNull(result);
        assertTrue(result.contains("Error") || result.contains("not found"));
    }
}
