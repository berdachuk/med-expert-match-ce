package com.berdachuk.medexpertmatch.llm.tools;

import com.berdachuk.medexpertmatch.clinicalexperience.domain.ClinicalExperience;
import com.berdachuk.medexpertmatch.clinicalexperience.repository.ClinicalExperienceRepository;
import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.doctor.repository.DoctorRepository;
import com.berdachuk.medexpertmatch.facility.domain.Facility;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.graph.service.GraphService;
import com.berdachuk.medexpertmatch.llm.tools.support.AgentToolGraphRowSupport;
import com.berdachuk.medexpertmatch.llm.tools.support.AgentToolSessionSupport;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring AI tools for graph-based network analytics and performance metrics.
 */
@Slf4j
@Component
public class GraphAnalyticsAgentTools {

    private final DoctorRepository doctorRepository;
    private final FacilityRepository facilityRepository;
    private final ClinicalExperienceRepository clinicalExperienceRepository;
    private final GraphService graphService;
    private final MedicalCaseRepository medicalCaseRepository;
    private final LogStreamService logStreamService;

    public GraphAnalyticsAgentTools(
            DoctorRepository doctorRepository,
            FacilityRepository facilityRepository,
            ClinicalExperienceRepository clinicalExperienceRepository,
            GraphService graphService,
            MedicalCaseRepository medicalCaseRepository,
            LogStreamService logStreamService) {
        this.doctorRepository = doctorRepository;
        this.facilityRepository = facilityRepository;
        this.clinicalExperienceRepository = clinicalExperienceRepository;
        this.graphService = graphService;
        this.medicalCaseRepository = medicalCaseRepository;
        this.logStreamService = logStreamService;
    }

    @Tool(description = "Query graph to find top experts for a specific condition based on historical performance and outcomes.")
    public List<String> graph_query_top_experts(
            @ToolParam(description = "ICD-10 code for the condition") String conditionCode,
            @ToolParam(description = "Maximum number of experts (default: 10)") Integer maxResults
    ) {
        log.info("graph_query_top_experts() tool called - conditionCode: {}, maxResults: {}", conditionCode, maxResults);
        String sessionId = AgentToolSessionSupport.getSessionId();
        logStreamService.logToolCall(sessionId, "graph_query_top_experts",
                String.format("conditionCode: %s, maxResults: %s", conditionCode, maxResults));

        try {
            if (conditionCode == null || conditionCode.trim().isEmpty()) {
                log.warn("Condition code is required for graph_query_top_experts");
                logStreamService.logError(sessionId, "graph_query_top_experts failed", "Condition code is required");
                return List.of("Error: Condition code is required");
            }

            int limit = (maxResults != null && maxResults > 0) ? maxResults : 10;

            if (!graphService.graphExists()) {
                log.debug("Graph does not exist, returning empty results");
                logStreamService.logToolResult(sessionId, "graph_query_top_experts",
                        "Graph not available - returning empty results");
                return List.of("Graph not available. Please ensure Apache AGE graph is populated.");
            }

            String cypherQuery = """
                    MATCH (d:Doctor)-[:TREATED]->(c:MedicalCase)-[:HAS_CONDITION]->(i:ICD10Code {code: $conditionCode})
                    RETURN d.id as doctorId, count(DISTINCT c) as caseCount, collect(DISTINCT c.id) as caseIds
                    ORDER BY count(DISTINCT c) DESC
                    LIMIT $maxResults
                    """;

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("conditionCode", conditionCode);
            parameters.put("maxResults", limit);

            List<Map<String, Object>> results = graphService.executeCypher(cypherQuery, parameters);

            List<String> expertResults = new ArrayList<>();
            for (Map<String, Object> row : results) {
                Object doctorIdObj = AgentToolGraphRowSupport.firstPresent(row, "doctorId", "c0");
                Object caseCountObj = AgentToolGraphRowSupport.firstPresent(row, "caseCount", "c1");

                if (doctorIdObj != null) {
                    String doctorId = doctorIdObj.toString();
                    long caseCount = caseCountObj != null ? Long.parseLong(caseCountObj.toString()) : 0;

                    List<ClinicalExperience> experiences = clinicalExperienceRepository.findByDoctorId(doctorId);
                    double avgRating = 0.0;
                    int successCount = 0;
                    int totalExperiences = experiences.size();

                    if (!experiences.isEmpty()) {
                        double totalRating = 0.0;
                        for (ClinicalExperience exp : experiences) {
                            if (exp.rating() != null) {
                                totalRating += exp.rating();
                            }
                            if ("SUCCESS".equalsIgnoreCase(exp.outcome()) ||
                                    "IMPROVED".equalsIgnoreCase(exp.outcome())) {
                                successCount++;
                            }
                        }
                        avgRating = totalExperiences > 0 ? totalRating / totalExperiences : 0.0;
                    }

                    double successRate = totalExperiences > 0 ? (double) successCount / totalExperiences : 0.0;

                    String result = String.format("Doctor ID: %s, Cases: %d, Avg Rating: %.2f, Success Rate: %.2f%%",
                            doctorId, caseCount, avgRating, successRate * 100);
                    expertResults.add(result);
                }
            }

            if (expertResults.isEmpty()) {
                logStreamService.logToolResult(sessionId, "graph_query_top_experts",
                        "No experts found for condition: " + conditionCode);
                return List.of("No experts found for condition code: " + conditionCode);
            }

            logStreamService.logToolResult(sessionId, "graph_query_top_experts",
                    String.format("Found %d experts", expertResults.size()));
            return expertResults;
        } catch (Exception e) {
            log.error("Error querying top experts from graph", e);
            logStreamService.logError(sessionId, "graph_query_top_experts failed", e.getMessage());
            return List.of("Error querying graph: " + e.getMessage());
        }
    }

