package com.berdachuk.medexpertmatch.llm.service;

import org.springaicommunity.agent.tools.TodoWriteTool;

import java.util.Optional;

/**
 * Tracks multi-step agent todo lists emitted by {@link TodoWriteTool}.
 */
public interface AgentTodoTrackingService {

    void handleTodos(TodoWriteTool.Todos todos);

    Optional<TodoWriteTool.Todos> latestTodos();

    /**
     * Validates the todo invariant from Part 3: at most one task is {@code in_progress}.
     */
    static int countInProgress(TodoWriteTool.Todos todos) {
        if (todos == null || todos.todos() == null) {
            return 0;
        }
        return (int) todos.todos().stream()
                .filter(item -> item.status() == TodoWriteTool.Todos.Status.in_progress)
                .count();
    }
}
