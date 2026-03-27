package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentLlmSupportService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentNetworkAnalyticsWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import com.berdachuk.medexpertmatch.llm.tools.MedicalAgentTools;
import com.berdachuk.medexpertmatch.medicalcase.domain.MedicalCase;
import com.berdachuk.medexpertmatch.medicalcase.repository.MedicalCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Network analytics workflow extracted from the main medical agent service.
 */
@Slf4j
@Service
public class MedicalAgentNetworkAnalyticsWorkflowServiceImpl implements MedicalAgentNetworkAnalyticsWorkflowService {

    private static final int MAX_NETWORK_ANALYTICS_CONDITIONS = 3;
    private static final int MAX_EXPERTS_PER_CONDITION = 10;

    private final MedicalCaseRepository medicalCaseRepository;
    private final MedicalAgentLlmSupportService medicalAgentLlmSupportService;
    private final LogStreamService logStreamService;
    private final MedicalAgentTools medicalAgentTools;

    public MedicalAgentNetworkAnalyticsWorkflowServiceImpl(
            MedicalCaseRepository medicalCaseRepository,
            MedicalAgentLlmSupportService medicalAgentLlmSupportService,
            LogStreamService logStreamService,
            MedicalAgentTools medicalAgentTools) {
        this.medicalCaseRepository = medicalCaseRepository;
        this.medicalAgentLlmSupportService = medicalAgentLlmSupportService;
        this.logStreamService = logStreamService;
        this.medicalAgentTools = medicalAgentTools;
    }

    @Override
    public MedicalAgentService.AgentResponse networkAnalytics(Map<String, Object> request) {
        log.info("networkAnalytics() called");
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        logStreamService.setCurrentSessionId(sessionId);

        try {
            logStreamService.sendLog(sessionId, "INFO", "Network analytics", "Starting network analytics (run tools, then summarize)");
            List<String> conditionCodes = resolveConditionCodes(request);
            logStreamService.sendLog(sessionId, "INFO", "Network analytics", "Condition codes: " + conditionCodes);

            StringBuilder raw = new StringBuilder();
            int maxConditions = Math.min(MAX_NETWORK_ANALYTICS_CONDITIONS, conditionCodes.size());

            for (int i = 0; i < maxConditions; i++) {
                String code = conditionCodes.get(i);
                logStreamService.sendLog(sessionId, "INFO", "Graph query", "Querying top experts for condition: " + code);
                List<String> experts = medicalAgentTools.graph_query_top_experts(code, MAX_EXPERTS_PER_CONDITION);
                raw.append("## Top experts for condition ").append(code).append("\n");
                for (String line : experts) {
                    raw.append("- ").append(line).append("\n");
                }
                raw.append("\n");
            }

            logStreamService.sendLog(sessionId, "INFO", "Aggregate metrics", "Aggregating condition and doctor metrics");
            String conditionMetrics = medicalAgentTools.aggregate_metrics("CONDITION", null, "PERFORMANCE");
            String doctorMetrics = medicalAgentTools.aggregate_metrics("DOCTOR", null, "PERFORMANCE");
            raw.append("## Aggregate metrics (condition)\n").append(conditionMetrics).append("\n");
            raw.append("## Aggregate metrics (doctor)\n").append(doctorMetrics).append("\n");

            logStreamService.sendLog(sessionId, "INFO", "LLM", "Summarizing results for user");
            String response = medicalAgentLlmSupportService.summarizeNetworkAnalyticsResults(raw.toString());

            logStreamService.logCompletion(sessionId, "Network analytics", "Successfully completed network analytics");

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("skills", List.of("network-analyzer"));
            metadata.put("conditionCodes", conditionCodes);
            return new MedicalAgentService.AgentResponse(response, metadata);
        } catch (Exception e) {
            log.error("Error in network analytics", e);
            logStreamService.logError(sessionId, "Network analytics failed", e.getMessage());
            throw e;
        } finally {
            logStreamService.clearCurrentSessionId();
        }
    }

    private List<String> resolveConditionCodes(Map<String, Object> request) {
        Object single = request.get("conditionCode");
        if (single != null && !single.toString().isBlank()) {
            return List.of(single.toString().trim());
        }
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) request.get("conditionCodes");
        if (list != null && !list.isEmpty()) {
            return list.stream().filter(code -> code != null && !code.isBlank()).map(String::trim).limit(5).toList();
        }

        List<String> caseIds = medicalCaseRepository.findAllIds(30);
        if (caseIds.isEmpty()) {
            return List.of("I21.9");
        }
        List<MedicalCase> cases = medicalCaseRepository.findByIds(caseIds);
        Set<String> codes = new LinkedHashSet<>();
        for (MedicalCase medicalCase : cases) {
            if (medicalCase.icd10Codes() != null) {
                codes.addAll(medicalCase.icd10Codes());
            }
        }
        if (codes.isEmpty()) {
            return List.of("I21.9");
        }
        return new ArrayList<>(codes).subList(0, Math.min(5, codes.size()));
    }
}
