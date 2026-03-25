package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.llm.service.MedicalAgentCaseAnalysisWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentCaseIntakeWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentDoctorMatchingWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentNetworkAnalyticsWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentQueuePrioritizationWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentRecommendationWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentRoutingWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Facade over workflow-oriented medical agent services.
 */
@Slf4j
@Service
public class MedicalAgentServiceImpl implements MedicalAgentService {

    private final MedicalAgentDoctorMatchingWorkflowService doctorMatchingWorkflowService;
    private final MedicalAgentQueuePrioritizationWorkflowService queuePrioritizationWorkflowService;
    private final MedicalAgentNetworkAnalyticsWorkflowService networkAnalyticsWorkflowService;
    private final MedicalAgentCaseAnalysisWorkflowService caseAnalysisWorkflowService;
    private final MedicalAgentRecommendationWorkflowService recommendationWorkflowService;
    private final MedicalAgentRoutingWorkflowService routingWorkflowService;
    private final MedicalAgentCaseIntakeWorkflowService caseIntakeWorkflowService;

    public MedicalAgentServiceImpl(
            MedicalAgentDoctorMatchingWorkflowService doctorMatchingWorkflowService,
            MedicalAgentQueuePrioritizationWorkflowService queuePrioritizationWorkflowService,
            MedicalAgentNetworkAnalyticsWorkflowService networkAnalyticsWorkflowService,
            MedicalAgentCaseAnalysisWorkflowService caseAnalysisWorkflowService,
            MedicalAgentRecommendationWorkflowService recommendationWorkflowService,
            MedicalAgentRoutingWorkflowService routingWorkflowService,
            MedicalAgentCaseIntakeWorkflowService caseIntakeWorkflowService) {
        this.doctorMatchingWorkflowService = doctorMatchingWorkflowService;
        this.queuePrioritizationWorkflowService = queuePrioritizationWorkflowService;
        this.networkAnalyticsWorkflowService = networkAnalyticsWorkflowService;
        this.caseAnalysisWorkflowService = caseAnalysisWorkflowService;
        this.recommendationWorkflowService = recommendationWorkflowService;
        this.routingWorkflowService = routingWorkflowService;
        this.caseIntakeWorkflowService = caseIntakeWorkflowService;
        log.info("MedicalAgentServiceImpl initialized as workflow facade");
    }

    @Override
    public AgentResponse matchDoctors(String caseId, Map<String, Object> request) {
        return doctorMatchingWorkflowService.matchDoctors(caseId, request);
    }

    @Override
    public AgentResponse prioritizeConsults(Map<String, Object> request) {
        return queuePrioritizationWorkflowService.prioritizeConsults(request);
    }

    @Override
    public AgentResponse networkAnalytics(Map<String, Object> request) {
        return networkAnalyticsWorkflowService.networkAnalytics(request);
    }

    @Override
    public AgentResponse analyzeCase(String caseId, Map<String, Object> request) {
        return caseAnalysisWorkflowService.analyzeCase(caseId, request);
    }

    @Override
    public AgentResponse generateRecommendations(String matchId, Map<String, Object> request) {
        return recommendationWorkflowService.generateRecommendations(matchId, request);
    }

    @Override
    public AgentResponse routeCase(String caseId, Map<String, Object> request) {
        return routingWorkflowService.routeCase(caseId, request);
    }

    @Override
    public AgentResponse matchFromText(String caseText, Map<String, Object> request) {
        return caseIntakeWorkflowService.matchFromText(caseText, request);
    }
}
