package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.llm.service.AgentQuestionService;
import com.berdachuk.medexpertmatch.llm.service.CaseIntakeClarificationService;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CaseIntakeClarificationServiceImpl implements CaseIntakeClarificationService {

    private static final String KEY_URGENCY = "urgencyLevel";
    private static final String KEY_CASE_TYPE = "caseType";
    private static final String KEY_PATIENT_AGE = "patientAge";

    private final AgentQuestionService agentQuestionService;

    public CaseIntakeClarificationServiceImpl(AgentQuestionService agentQuestionService) {
        this.agentQuestionService = agentQuestionService;
    }

    @Override
    public boolean needsClarification(Map<String, Object> request) {
        return isBlank(request, KEY_URGENCY)
                || isBlank(request, KEY_CASE_TYPE)
                || request.get(KEY_PATIENT_AGE) == null;
    }

    @Override
    public Map<String, String> resolveMissingFields(
            String sessionId,
            Map<String, Object> request,
            AskUserQuestionTool.QuestionHandler handlerOverride) {
        List<AskUserQuestionTool.Question> questions = buildQuestions(request);
        if (questions.isEmpty()) {
            return Map.of();
        }
        AskUserQuestionTool.QuestionHandler handler = handlerOverride != null
                ? handlerOverride
                : qs -> agentQuestionService.resolveQuestions(sessionId, qs);

        Map<String, String>[] answersHolder = new Map[1];
        AskUserQuestionTool tool = AskUserQuestionTool.builder()
                .answersValidation(false)
                .questionHandler(qs -> {
                    answersHolder[0] = handler.handle(qs);
                    Map<String, String> toolAnswers = new HashMap<>();
                    for (AskUserQuestionTool.Question question : qs) {
                        String answer = answersHolder[0].get(question.header());
                        if (answer == null) {
                            answer = answersHolder[0].get(question.question());
                        }
                        if (answer != null) {
                            toolAnswers.put(question.question(), answer);
                        }
                    }
                    return toolAnswers;
                })
                .build();
        tool.askUserQuestion(questions, Map.of());
        return answersHolder[0] != null ? answersHolder[0] : Map.of();
    }

    @Override
    public Map<String, Object> mergeAnswers(Map<String, Object> request, Map<String, String> answers) {
        Map<String, Object> merged = new HashMap<>(request);
        if (answers == null || answers.isEmpty()) {
            return merged;
        }
        applyAnswer(merged, answers, "urgency", KEY_URGENCY);
        applyAnswer(merged, answers, "caseType", KEY_CASE_TYPE);
        applyAnswer(merged, answers, "patientAge", KEY_PATIENT_AGE);
        return merged;
    }

    @Override
    public List<AskUserQuestionTool.Question> buildQuestions(Map<String, Object> request) {
        List<AskUserQuestionTool.Question> questions = new ArrayList<>();
        if (isBlank(request, KEY_URGENCY)) {
            questions.add(new AskUserQuestionTool.Question(
                    "How urgent is this case?",
                    "urgency",
                    List.of(
                            new AskUserQuestionTool.Question.Option("LOW", "Low urgency"),
                            new AskUserQuestionTool.Question.Option("MEDIUM", "Medium urgency"),
                            new AskUserQuestionTool.Question.Option("HIGH", "High urgency"),
                            new AskUserQuestionTool.Question.Option("CRITICAL", "Critical / life-threatening")),
                    false));
        }
        if (isBlank(request, KEY_CASE_TYPE)) {
            questions.add(new AskUserQuestionTool.Question(
                    "What type of care setting applies?",
                    "caseType",
                    List.of(
                            new AskUserQuestionTool.Question.Option("INPATIENT", "Inpatient"),
                            new AskUserQuestionTool.Question.Option("OUTPATIENT", "Outpatient"),
                            new AskUserQuestionTool.Question.Option("EMERGENCY", "Emergency")),
                    false));
        }
        if (request.get(KEY_PATIENT_AGE) == null) {
            questions.add(new AskUserQuestionTool.Question(
                    "What is the patient's age (years)?",
                    "patientAge",
                    List.of(
                            new AskUserQuestionTool.Question.Option("18-40", "18 to 40"),
                            new AskUserQuestionTool.Question.Option("41-65", "41 to 65"),
                            new AskUserQuestionTool.Question.Option("66+", "66 or older")),
                    false));
        }
        return questions;
    }

    private void applyAnswer(Map<String, Object> target, Map<String, String> answers, String answerKey, String fieldKey) {
        String value = answers.get(answerKey);
        if (value == null || value.isBlank()) {
            return;
        }
        if (KEY_PATIENT_AGE.equals(fieldKey)) {
            target.put(fieldKey, parsePatientAge(value));
        } else {
            target.put(fieldKey, value);
        }
    }

    private Integer parsePatientAge(String value) {
        if (value.contains("-")) {
            String first = value.split("-")[0].replace("+", "").trim();
            try {
                return Integer.parseInt(first);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (value.endsWith("+")) {
            try {
                return Integer.parseInt(value.replace("+", "").trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.debug("Could not parse patient age answer: {}", value);
            return null;
        }
    }

    private boolean isBlank(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        return false;
    }
}
