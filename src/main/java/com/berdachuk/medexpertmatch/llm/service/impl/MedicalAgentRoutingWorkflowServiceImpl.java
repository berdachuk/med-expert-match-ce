package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentRoutingWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.MedicalAgentTools;
import com.berdachuk.medexpertmatch.retrieval.domain.FacilityMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Facility routing workflow extracted from the main medical agent service.
 */
@Slf4j
@Service
public class MedicalAgentRoutingWorkflowServiceImpl implements MedicalAgentRoutingWorkflowService {

    private final MedicalAgentLlmSupportService medicalAgentLlmSupportService;
    private final LogStreamService logStreamService;
    private final MedicalAgentTools medicalAgentTools;

    public MedicalAgentRoutingWorkflowServiceImpl(
            MedicalAgentLlmSupportService medicalAgentLlmSupportService,
            LogStreamService logStreamService,
            MedicalAgentTools medicalAgentTools) {
        this.medicalAgentLlmSupportService = medicalAgentLlmSupportService;
        this.logStreamService = logStreamService;
        this.medicalAgentTools = medicalAgentTools;
    }

    @Override
    public MedicalAgentService.AgentResponse routeCase(String caseId, Map<String, Object> request) {
        log.info("routeCase() called - caseId: {}", caseId);
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        logStreamService.setCurrentSessionId(sessionId);

        try {
            logStreamService.sendLog(sessionId, "INFO", "MedGemma routing analysis", "Analyzing case for routing");
            String caseAnalysis = medicalAgentLlmSupportService.analyzeCaseWithMedGemma(caseId);

            logStreamService.sendLog(sessionId, "INFO", "Routing tools", "Calling match_facilities_for_case");
            List<FacilityMatch> facilityMatches = medicalAgentTools.match_facilities_for_case(caseId, 5, null, null, null, null);
            logStreamService.sendLog(sessionId, "INFO", "Routing tools",
                    String.format("match_facilities_for_case returned %d facility matches",
                            facilityMatches != null ? facilityMatches.size() : 0));

            StringBuilder raw = new StringBuilder();
            raw.append("## Facility routing matches (match_facilities_for_case)\n");
            if (facilityMatches == null || facilityMatches.isEmpty()) {
                raw.append("No facility matches found for this case.\n");
            } else {
                for (FacilityMatch match : facilityMatches) {
                    var facility = match.facility();
                    raw.append("- Rank ").append(match.rank()).append(": ")
                            .append(facility != null ? facility.name() : "Unknown").append(" (")
                            .append(facility != null && facility.facilityType() != null ? facility.facilityType() : "N/A").append("), ")
                            .append(facility != null && facility.locationCity() != null ? facility.locationCity() : "").append(" ")
                            .append(facility != null && facility.locationState() != null ? facility.locationState() : "").append("; ")
                            .append("score: ").append(String.format("%.1f", match.routeScore())).append("; ")
                            .append(match.rationale() != null ? match.rationale() : "").append("\n");
                }
            }
            String toolResults = raw.toString();

            logStreamService.sendLog(sessionId, "INFO", "MedGemma routing interpretation", "Summarizing routing results");
            String response = medicalAgentLlmSupportService.summarizeRoutingResults(toolResults, caseAnalysis);

            logStreamService.logCompletion(sessionId, "Case routing", "Successfully completed facility routing");

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("caseId", caseId);
            metadata.put("skills", List.of("case-analyzer", "routing-planner"));
            metadata.put("hybridApproach", true);
            metadata.put("medgemmaUsed", true);
            metadata.put("facilityMatchCount", facilityMatches != null ? facilityMatches.size() : 0);

            return new MedicalAgentService.AgentResponse(response, metadata);
        } catch (Exception e) {
            log.error("Error in routeCase", e);
            logStreamService.logError(sessionId, "Case routing failed", e.getMessage());
            throw e;
        } finally {
            logStreamService.clearCurrentSessionId();
        }
    }
}
