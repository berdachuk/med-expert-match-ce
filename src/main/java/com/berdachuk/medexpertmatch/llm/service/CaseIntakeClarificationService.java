package com.berdachuk.medexpertmatch.llm.service;

import org.springaicommunity.agent.tools.AskUserQuestionTool;

import java.util.List;
import java.util.Map;

/**
 * Resolves missing intake fields via structured clarification questions.
 */
public interface CaseIntakeClarificationService {

    boolean needsClarification(Map<String, Object> request);

    Map<String, String> resolveMissingFields(
            String sessionId,
            Map<String, Object> request,
            AskUserQuestionTool.QuestionHandler handlerOverride);

    Map<String, Object> mergeAnswers(Map<String, Object> request, Map<String, String> answers);

    List<AskUserQuestionTool.Question> buildQuestions(Map<String, Object> request);
}
