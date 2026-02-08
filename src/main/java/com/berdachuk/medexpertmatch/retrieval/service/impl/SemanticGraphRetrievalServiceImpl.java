package com.berdachuk.medexpertmatch.retrieval.service.impl;

import com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience;
import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.doctor.domain.Doctor;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.embedding.service.EmbeddingService;
import com.berdachuk.medexpertmatch.facility.domain.Facility;
import com.berdachuk.medexpertmatch.graph.service.GraphQueryService;
import com.berdachuk.medexpertmatch.graph.service.GraphService;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.domain.UrgencyLevel;
import com.berdachuk.medexpertmatch.retrieval.domain.PriorityScore;
import com.berdachuk.medexpertmatch.retrieval.domain.RouteScoreResult;
import com.berdachuk.medexpertmatch.retrieval.domain.ScoreResult;
import com.berdachuk.medexpertmatch.retrieval.service.SemanticGraphRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Semantic Graph Retrieval service implementation.
 * Combines vector embeddings, graph relationships, and historical performance for scoring.
 */
@Slf4j
@Service
public class SemanticGraphRetrievalServiceImpl implements SemanticGraphRetrievalService {

    private static final double VECTOR_WEIGHT = 0.4;
    private static final double GRAPH_WEIGHT = 0.3;
    private static final double HISTORICAL_WEIGHT = 0.3;

    private static final int FACILITY_DOCTOR_LIMIT = 500;

    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final GraphService graphService;
    private final ClinicalExperienceRepository clinicalExperienceRepository;
    private final DoctorRepository doctorRepository;
    private final EmbeddingService embeddingService;
    private final LogStreamService logStreamService;
    private final com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository medicalCaseRepository;
    private final GraphQueryService graphQueryService;