    @Tool(description = "Aggregate performance metrics for doctors, conditions, or facilities.")
    public String aggregate_metrics(
            @ToolParam(description = "Type of entity: DOCTOR, CONDITION, or FACILITY") String entityType,
            @ToolParam(description = "Entity ID (optional, for specific entity)") String entityId,
            @ToolParam(description = "Type of metrics: PERFORMANCE, OUTCOMES, or VOLUME") String metricType
    ) {
        log.info("aggregate_metrics() tool called - entityType: {}, entityId: {}, metricType: {}",
                entityType, entityId, metricType);
        String sessionId = AgentToolSessionSupport.getSessionId();
        logStreamService.logToolCall(sessionId, "aggregate_metrics",
                String.format("entityType: %s, entityId: %s, metricType: %s", entityType, entityId, metricType));

        try {
            if (entityType == null || entityType.trim().isEmpty()) {
                log.warn("Entity type is required for aggregate_metrics");
                logStreamService.logError(sessionId, "aggregate_metrics failed", "Entity type is required");
                return "Error: Entity type is required (DOCTOR, CONDITION, or FACILITY)";
            }

            String normalizedEntityType = entityType.toUpperCase();
            String normalizedMetricType = metricType != null ? metricType.toUpperCase() : "PERFORMANCE";

            StringBuilder result = new StringBuilder();
            result.append(String.format("Metrics for %s", normalizedEntityType));
            if (entityId != null && !entityId.trim().isEmpty()) {
                result.append(String.format(" (ID: %s)", entityId));
            }
            result.append(":\n");

            switch (normalizedEntityType) {
                case "DOCTOR" -> result.append(aggregateDoctorMetrics(entityId, normalizedMetricType));
                case "CONDITION" -> result.append(aggregateConditionMetrics(entityId, normalizedMetricType));
                case "FACILITY" -> result.append(aggregateFacilityMetrics(entityId, normalizedMetricType));
                default -> {
                    log.warn("Unknown entity type: {}", entityType);
                    logStreamService.logError(sessionId, "aggregate_metrics failed",
                            "Unknown entity type: " + entityType);
                    return "Error: Unknown entity type. Must be DOCTOR, CONDITION, or FACILITY";
                }
            }

            logStreamService.logToolResult(sessionId, "aggregate_metrics",
                    String.format("Aggregated %s metrics for %s", normalizedMetricType, normalizedEntityType));
            return result.toString();
        } catch (Exception e) {
            log.error("Error aggregating metrics", e);
            logStreamService.logError(sessionId, "aggregate_metrics failed", e.getMessage());
            return "Error aggregating metrics: " + e.getMessage();
        }
    }

