package com.berdachuk.medexpertmatch.llm.service.impl;

import com.berdachuk.medexpertmatch.llm.service.AgentTodoTrackingService;
import com.berdachuk.medexpertmatch.llm.service.AgentTodoUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class AgentTodoTrackingServiceImpl implements AgentTodoTrackingService {

    private final ApplicationEventPublisher eventPublisher;
    private final AtomicReference<TodoWriteTool.Todos> latest = new AtomicReference<>();

    public AgentTodoTrackingServiceImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void handleTodos(TodoWriteTool.Todos todos) {
        if (todos == null) {
            return;
        }
        latest.set(todos);
        int inProgress = AgentTodoTrackingService.countInProgress(todos);
        log.debug("Agent todo update: {} item(s), {} in_progress", todos.todos().size(), inProgress);
        eventPublisher.publishEvent(new AgentTodoUpdateEvent(this, todos));
    }

    @Override
    public Optional<TodoWriteTool.Todos> latestTodos() {
        return Optional.ofNullable(latest.get());
    }
}
