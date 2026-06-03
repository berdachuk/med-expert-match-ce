package com.berdachuk.medexpertmatch.llm.agent;

import com.berdachuk.medexpertmatch.llm.event.ContextReadyEvent;
import com.berdachuk.medexpertmatch.llm.event.ExecutionPlan;
import com.berdachuk.medexpertmatch.llm.event.PlanReadyEvent;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundle;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextBundleService;
import com.berdachuk.medexpertmatch.llm.harness.CaseContextIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@Profile("event-driven")
public class ContextBuilderAgent {

    private final ApplicationEventPublisher eventPublisher;
    private final CaseContextBundleService caseContextBundleService;

    public ContextBuilderAgent(ApplicationEventPublisher eventPublisher, CaseContextBundleService caseContextBundleService) {
        this.eventPublisher = eventPublisher;
        this.caseContextBundleService = caseContextBundleService;
    }

    @EventListener
    public void onPlanReady(PlanReadyEvent event) {
        log.info("ContextBuilderAgent: plan ready session={} steps={}", event.sessionId(), event.plan().steps().size());

        String caseId = findCaseId(event.plan());
        if (caseId == null) {
            log.warn("ContextBuilderAgent: no caseId in plan, skipping context build session={}", event.sessionId());
            return;
        }

        CaseContextIntent intent = resolveIntent(event.plan());
        CaseContextBundle bundle = caseContextBundleService.build(caseId, intent);
        log.info("ContextBuilderAgent: built context session={} caseId={} intent={} sections={}",
                event.sessionId(), caseId, intent, bundle.coreSections().size());

        eventPublisher.publishEvent(new ContextReadyEvent(event.sessionId(), bundle, Instant.now()));
    }

    private static String findCaseId(ExecutionPlan plan) {
        return plan.steps().stream()
                .filter(s -> s.params() instanceof String)
                .map(s -> (String) s.params())
                .findFirst()
                .orElse(null);
    }

    private static CaseContextIntent resolveIntent(ExecutionPlan plan) {
        return plan.steps().stream()
                .map(ExecutionPlan.Step::stepType)
                .filter(s -> s.startsWith("DOCTOR_MATCH"))
                .findFirst()
                .map(s -> CaseContextIntent.MATCH)
                .orElseGet(() -> plan.steps().stream()
                        .map(ExecutionPlan.Step::stepType)
                        .filter(s -> s.startsWith("ROUTING"))
                        .findFirst()
                        .map(s -> CaseContextIntent.ROUTE)
                        .orElseGet(() -> plan.steps().stream()
                                .map(ExecutionPlan.Step::stepType)
                                .filter(s -> s.startsWith("ANALYSIS"))
                                .findFirst()
                                .map(s -> CaseContextIntent.ANALYZE)
                                .orElse(CaseContextIntent.CHAT_AUTO)));
    }
}