    private String aggregateDoctorMetrics(String doctorId, String metricType) {
        StringBuilder metrics = new StringBuilder();

        if (doctorId != null && !doctorId.trim().isEmpty()) {
            List<ClinicalExperience> experiences = clinicalExperienceRepository.findByDoctorId(doctorId);
            return aggregateDoctorMetricsFromExperiences(experiences, metricType);
        }

        List<String> allDoctorIds = doctorRepository.findAllIds(1000);
        Map<String, List<ClinicalExperience>> experiencesByDoctor =
                clinicalExperienceRepository.findByDoctorIds(allDoctorIds);

        int totalDoctors = experiencesByDoctor.size();
        int totalCases = 0;
        double totalRating = 0.0;
        int totalSuccess = 0;
        int totalExperiences = 0;

        for (List<ClinicalExperience> experiences : experiencesByDoctor.values()) {
            totalExperiences += experiences.size();
            for (ClinicalExperience exp : experiences) {
                totalCases++;
                if (exp.rating() != null) {
                    totalRating += exp.rating();
                }
                if ("SUCCESS".equalsIgnoreCase(exp.outcome()) ||
                        "IMPROVED".equalsIgnoreCase(exp.outcome())) {
                    totalSuccess++;
                }
            }
        }

        metrics.append(String.format("Total Doctors: %d\n", totalDoctors));
        metrics.append(String.format("Total Cases: %d\n", totalCases));
        if (totalExperiences > 0) {
            double avgRating = totalRating / totalExperiences;
            double successRate = (double) totalSuccess / totalExperiences;
            metrics.append(String.format("Average Rating: %.2f\n", avgRating));
            metrics.append(String.format("Success Rate: %.2f%%\n", successRate * 100));
        }

        return metrics.toString();
    }

    private String aggregateDoctorMetricsFromExperiences(List<ClinicalExperience> experiences, String metricType) {
        StringBuilder metrics = new StringBuilder();

        if (experiences.isEmpty()) {
            return "No clinical experiences found for this doctor.\n";
        }

        int totalCases = experiences.size();
        double totalRating = 0.0;
        int successCount = 0;
        int improvedCount = 0;
        int stableCount = 0;
        int complicatedCount = 0;
        Map<String, Integer> complexityDistribution = new HashMap<>();
        int totalTimeToResolution = 0;
        int casesWithTime = 0;

        for (ClinicalExperience exp : experiences) {
            if (exp.rating() != null) {
                totalRating += exp.rating();
            }
            if (exp.outcome() != null) {
                switch (exp.outcome().toUpperCase()) {
                    case "SUCCESS" -> successCount++;
                    case "IMPROVED" -> improvedCount++;
                    case "STABLE" -> stableCount++;
                    case "COMPLICATED" -> complicatedCount++;
                    default -> { }
                }
            }
            if (exp.complexityLevel() != null) {
                complexityDistribution.merge(exp.complexityLevel().toUpperCase(), 1, Integer::sum);
            }
            if (exp.timeToResolution() != null) {
                totalTimeToResolution += exp.timeToResolution();
                casesWithTime++;
            }
        }

        double avgRating = totalCases > 0 ? totalRating / totalCases : 0.0;
        double successRate = totalCases > 0 ? (double) (successCount + improvedCount) / totalCases : 0.0;
        double avgTimeToResolution = casesWithTime > 0 ? (double) totalTimeToResolution / casesWithTime : 0.0;

        switch (metricType) {
            case "PERFORMANCE" -> {
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append(String.format("Average Rating: %.2f\n", avgRating));
                metrics.append(String.format("Success Rate: %.2f%%\n", successRate * 100));
                metrics.append(String.format("Average Time to Resolution: %.1f days\n", avgTimeToResolution));
            }
            case "OUTCOMES" -> {
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append(String.format("Success: %d (%.2f%%)\n", successCount,
                        totalCases > 0 ? (double) successCount / totalCases * 100 : 0.0));
                metrics.append(String.format("Improved: %d (%.2f%%)\n", improvedCount,
                        totalCases > 0 ? (double) improvedCount / totalCases * 100 : 0.0));
                metrics.append(String.format("Stable: %d (%.2f%%)\n", stableCount,
                        totalCases > 0 ? (double) stableCount / totalCases * 100 : 0.0));
                metrics.append(String.format("Complicated: %d (%.2f%%)\n", complicatedCount,
                        totalCases > 0 ? (double) complicatedCount / totalCases * 100 : 0.0));
            }
            case "VOLUME" -> {
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append("Complexity Distribution:\n");
                for (Map.Entry<String, Integer> entry : complexityDistribution.entrySet()) {
                    metrics.append(String.format("  %s: %d (%.2f%%)\n", entry.getKey(), entry.getValue(),
                            totalCases > 0 ? (double) entry.getValue() / totalCases * 100 : 0.0));
                }
            }
            default -> {
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append(String.format("Average Rating: %.2f\n", avgRating));
                metrics.append(String.format("Success Rate: %.2f%%\n", successRate * 100));
                metrics.append(String.format("Average Time to Resolution: %.1f days\n", avgTimeToResolution));
            }
        }

        return metrics.toString();
    }

