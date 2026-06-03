package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.llm.agent.OrchestrationContextHolder;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassification;
import com.berdachuk.medexpertmatch.llm.chat.GoalClassifier;
import com.berdachuk.medexpertmatch.llm.event.GoalIdentifiedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class GoalIdentifiedEventPublisher {

    private final ApplicationEventPublisher eventPublisher;
    private final GoalClassifier goalClassifier;

    public GoalIdentifiedEventPublisher(ApplicationEventPublisher eventPublisher, GoalClassifier goalClassifier) {
        this.eventPublisher = eventPublisher;
        this.goalClassifier = goalClassifier;
    }

    public GoalClassification classifyAndPublish(String message) {
        GoalClassification result = goalClassifier.classify(message);
        if (result.isRoutableToEngine() && result.hasCaseId()) {
            String caseId = result.caseId().orElse("");
            eventPublisher.publishEvent(new GoalIdentifiedEvent(
                    OrchestrationContextHolder.sessionIdOrNull(),
                    result, caseId, Instant.now()));
        }
        return result;
    }
}