    public SemanticGraphRetrievalServiceImpl(
            NamedParameterJdbcTemplate namedJdbcTemplate,
            GraphService graphService,
            ClinicalExperienceRepository clinicalExperienceRepository,
            DoctorRepository doctorRepository,
            EmbeddingService embeddingService,
            LogStreamService logStreamService,
            com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository medicalCaseRepository,
            GraphQueryService graphQueryService) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.graphService = graphService;
        this.clinicalExperienceRepository = clinicalExperienceRepository;
        this.doctorRepository = doctorRepository;
        this.embeddingService = embeddingService;
        this.logStreamService = logStreamService;
        this.medicalCaseRepository = medicalCaseRepository;
        this.graphQueryService = graphQueryService;
    }

    @Override
    @Transactional(readOnly = true)
    public ScoreResult score(MedicalCase medicalCase, Doctor doctor) {
        String sessionId = logStreamService.getCurrentSessionId();
        logStreamService.sendLog(sessionId, "INFO", "Graph usage: Starting Semantic Graph Retrieval scoring",
                String.format("Case: %s, Doctor: %s", medicalCase.id(), doctor.id()));

        // Calculate vector similarity score
        double vectorScore = calculateVectorSimilarityScore(medicalCase, doctor);

        // Calculate graph relationship score
        double graphScore = calculateGraphRelationshipScore(medicalCase, doctor);

        // Calculate historical performance score
        double historicalScore = calculateHistoricalPerformanceScore(doctor, medicalCase);

        // Combine scores with weighted average
        double overallScore = (vectorScore * VECTOR_WEIGHT) +
                (graphScore * GRAPH_WEIGHT) +
                (historicalScore * HISTORICAL_WEIGHT);

        // Scale to 0-100
        overallScore = overallScore * 100;

        String rationale = String.format(
                "Vector similarity: %.2f, Graph relationships: %.2f, Historical performance: %.2f",
                vectorScore, graphScore, historicalScore
        );

        logStreamService.sendLog(sessionId, "INFO", "Graph usage: Semantic Graph Retrieval scoring complete",
                String.format("Overall score: %.2f (Vector: %.2f, Graph: %.2f, Historical: %.2f)",
                        overallScore, vectorScore * 100, graphScore * 100, historicalScore * 100));

        return new ScoreResult(
                overallScore,
                vectorScore,
                graphScore,
                historicalScore,
                rationale
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RouteScoreResult semanticGraphRetrievalRouteScore(MedicalCase medicalCase, Facility facility) {
        // Calculate case complexity match score
        double complexityScore = calculateComplexityMatchScore(medicalCase, facility);

        // Calculate historical outcomes score
        double historicalOutcomesScore = calculateHistoricalOutcomesScore(medicalCase, facility);

        // Calculate center capacity score
        double capacityScore = calculateCapacityScore(facility);

        // Calculate geographic proximity score (simplified - always 0.5 for now)
        double geographicScore = 0.5;

        // Combine scores with weighted average
        double overallScore = (complexityScore * 0.3) +
                (historicalOutcomesScore * 0.3) +
                (capacityScore * 0.2) +
                (geographicScore * 0.2);

        // Scale to 0-100
        overallScore = overallScore * 100;

        String rationale = String.format(
                "Complexity match: %.2f, Historical outcomes: %.2f, Capacity: %.2f, Geographic: %.2f",
                complexityScore, historicalOutcomesScore, capacityScore, geographicScore
        );

        return new RouteScoreResult(
                overallScore,
                complexityScore,
                historicalOutcomesScore,
                capacityScore,
                geographicScore,
                rationale
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PriorityScore computePriorityScore(MedicalCase medicalCase) {
        // Calculate urgency score based on urgency level
        double urgencyScore = calculateUrgencyScore(medicalCase.urgencyLevel());

        // Calculate complexity score (simplified - based on urgency for now)
        double complexityScore = calculateCaseComplexityScore(medicalCase);

        // Calculate availability score (simplified - always 0.5 for now)
        double availabilityScore = 0.5;

        // Combine scores with weighted average
        double overallScore = (urgencyScore * 0.5) +
                (complexityScore * 0.3) +
                (availabilityScore * 0.2);

        // Scale to 0-100
        overallScore = overallScore * 100;

        String rationale = String.format(
                "Urgency: %.2f, Complexity: %.2f, Availability: %.2f",
                urgencyScore, complexityScore, availabilityScore
        );

        return new PriorityScore(
                overallScore,
                urgencyScore,
                complexityScore,
                availabilityScore,
                rationale
        );
    }

    /**
     * Calculates vector similarity score using cosine similarity of embeddings.
     */
    private double calculateVectorSimilarityScore(MedicalCase medicalCase, Doctor doctor) {
        try {
            // Check if query case has embedding
            boolean hasEmbedding = medicalCaseRepository.hasEmbedding(medicalCase.id());

            if (!hasEmbedding) {
                // No embedding available - return low score instead of neutral
                log.debug("Case {} has no embedding, returning low vector score", medicalCase.id());
                return 0.1;
            }

            // Find cases treated by this doctor via clinical experiences
            List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience> experiences =
                    clinicalExperienceRepository.findByDoctorId(doctor.id());

            if (experiences.isEmpty()) {
                // Doctor has no historical cases - return low score instead of neutral
                log.debug("Doctor {} has no clinical experiences, returning low vector score", doctor.id());
                return 0.1;
            }

            // Extract case IDs treated by doctor
            List<String> doctorCaseIds = experiences.stream()
                    .map(com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience::caseId)
                    .distinct()
                    .toList();

            if (doctorCaseIds.isEmpty()) {
                return 0.1;
            }

            // Calculate average cosine similarity between query case and doctor's cases
            // Using repository method instead of inline SQL
            Double avgSimilarity = medicalCaseRepository.calculateVectorSimilarity(medicalCase.id(), doctorCaseIds);

            if (avgSimilarity == null || Double.isNaN(avgSimilarity)) {
                log.warn("Could not calculate similarity for case {} and doctor {}: invalid result",
                        medicalCase.id(), doctor.id());
                return 0.1;
            }

            // Ensure score is in valid range [0, 1]
            double score = Math.max(0.0, Math.min(1.0, avgSimilarity));
            log.debug("Vector similarity score for case {} and doctor {}: {}", medicalCase.id(), doctor.id(), score);
            return score;
        } catch (Exception e) {
            log.error("Failed to calculate vector similarity score for case {} and doctor {}: {}",
                    medicalCase.id(), doctor.id(), e.getMessage(), e);
            return 0.0; // Return zero score on error instead of neutral
        }
    }

    /**
     * Calculates graph relationship score using Apache AGE (doctor-case relationships, expertise, specializations).
     */
    private double calculateGraphRelationshipScore(MedicalCase medicalCase, Doctor doctor) {
        String sessionId = logStreamService.getCurrentSessionId();
        try {
            if (!graphService.graphExists()) {
                log.warn("Graph does not exist, returning zero graph score");
                logStreamService.sendLog(sessionId, "WARN", "Graph usage: Graph does not exist, returning zero score", null);
                return 0.0;
            }

            logStreamService.sendLog(sessionId, "INFO", "Graph usage: Calculating relationship scores",
                    String.format("Case: %s, Doctor: %s", medicalCase.id(), doctor.id()));

            double directRelationshipScore = calculateDirectRelationshipScore(medicalCase, doctor);
            double conditionExpertiseScore = calculateConditionExpertiseScore(medicalCase, doctor);
            double specializationMatchScore = calculateSpecializationMatchScore(medicalCase, doctor);
            double similarCasesScore = calculateSimilarCasesScore(medicalCase, doctor);

            // Combine scores with weighted average
            // Direct relationships are most important (40%)
            // Condition expertise and specialization are equally important (25% each)
            // Similar cases provide additional signal (10%)
            double combinedScore = (directRelationshipScore * 0.4) +
                    (conditionExpertiseScore * 0.25) +
                    (specializationMatchScore * 0.25) +
                    (similarCasesScore * 0.1);

            String graphDetails = String.format(
                    "Direct relationships: %.2f (40%%), Condition expertise: %.2f (25%%), " +
                            "Specialization match: %.2f (25%%), Similar cases: %.2f (10%%), Combined: %.2f",
                    directRelationshipScore, conditionExpertiseScore, specializationMatchScore,
                    similarCasesScore, combinedScore
            );
            logStreamService.sendLog(sessionId, "INFO", "Graph usage: Relationship scores calculated", graphDetails);

            return Math.max(0.0, Math.min(1.0, combinedScore));
        } catch (com.berdachuk.medexpertmatch.core.exception.RetrievalException e) {
            // Graph query failed - return zero score instead of neutral to distinguish from missing data
            log.error("Graph query failed (graph may not exist or query error): {}", e.getMessage());
            logStreamService.sendLog(sessionId, "ERROR", "Graph usage: Query failed, returning zero score", e.getMessage());
            return 0.0;
        } catch (Exception e) {
            // Any other exception - return zero score instead of neutral
            log.error("Failed to calculate graph relationship score: {}", e.getMessage(), e);
            logStreamService.sendLog(sessionId, "ERROR", "Graph usage: Calculation error, returning zero score", e.getMessage());
            return 0.0; // Return zero score on error
        }
    }

    /**
     * Calculates direct relationship score (doctor treated/consulted on this exact case).
     */
    private double calculateDirectRelationshipScore(MedicalCase medicalCase, Doctor doctor) {
        String sessionId = logStreamService.getCurrentSessionId();
        return graphQueryService.calculateDirectRelationshipScore(doctor.id(), medicalCase.id(), sessionId);
    }

    /**
     * Calculates condition expertise score (doctor treats conditions present in this case).
     */
    private double calculateConditionExpertiseScore(MedicalCase medicalCase, Doctor doctor) {
        String sessionId = logStreamService.getCurrentSessionId();
        return graphQueryService.calculateConditionExpertiseScore(doctor.id(), medicalCase.icd10Codes(), sessionId);
    }

    /**
     * Calculates specialization match score (doctor specializes in case's required specialty).
     */
    private double calculateSpecializationMatchScore(MedicalCase medicalCase, Doctor doctor) {
        String sessionId = logStreamService.getCurrentSessionId();
        return graphQueryService.calculateSpecializationMatchScore(doctor.id(), medicalCase.requiredSpecialty(), sessionId);
    }

    /**
     * Calculates similar cases score (doctor treated cases with same ICD-10 codes).
     */
    private double calculateSimilarCasesScore(MedicalCase medicalCase, Doctor doctor) {
        String sessionId = logStreamService.getCurrentSessionId();
        return graphQueryService.calculateSimilarCasesScore(doctor.id(), medicalCase.icd10Codes(), sessionId);
    }

    /**
     * Calculates historical performance score based on clinical experiences.
     */
    private double calculateHistoricalPerformanceScore(Doctor doctor, MedicalCase medicalCase) {
        try {
            List<com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience> experiences =
                    clinicalExperienceRepository.findByDoctorId(doctor.id());

            if (experiences.isEmpty()) {
                // No historical data - return low score instead of neutral
                log.debug("Doctor {} has no clinical experiences, returning low historical performance score", doctor.id());
                return 0.1;
            }

            // Calculate average rating and success rate
            double totalRating = 0.0;
            int successCount = 0;
            int totalCount = experiences.size();
            int ratingCount = 0;

            for (var experience : experiences) {
                if (experience.rating() != null) {
                    totalRating += experience.rating();
                    ratingCount++;
                }
                if ("SUCCESS".equalsIgnoreCase(experience.outcome()) ||
                        "IMPROVED".equalsIgnoreCase(experience.outcome())) {
                    successCount++;
                }
            }

            // If no ratings available, use only success rate
            double avgRating = ratingCount > 0 ? totalRating / ratingCount : 2.5; // Default to mid-range
            double successRate = totalCount > 0 ? (double) successCount / totalCount : 0.0;

            // Normalize rating (1-5 scale) to 0-1
            double normalizedRating = (avgRating - 1.0) / 4.0;

            // Combine rating and success rate
            double performanceScore = (normalizedRating * 0.6) + (successRate * 0.4);

            return Math.max(0.0, Math.min(1.0, performanceScore));
        } catch (Exception e) {
            log.error("Failed to calculate historical performance score for doctor {}: {}",
                    doctor.id(), e.getMessage(), e);
            return 0.0; // Return zero score on error instead of neutral
        }
    }

    /**
     * Calculates complexity match score between case and facility.
     */
    private double calculateComplexityMatchScore(MedicalCase medicalCase, Facility facility) {
        // Simplified: check if facility has required capabilities
        if (medicalCase.requiredSpecialty() == null || facility.capabilities() == null) {
            return 0.5;
        }

        // Check if facility capabilities match case requirements
        // This is a simplified check - in reality, we'd need specialty-to-capability mapping
        boolean hasMatch = facility.capabilities().stream()
                .anyMatch(cap -> medicalCase.requiredSpecialty() != null &&
                        medicalCase.requiredSpecialty().toLowerCase().contains(cap.toLowerCase()));

        return hasMatch ? 1.0 : 0.5;
    }

    /**
     * Calculates historical outcomes score for facility-case routing.
     * Aggregates outcomes of clinical experiences for doctors affiliated with the facility.
     */
    private double calculateHistoricalOutcomesScore(MedicalCase medicalCase, Facility facility) {
        try {
            List<String> doctorIds = doctorRepository.findDoctorIdsByFacilityId(facility.id(), FACILITY_DOCTOR_LIMIT);
            if (doctorIds.isEmpty()) {
                return 0.5;
            }
            Map<String, List<ClinicalExperience>> experiencesByDoctor = clinicalExperienceRepository.findByDoctorIds(doctorIds);
            double totalRating = 0.0;
            int successCount = 0;
            int totalCount = 0;
            int ratingCount = 0;
            for (List<ClinicalExperience> experiences : experiencesByDoctor.values()) {
                for (ClinicalExperience experience : experiences) {
                    totalCount++;
                    if (experience.rating() != null) {
                        totalRating += experience.rating();
                        ratingCount++;
                    }
                    if ("SUCCESS".equalsIgnoreCase(experience.outcome()) ||
                            "IMPROVED".equalsIgnoreCase(experience.outcome())) {
                        successCount++;
                    }
                }
            }
            if (totalCount == 0) {
                return 0.5;
            }
            double avgRating = ratingCount > 0 ? totalRating / ratingCount : 2.5;
            double successRate = (double) successCount / totalCount;
            double normalizedRating = (avgRating - 1.0) / 4.0;
            double performanceScore = (normalizedRating * 0.6) + (successRate * 0.4);
            return Math.max(0.0, Math.min(1.0, performanceScore));
        } catch (Exception e) {
            log.error("Failed to calculate historical outcomes score for facility {}: {}",
                    facility.id(), e.getMessage(), e);
            return 0.5;
        }
    }

    /**
     * Calculates capacity score based on facility capacity and occupancy.
     */
    private double calculateCapacityScore(Facility facility) {
        if (facility.capacity() == null || facility.capacity() == 0) {
            return 0.5; // Unknown capacity
        }

        if (facility.currentOccupancy() == null) {
            return 1.0; // No occupancy data, assume available
        }

        double occupancyRate = (double) facility.currentOccupancy() / facility.capacity();
        // Lower occupancy = higher score (more capacity available)
        return 1.0 - occupancyRate;
    }

    /**
     * Calculates urgency score based on urgency level.
     */
    private double calculateUrgencyScore(UrgencyLevel urgencyLevel) {
        if (urgencyLevel == null) {
            return 0.5;
        }

        return switch (urgencyLevel) {
            case CRITICAL -> 1.0;
            case HIGH -> 0.75;
            case MEDIUM -> 0.5;
            case LOW -> 0.25;
        };
    }

    /**
     * Calculates case complexity score.
     */
    private double calculateCaseComplexityScore(MedicalCase medicalCase) {
        // Simplified: use urgency as proxy for complexity
        return calculateUrgencyScore(medicalCase.urgencyLevel());
    }
}