    private String aggregateConditionMetrics(String conditionCode, String metricType) {
        if (conditionCode != null && !conditionCode.trim().isEmpty()) {
            return aggregateSingleConditionMetrics(conditionCode, metricType);
        }

        StringBuilder metrics = new StringBuilder();
        List<String> caseIds = medicalCaseRepository.findAllIds(500);
        if (caseIds.isEmpty()) {
            return "No cases found.\n";
        }
        List<MedicalCase> cases = medicalCaseRepository.findByIds(caseIds);
        int totalCases = cases.size();
        Map<String, Integer> urgencyDistribution = new HashMap<>();
        Map<String, Integer> specialtyDistribution = new HashMap<>();
        Map<String, Integer> conditionDistribution = new HashMap<>();

        for (MedicalCase medicalCase : cases) {
            if (medicalCase.urgencyLevel() != null) {
                urgencyDistribution.merge(medicalCase.urgencyLevel().name(), 1, Integer::sum);
            }
            if (medicalCase.icd10Codes() != null) {
                for (String code : medicalCase.icd10Codes()) {
                    conditionDistribution.merge(code, 1, Integer::sum);
                }
            }
            if (medicalCase.requiredSpecialty() != null) {
                specialtyDistribution.merge(medicalCase.requiredSpecialty(), 1, Integer::sum);
            }
        }

        metrics.append(String.format("Total Cases: %d\n", totalCases));
        metrics.append(String.format("Unique Conditions: %d\n", conditionDistribution.size()));
        metrics.append(String.format("Unique Specialties: %d\n", specialtyDistribution.size()));

        metrics.append("\nCondition Distribution:\n");
        conditionDistribution.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> metrics.append(String.format("  %s: %d (%.1f%%)\n",
                        e.getKey(), e.getValue(),
                        totalCases > 0 ? (double) e.getValue() / totalCases * 100 : 0.0)));

