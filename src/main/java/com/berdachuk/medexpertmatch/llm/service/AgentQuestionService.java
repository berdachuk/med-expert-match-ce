package com.berdachuk.medexpertmatch.llm.service;

import org.springaicommunity.agent.tools.AskUserQuestionTool;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Bridges {@link AskUserQuestionTool} questions to the web UI via async resolution.
 */
public interface AgentQuestionService {

    Map<String, String> resolveQuestions(String sessionId, List<AskUserQuestionTool.Question> questions);

    Optional<PendingAgentQuestions> getPending(String sessionId);

    void submitAnswers(String sessionId, Map<String, String> answers);

    record PendingAgentQuestions(
            String sessionId,
            List<AskUserQuestionTool.Question> questions
    ) {
    }
}
