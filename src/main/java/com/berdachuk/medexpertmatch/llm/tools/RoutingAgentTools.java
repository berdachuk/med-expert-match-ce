package com.berdachuk.medexpertmatch.llm.tools;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.facility.domain.Facility;
import com.berdachuk.medexpertmatch.facility.repository.FacilityRepository;
import com.berdachuk.medexpertmatch.graph.service.GraphService;
import com.berdachuk.medexpertmatch.llm.tools.support.AgentToolGraphRowSupport;
import com.berdachuk.medexpertmatch.llm.tools.support.AgentToolSessionSupport;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import com.berdachuk.medexpertmatch.retrieval.domain.FacilityMatch;
import com.berdachuk.medexpertmatch.retrieval.domain.RouteScoreResult;
import com.berdachuk.medexpertmatch.retrieval.domain.RoutingOptions;
import com.berdachuk.medexpertmatch.retrieval.service.MatchingService;
import com.berdachuk.medexpertmatch.retrieval.service.SemanticGraphRetrievalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spring AI tools for facility routing and regional center selection.
 */
@Slf4j
@Component
public class RoutingAgentTools {

    private final MedicalCaseRepository medicalCaseRepository;
    private final FacilityRepository facilityRepository;
    private final GraphService graphService;
    private final MatchingService matchingService;
    private final SemanticGraphRetrievalService semanticGraphRetrievalService;
    private final LogStreamService logStreamService;

    public RoutingAgentTools(
            MedicalCaseRepository medicalCaseRepository,
            FacilityRepository facilityRepository,
            GraphService graphService,
            @Lazy MatchingService matchingService,
            @Lazy SemanticGraphRetrievalService semanticGraphRetrievalService,
            LogStreamService logStreamService) {
        this.medicalCaseRepository = medicalCaseRepository;
        this.facilityRepository = facilityRepository;
        this.graphService = graphService;
        this.matchingService = matchingService;
        this.semanticGraphRetrievalService = semanticGraphRetrievalService;
        this.logStreamService = logStreamService;
    }

    @Tool(description = "Query graph to find candidate facilities/centers for a specific condition.")
    public List<String> graph_query_candidate_centers(
            @ToolParam(description = "ICD-10 code for the condition") String conditionCode,
            @ToolParam(description = "Maximum number of facilities (default: 10)") Integer maxResults
    ) {
        log.info("graph_query_candidate_centers() tool called - conditionCode: {}, maxResults: {}",
                conditionCode, maxResults);
        String sessionId = AgentToolSessionSupport.getSessionId();
        logStreamService.logToolCall(sessionId, "graph_query_candidate_centers",
                String.format("conditionCode: %s, maxResults: %s", conditionCode, maxResults));

        try {
            if (conditionCode == null || conditionCode.trim().isEmpty()) {
                log.warn("Condition code is required for graph_query_candidate_centers");
                logStreamService.logError(sessionId, "graph_query_candidate_centers failed",
                        "Condition code is required");
                return List.of("Error: Condition code is required");
            }

            int limit = (maxResults != null && maxResults > 0) ? maxResults : 10;

            if (!graphService.graphExists()) {
                log.debug("Graph does not exist, returning empty results");
                logStreamService.logToolResult(sessionId, "graph_query_candidate_centers",
                        "Graph not available - returning empty results");
                return List.of("Graph not available. Please ensure Apache AGE graph is populated.");
            }

            String cypherQuery = """
                    MATCH (f:Facility)<-[:AFFILIATED_WITH]-(d:Doctor)-[:TREATED]->(c:MedicalCase)-[:HAS_CONDITION]->(i:ICD10Code {code: $conditionCode})
                    RETURN DISTINCT f.id as facilityId, count(DISTINCT c) as caseCount
                    ORDER BY count(DISTINCT c) DESC
                    LIMIT $maxResults
                    """;

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("conditionCode", conditionCode);
            parameters.put("maxResults", limit);

            List<Map<String, Object>> results = graphService.executeCypher(cypherQuery, parameters);

            List<String> facilityResults = new ArrayList<>();
            for (Map<String, Object> row : results) {
                Object facilityIdObj = AgentToolGraphRowSupport.firstPresent(row, "facilityId", "c0");
                Object caseCountObj = AgentToolGraphRowSupport.firstPresent(row, "caseCount", "c1");

                if (facilityIdObj != null) {
                    String facilityId = facilityIdObj.toString();
                    long caseCount = caseCountObj != null ? Long.parseLong(caseCountObj.toString()) : 0;
                    facilityResults.add(String.format("Facility ID: %s, Cases: %d", facilityId, caseCount));
                }
            }

            if (facilityResults.isEmpty()) {
                logStreamService.logToolResult(sessionId, "graph_query_candidate_centers",
                        "No facilities found for condition: " + conditionCode);
                return List.of("No facilities found for condition code: " + conditionCode);
            }

            logStreamService.logToolResult(sessionId, "graph_query_candidate_centers",
                    String.format("Found %d facilities", facilityResults.size()));
            return facilityResults;
        } catch (Exception e) {
            log.error("Error querying candidate centers from graph", e);
            logStreamService.logError(sessionId, "graph_query_candidate_centers failed", e.getMessage());
            return List.of("Error querying graph: " + e.getMessage());
        }
    }