        metrics.append("\nUrgency Distribution:\n");
        urgencyDistribution.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> metrics.append(String.format("  %s: %d (%.1f%%)\n",
                        e.getKey(), e.getValue(),
                        totalCases > 0 ? (double) e.getValue() / totalCases * 100 : 0.0)));

        return metrics.toString();
    }

    private String aggregateSingleConditionMetrics(String conditionCode, String metricType) {
        StringBuilder metrics = new StringBuilder();

        List<MedicalCase> cases = medicalCaseRepository.findByIcd10Code(conditionCode, 1000);

        if (cases.isEmpty()) {
            return String.format("No cases found for condition code: %s\n", conditionCode);
        }

        int totalCases = cases.size();
        Map<String, Integer> urgencyDistribution = new HashMap<>();
        Map<String, Integer> specialtyDistribution = new HashMap<>();

        for (MedicalCase medicalCase : cases) {
            if (medicalCase.urgencyLevel() != null) {
                urgencyDistribution.merge(medicalCase.urgencyLevel().name(), 1, Integer::sum);
            }
            if (medicalCase.requiredSpecialty() != null) {
                specialtyDistribution.merge(medicalCase.requiredSpecialty(), 1, Integer::sum);
            }
        }

        int doctorCount = 0;
        if (graphService.graphExists()) {
            try {
                String cypherQuery = """
                        MATCH (d:Doctor)-[:TREATED]->(c:MedicalCase)-[:HAS_CONDITION]->(i:ICD10Code {code: $conditionCode})
                        RETURN count(DISTINCT d) as doctorCount
                        """;
                Map<String, Object> params = new HashMap<>();
                params.put("conditionCode", conditionCode);
                List<Map<String, Object>> results = graphService.executeCypher(cypherQuery, params);
                if (!results.isEmpty() && results.get(0).get("doctorCount") != null) {
                    doctorCount = Integer.parseInt(results.get(0).get("doctorCount").toString());
                }
            } catch (Exception e) {
                log.debug("Could not query graph for doctor count", e);
            }
        }

        switch (metricType) {
            case "PERFORMANCE" -> {
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append(String.format("Doctors Treating Condition: %d\n", doctorCount));
            }
            case "OUTCOMES" -> {
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append("Note: Outcome metrics require case ID to query clinical experiences.\n");
            }
            case "VOLUME" -> {
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append(String.format("Doctors Treating Condition: %d\n", doctorCount));
                metrics.append("Urgency Distribution:\n");
                for (Map.Entry<String, Integer> entry : urgencyDistribution.entrySet()) {
                    metrics.append(String.format("  %s: %d (%.2f%%)\n", entry.getKey(), entry.getValue(),
                            totalCases > 0 ? (double) entry.getValue() / totalCases * 100 : 0.0));
                }
                metrics.append("Specialty Distribution:\n");
                for (Map.Entry<String, Integer> entry : specialtyDistribution.entrySet()) {
                    metrics.append(String.format("  %s: %d (%.2f%%)\n", entry.getKey(), entry.getValue(),
                            totalCases > 0 ? (double) entry.getValue() / totalCases * 100 : 0.0));
                }
            }
            default -> {
                metrics.append(String.format("Total Cases: %d\n", totalCases));
                metrics.append(String.format("Doctors Treating Condition: %d\n", doctorCount));
            }
        }

        return metrics.toString();
    }

    private String aggregateFacilityMetrics(String facilityId, String metricType) {
        StringBuilder metrics = new StringBuilder();

        if (facilityId != null && !facilityId.trim().isEmpty()) {
            Optional<Facility> facilityOpt = facilityRepository.findById(facilityId);
            if (facilityOpt.isEmpty()) {
                return String.format("Facility not found: %s\n", facilityId);
            }

            Facility facility = facilityOpt.get();
            metrics.append(String.format("Facility: %s\n", facility.name()));
            metrics.append(String.format("Type: %s\n", facility.facilityType()));
            metrics.append(String.format("Capacity: %d\n", facility.capacity() != null ? facility.capacity() : 0));

            if (graphService.graphExists()) {
                try {
                    String cypherQuery = """
                            MATCH (f:Facility {id: $facilityId})<-[:AFFILIATED_WITH]-(d:Doctor)-[:TREATED]->(c:MedicalCase)
                            RETURN count(DISTINCT d) as doctorCount, count(DISTINCT c) as caseCount
                            """;
                    Map<String, Object> params = new HashMap<>();
                    params.put("facilityId", facilityId);
                    List<Map<String, Object>> results = graphService.executeCypher(cypherQuery, params);
                    if (!results.isEmpty()) {
                        Object doctorCountObj = results.get(0).get("doctorCount");
                        Object caseCountObj = results.get(0).get("caseCount");
                        if (doctorCountObj != null) {
                            metrics.append(String.format("Affiliated Doctors: %s\n", doctorCountObj));
                        }
                        if (caseCountObj != null) {
                            metrics.append(String.format("Total Cases: %s\n", caseCountObj));
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not query graph for facility metrics", e);
                }
            }
        } else {
            List<Facility> facilities = facilityRepository.findAll();
            int totalFacilities = facilities.size();
            int totalCapacity = 0;
            Map<String, Integer> typeDistribution = new HashMap<>();

            for (Facility facility : facilities) {
                if (facility.capacity() != null) {
                    totalCapacity += facility.capacity();
                }
                if (facility.facilityType() != null) {
                    typeDistribution.merge(facility.facilityType(), 1, Integer::sum);
                }
            }

            metrics.append(String.format("Total Facilities: %d\n", totalFacilities));
            metrics.append(String.format("Total Capacity: %d\n", totalCapacity));
            metrics.append("Type Distribution:\n");
            for (Map.Entry<String, Integer> entry : typeDistribution.entrySet()) {
                metrics.append(String.format("  %s: %d\n", entry.getKey(), entry.getValue()));
            }
        }

        return metrics.toString();
    }
}
