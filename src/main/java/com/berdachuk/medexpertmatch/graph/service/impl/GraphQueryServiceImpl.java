package com.berdachuk.medexpertmatch.graph.service.impl;

import com.berdachuk.medexpertmatch.graph.service.GraphQueryService;
import com.berdachuk.medexpertmatch.graph.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of GraphQueryService that encapsulates specific Cypher queries
 * used in Semantic Graph Retrieval.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphQueryServiceImpl implements GraphQueryService {

    private final GraphService graphService;

    @Override
    public double calculateDirectRelationshipScore(String doctorId, String caseId, String sessionId) {
        // First check for TREATED relationship
        String cypherQuery1 = """
                MATCH (d:Doctor {id: $doctorId})-[:TREATED]->(c:MedicalCase {id: $caseId})
                RETURN count(*)
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("doctorId", doctorId);
        params.put("caseId", caseId);

        List<Map<String, Object>> results1 = graphService.executeCypher(cypherQuery1, params);

        int treatedCount = 0;
        if (!results1.isEmpty()) {
            // The result is stored under "c" for single-column queries
            Object countObj = results1.get(0).get("c");
            if (countObj != null) {
                treatedCount = Integer.parseInt(countObj.toString());
            }
        }

        // Then check for CONSULTED_ON relationship
        String cypherQuery2 = """
                MATCH (d:Doctor {id: $doctorId})-[:CONSULTED_ON]->(c:MedicalCase {id: $caseId})
                RETURN count(*)
                """;

        List<Map<String, Object>> results2 = graphService.executeCypher(cypherQuery2, params);

        int consultedCount = 0;
        if (!results2.isEmpty()) {
            // The result is stored under "c" for single-column queries
            Object countObj = results2.get(0).get("c");
            if (countObj != null) {
                consultedCount = Integer.parseInt(countObj.toString());
            }
        }

        // If either relationship exists, return 1.0, otherwise 0.0
        int totalCount = treatedCount + consultedCount;
        double score = Math.min(1.0, totalCount);
        return score;
    }

    @Override
    public double calculateConditionExpertiseScore(String doctorId, List<String> icd10Codes, String sessionId) {
        if (icd10Codes == null || icd10Codes.isEmpty()) {
            // Logging removed for modularity - caller can handle logging if needed
            return 0.5; // No ICD-10 codes, return neutral score
        }

        // Logging removed for modularity - caller can handle logging if needed

        // Count how many case ICD-10 codes the doctor treats
        int matchingConditions = 0;
        for (String icd10Code : icd10Codes) {
            String cypherQuery = """
                    MATCH (d:Doctor {id: $doctorId})-[:TREATS_CONDITION]->(i:ICD10Code {code: $icd10Code})
                    RETURN count(*)
                    """;

            Map<String, Object> params = new HashMap<>();
            params.put("doctorId", doctorId);
            params.put("icd10Code", icd10Code);

            List<Map<String, Object>> results = graphService.executeCypher(cypherQuery, params);
            if (!results.isEmpty()) {
                // The result is stored under "c" for single-column queries
                Object countObj = results.get(0).get("c");
                if (countObj != null) {
                    int count = Integer.parseInt(countObj.toString());
                    if (count > 0) {
                        matchingConditions++;
                    }
                }
            }
        }

        double score = icd10Codes.isEmpty() ? 0.5 :
                (double) matchingConditions / icd10Codes.size();
        // Logging removed for modularity - caller can handle logging if needed
        // Normalize: all conditions match = 1.0, none match = 0.0
        return score;
    }

    @Override
    public double calculateSpecializationMatchScore(String doctorId, String specialtyName, String sessionId) {
        if (specialtyName == null || specialtyName.isEmpty()) {
            // Logging removed for modularity - caller can handle logging if needed
            return 0.5; // No specialty name, return neutral score
        }

        String cypherQuery = """
                MATCH (d:Doctor {id: $doctorId})-[:SPECIALIZES_IN]->(s:MedicalSpecialty {name: $specialtyName})
                RETURN count(*)
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("doctorId", doctorId);
        params.put("specialtyName", specialtyName);

        // Logging removed for modularity - caller can handle logging if needed

        List<Map<String, Object>> results = graphService.executeCypher(cypherQuery, params);

        if (results.isEmpty()) {
            // Logging removed for modularity - caller can handle logging if needed
            return 0.0;
        }

        // The result is stored under "c" for single-column queries
        Object countObj = results.get(0).get("c");
        if (countObj == null) {
            // Logging removed for modularity - caller can handle logging if needed
            return 0.0;
        }

        int count = Integer.parseInt(countObj.toString());
        double score = Math.min(1.0, count);
        // Logging removed for modularity - caller can handle logging if needed
        // Specialization match = 1.0, no match = 0.0
        return score;
    }

    @Override
    public double calculateSimilarCasesScore(String doctorId, List<String> icd10Codes, String sessionId) {
        if (icd10Codes == null || icd10Codes.isEmpty()) {
            // Logging removed for modularity - caller can handle logging if needed
            return 0.5; // No ICD-10 codes, return neutral score
        }

        // Logging removed for modularity - caller can handle logging if needed

        // Count cases treated by doctor that share at least one ICD-10 code with this case
        // Query for each ICD-10 code separately and aggregate (more compatible with Apache AGE)
        int totalSimilarCases = 0;
        for (String icd10Code : icd10Codes) {
            String cypherQuery = """
                    MATCH (d:Doctor {id: $doctorId})-[:TREATED]->(c:MedicalCase)-[:HAS_CONDITION]->(i:ICD10Code {code: $icd10Code})
                    RETURN count(*)
                    """;

            Map<String, Object> params = new HashMap<>();
            params.put("doctorId", doctorId);
            params.put("icd10Code", icd10Code);

            List<Map<String, Object>> results = graphService.executeCypher(cypherQuery, params);

            if (!results.isEmpty()) {
                // The result is stored under "c" for single-column queries
                Object countObj = results.get(0).get("c");
                if (countObj != null) {
                    int count = Integer.parseInt(countObj.toString());
                    totalSimilarCases = Math.max(totalSimilarCases, count); // Use max across all codes
                }
            }
        }

        // Normalize: 1+ similar cases = 1.0, 0 = 0.0
        // Use logarithmic scaling to prevent overwhelming signal from doctors with many cases
        double score;
        if (totalSimilarCases == 0) {
            score = 0.0;
        } else if (totalSimilarCases == 1) {
            score = 0.5;
        } else if (totalSimilarCases <= 5) {
            score = 0.75;
        } else {
            score = 1.0;
        }

        // Logging removed for modularity - caller can handle logging if needed
        return score;
    }
}
