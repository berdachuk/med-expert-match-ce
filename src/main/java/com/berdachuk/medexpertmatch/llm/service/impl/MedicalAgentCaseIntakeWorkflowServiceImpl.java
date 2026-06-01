package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.core.service.LogStreamService;
import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.exception.AgentExecutionException;
import com.berdachuk.medexpertmatch.llm.harness.CaseIntakeWorkflowEngine;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentCaseIntakeWorkflowService;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Case intake workflow delegated to harness engine (M30).
 */
@Slf4j
@Service
public class MedicalAgentCaseIntakeWorkflowServiceImpl implements MedicalAgentCaseIntakeWorkflowService {

    private final CaseIntakeWorkflowEngine caseIntakeWorkflowEngine;
    private final LogStreamService logStreamService;

    public MedicalAgentCaseIntakeWorkflowServiceImpl(
            CaseIntakeWorkflowEngine caseIntakeWorkflowEngine,
            LogStreamService logStreamService) {
        this.caseIntakeWorkflowEngine = caseIntakeWorkflowEngine;
        this.logStreamService = logStreamService;
    }

    @Override
    public MedicalAgentService.AgentResponse matchFromText(String caseText, Map<String, Object> request) {
        log.info("matchFromText() called - caseText length: {}", caseText != null ? caseText.length() : 0);
        if (caseText == null || caseText.isBlank()) {
            throw new IllegalArgumentException("caseText is required and cannot be empty");
        }

        String sessionId = (String) request.getOrDefault("sessionId", "default");
        logStreamService.setCurrentSessionId(sessionId);
        OrchestrationContextHolder.setSessionId(sessionId);
        logStreamService.sendLog(sessionId, "INFO", "matchFromText started", "Harness case intake");

        try {
            return caseIntakeWorkflowEngine.execute(caseText, request);
        } catch (IllegalArgumentException e) {
            log.error("Validation error in matchFromText: {}", e.getMessage());
            logStreamService.sendLog(sessionId, "ERROR", "Validation error", e.getMessage());
            throw e;
        } catch (AgentExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in matchFromText", e);
            logStreamService.sendLog(sessionId, "ERROR", "matchFromText failed", e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("matching")) {
                Map<String, Object> errorMetadata = new HashMap<>();
                errorMetadata.put("error", e.getMessage());
                return new MedicalAgentService.AgentResponse(
                        "Case intake completed but doctor matching failed: " + e.getMessage(),
                        errorMetadata);
            }
            throw new AgentExecutionException("Failed to match doctors from text: " + e.getMessage(), e);
        } finally {
            OrchestrationContextHolder.clear();
            logStreamService.clearCurrentSessionId();
        }
    }
}
