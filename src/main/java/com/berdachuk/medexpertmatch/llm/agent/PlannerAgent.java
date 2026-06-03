package com.berdachuk.medexpertmatch.llm.agent;

import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;
import com.berdachuk.medexpertmatch.llm.event.ExecutionPlan;
import com.berdachuk.medexpertmatch.llm.event.GoalIdentifiedEvent;
import com.berdachuk.medexpertmatch.llm.event.PlanReadyEvent;
import com.berdachuk.medexpertmatch.llm.harness.DoctorMatchWorkflowState;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowRun;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowRunJdbcRepository;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowRunStore;
import com.berdachuk.medexpertmatch.llm.harness.HarnessWorkflowType;
import com.berdachuk.medexpertmatch.llm.service.MedicalAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@Profile("event-driven")
public class PlannerAgent {

    private final ApplicationEventPublisher eventPublisher;
    private final HarnessWorkflowRunStore workflowRunStore;

    public PlannerAgent(ApplicationEventPublisher eventPublisher, HarnessWorkflowRunStore workflowRunStore) {
        this.eventPublisher = eventPublisher;
        this.workflowRunStore = workflowRunStore;
    }

    @EventListener
    public void onGoalIdentified(GoalIdentifiedEvent event) {
        log.info("PlannerAgent: goal identified session={} goalType={}", event.sessionId(), event.goal().goalType());
        ExecutionPlan plan = buildPlan(event.sessionId(), event.goal(), event.caseId());
        persistPlan(event.sessionId(), event.caseId(), plan);
        eventPublisher.publishEvent(new PlanReadyEvent(event.sessionId(), plan, Instant.now()));
        log.info("PlannerAgent: published PlanReadyEvent session={} steps={}", event.sessionId(), plan.steps().size());
    }

    ExecutionPlan buildPlan(String sessionId, GoalClassification goal, String caseId) {
        return switch (goal.goalType()) {
            case MATCH_DOCTORS -> new ExecutionPlan(sessionId, List.of(
                    new ExecutionPlan.Step("CONTEXT_BUILD", "CaseContextBundleService", caseId),
                    new ExecutionPlan.Step("DOCTOR_MATCH", "DoctorMatchWorkflowEngine", caseId),
                    new ExecutionPlan.Step("VERIFY", "AgentResponseVerifier", null),
                    new ExecutionPlan.Step("CRITIC", "MedicalAgentCriticService", null)
            ));
            case ROUTE_CASE -> new ExecutionPlan(sessionId, List.of(
                    new ExecutionPlan.Step("CONTEXT_BUILD", "CaseContextBundleService", caseId),
                    new ExecutionPlan.Step("ROUTING", "RoutingWorkflowEngine", caseId),
                    new ExecutionPlan.Step("VERIFY", "AgentResponseVerifier", null),
                    new ExecutionPlan.Step("CRITIC", "MedicalAgentCriticService", null)
            ));
            case ANALYZE_CASE -> new ExecutionPlan(sessionId, List.of(
                    new ExecutionPlan.Step("CONTEXT_BUILD", "CaseContextBundleService", caseId),
                    new ExecutionPlan.Step("ANALYSIS", "MedicalAgentCaseAnalysisWorkflowService", caseId)
            ));
            default -> new ExecutionPlan(sessionId, List.of());
        };
    }

    private void persistPlan(String sessionId, String caseId, ExecutionPlan plan) {
        String runId = HarnessWorkflowRunJdbcRepository.newRunId();
        workflowRunStore.save(new HarnessWorkflowRun(
                runId, sessionId, caseId != null ? caseId : "",
                HarnessWorkflowType.CHAT, DoctorMatchWorkflowState.TASK_CREATED,
                HarnessWorkflowRunJdbcRepository.newResumeToken(),
                plan.toString(), Instant.now(), Instant.now()));
    }
}