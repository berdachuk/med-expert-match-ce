package com.berdachuk.medexpertmatch.llm.service;

import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.context.ApplicationEvent;

/**
 * Published when the agent updates its multi-step plan via {@link TodoWriteTool}.
 */
public class AgentTodoUpdateEvent extends ApplicationEvent {

    private final TodoWriteTool.Todos todos;

    public AgentTodoUpdateEvent(Object source, TodoWriteTool.Todos todos) {
        super(source);
        this.todos = todos;
    }

    public TodoWriteTool.Todos todos() {
        return todos;
    }
}
