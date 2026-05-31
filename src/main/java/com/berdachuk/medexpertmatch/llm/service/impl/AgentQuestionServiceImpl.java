package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.llm.service.AgentQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.agent.tools.AskUserQuestionTool;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class AgentQuestionServiceImpl implements AgentQuestionService {

    private static final long QUESTION_TIMEOUT_SECONDS = 120;

    private final Map<String, PendingRequest> pending = new ConcurrentHashMap<>();

    @Override
    public Map<String, String> resolveQuestions(String sessionId, List<AskUserQuestionTool.Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return Map.of();
        }
        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        pending.put(sessionId, new PendingRequest(questions, future));
        log.info("Waiting for user answers on session {} ({} question(s))", sessionId, questions.size());
        try {
            return future.get(QUESTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Timed out waiting for user answers on session {}", sessionId);
            pending.remove(sessionId);
            return Map.of();
        } catch (Exception e) {
            log.warn("Failed waiting for user answers on session {}: {}", sessionId, e.getMessage());
            pending.remove(sessionId);
            return Map.of();
        }
    }

    @Override
    public Optional<PendingAgentQuestions> getPending(String sessionId) {
        PendingRequest request = pending.get(sessionId);
        if (request == null) {
            return Optional.empty();
        }
        return Optional.of(new PendingAgentQuestions(sessionId, request.questions()));
    }

    @Override
    public void submitAnswers(String sessionId, Map<String, String> answers) {
        PendingRequest request = pending.remove(sessionId);
        if (request == null) {
            log.debug("No pending questions for session {}", sessionId);
            return;
        }
        Map<String, String> safeAnswers = answers != null ? new HashMap<>(answers) : Map.of();
        request.future().complete(safeAnswers);
    }

    private record PendingRequest(
            List<AskUserQuestionTool.Question> questions,
            CompletableFuture<Map<String, String>> future
    ) {
    }
}
