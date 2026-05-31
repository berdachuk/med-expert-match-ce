package com.berdachuk.medexpertmatch.llm.rest;

import com.berdachuk.medexpertmatch.llm.service.AgentTodoTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Agent Todos", description = "Multi-step agent plan tracking (TodoWrite)")
@RestController
@RequestMapping("/api/v1/agent/todos")
public class AgentTodoController {

    private final AgentTodoTrackingService todoTrackingService;

    public AgentTodoController(AgentTodoTrackingService todoTrackingService) {
        this.todoTrackingService = todoTrackingService;
    }

    @Operation(summary = "Get latest agent todo list")
    @GetMapping("/latest")
    public ResponseEntity<Map<String, Object>> latestTodos() {
        return todoTrackingService.latestTodos()
                .map(todos -> ResponseEntity.ok(Map.of(
                        "todos", mapTodos(todos),
                        "inProgressCount", AgentTodoTrackingService.countInProgress(todos))))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private static List<Map<String, Object>> mapTodos(TodoWriteTool.Todos todos) {
        return todos.todos().stream()
                .map(item -> Map.<String, Object>of(
                        "content", item.content(),
                        "status", item.status().name(),
                        "activeForm", item.activeForm() != null ? item.activeForm() : ""))
                .toList();
    }
}