    @Tool(description = "Score a facility-case routing match using Semantic Graph Retrieval (combines complexity match, historical outcomes, capacity, and geography).")
    public RouteScoreResult semantic_graph_retrieval_route_score(
            @ToolParam(description = "Medical case ID") String caseId,
            @ToolParam(description = "Facility ID") String facilityId
    ) {
        log.info("semantic_graph_retrieval_route_score() tool called - caseId: {}, facilityId: {}",
                caseId, facilityId);
        String sessionId = AgentToolSessionSupport.getSessionId();
        logStreamService.logToolCall(sessionId, "semantic_graph_retrieval_route_score",
                String.format("caseId: %s, facilityId: %s", caseId, facilityId));

        try {
            Optional<MedicalCase> caseOpt = medicalCaseRepository.findById(caseId);
            Optional<Facility> facilityOpt = facilityRepository.findById(facilityId);

            if (caseOpt.isEmpty()) {
                log.warn("Medical case not found - caseId: {}", caseId);
                logStreamService.logError(sessionId, "semantic_graph_retrieval_route_score failed",
                        "Medical case not found: " + caseId);
                throw new IllegalArgumentException("Medical case not found: " + caseId);
            }

            if (facilityOpt.isEmpty()) {
                log.warn("Facility not found - facilityId: {}", facilityId);
                logStreamService.logError(sessionId, "semantic_graph_retrieval_route_score failed",
                        "Facility not found: " + facilityId);
                throw new IllegalArgumentException("Facility not found: " + facilityId);
            }

            RouteScoreResult result = semanticGraphRetrievalService.semanticGraphRetrievalRouteScore(
                    caseOpt.get(), facilityOpt.get());
            logStreamService.logToolResult(sessionId, "semantic_graph_retrieval_route_score",
                    String.format("Route score: %.2f", result.overallScore()));
            return result;
        } catch (IllegalArgumentException e) {
            logStreamService.logError(sessionId, "semantic_graph_retrieval_route_score failed", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error scoring facility-case routing match", e);
            logStreamService.logError(sessionId, "semantic_graph_retrieval_route_score failed", e.getMessage());
            throw e;
        }
    }

    @Tool(description = "Match facilities for case routing with scoring and ranking. Returns list of facility matches sorted by route score.")
    public List<FacilityMatch> match_facilities_for_case(
            @ToolParam(description = "Medical case ID") String caseId,
            @ToolParam(description = "Maximum number of results (default: 5)") Integer maxResults,
            @ToolParam(description = "Minimum route score threshold (0-100, optional)") Double minScore,
            @ToolParam(description = "List of preferred facility types (optional)") List<String> preferredFacilityTypes,
            @ToolParam(description = "List of required capabilities (optional)") List<String> requiredCapabilities,
            @ToolParam(description = "Maximum distance in kilometers (optional)") Double maxDistanceKm
    ) {
        String normalizedCaseId = caseId != null ? caseId.trim().toLowerCase() : null;

        log.info("match_facilities_for_case() tool called - caseId: {} (normalized: {}), maxResults: {}, minScore: {}, preferredFacilityTypes: {}, requiredCapabilities: {}, maxDistanceKm: {}",
                caseId, normalizedCaseId, maxResults, minScore, preferredFacilityTypes, requiredCapabilities, maxDistanceKm);

        RoutingOptions options = RoutingOptions.builder()
                .maxResults(maxResults != null && maxResults > 0 ? maxResults : 5)
                .minScore(minScore)
                .preferredFacilityTypes(preferredFacilityTypes)
                .requiredCapabilities(requiredCapabilities)
                .maxDistanceKm(maxDistanceKm)
                .build();

        return matchingService.matchFacilitiesForCase(normalizedCaseId, options);
    }
}
