package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.llm.service.AgentQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Agent Questions", description = "Interactive intake clarification (AskUserQuestion)")
@RestController
@RequestMapping("/api/v1/agent/questions")
public class AgentQuestionController {

    private final AgentQuestionService agentQuestionService;

    public AgentQuestionController(AgentQuestionService agentQuestionService) {
        this.agentQuestionService = agentQuestionService;
    }

    @Operation(summary = "Get pending clarification questions for a session")
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> pending(@RequestParam String sessionId) {
        return agentQuestionService.getPending(sessionId)
                .map(pending -> ResponseEntity.ok(Map.of(
                        "sessionId", pending.sessionId(),
                        "questions", pending.questions().stream()
                                .map(q -> Map.of(
                                        "header", q.header(),
                                        "question", q.question(),
                                        "multiSelect", q.multiSelect()))
                                .toList())))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Submit answers to pending clarification questions")
    @PostMapping("/answer")
    public ResponseEntity<Map<String, String>> answer(
            @RequestParam String sessionId,
            @RequestBody Map<String, String> answers) {
        agentQuestionService.submitAnswers(sessionId, answers);
        return ResponseEntity.ok(Map.of("status", "accepted"));
    }
}
