package com.berdachuk.medexpertmatch.llm.service;

import com.berdachuk.medexpertmatch.llm.service.impl.AgentTodoTrackingServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springaicommunity.agent.tools.TodoWriteTool;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies TodoWrite plan tracking: todo lists are stored, events published, and at most one
 * task is {@code in_progress} at a time (Part 3 invariant).
 */
@ExtendWith(MockitoExtension.class)
class RecommendationTodoTrackingTest {

    @Test
    @DisplayName("handleTodos stores latest list and publishes AgentTodoUpdateEvent")
    void storesLatestAndPublishesEvent() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        AgentTodoTrackingServiceImpl service = new AgentTodoTrackingServiceImpl(publisher);
        TodoWriteTool.Todos todos = samplePlanWithSingleInProgress();

        service.handleTodos(todos);

        assertTrue(service.latestTodos().isPresent());
        assertEquals(3, service.latestTodos().orElseThrow().todos().size());
        ArgumentCaptor<AgentTodoUpdateEvent> captor = ArgumentCaptor.forClass(AgentTodoUpdateEvent.class);
        verify(publisher).publishEvent(captor.capture());
        assertEquals(3, captor.getValue().todos().todos().size());
    }

    @Test
    @DisplayName("valid plan has at most one in_progress task")
    void atMostOneInProgress() {
        TodoWriteTool.Todos valid = samplePlanWithSingleInProgress();
        assertEquals(1, AgentTodoTrackingService.countInProgress(valid));
    }

    @Test
    @DisplayName("TodoWriteTool forwards updates to the configured handler")
    void todoWriteToolInvokesHandler() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        AgentTodoTrackingServiceImpl service = new AgentTodoTrackingServiceImpl(publisher);
        TodoWriteTool tool = TodoWriteTool.builder()
                .todoEventHandler(service::handleTodos)
                .build();

        tool.todoWrite(samplePlanWithSingleInProgress());

        assertTrue(service.latestTodos().isPresent());
        verify(publisher).publishEvent(org.mockito.ArgumentMatchers.any(AgentTodoUpdateEvent.class));
    }

    private static TodoWriteTool.Todos samplePlanWithSingleInProgress() {
        return new TodoWriteTool.Todos(List.of(
                new TodoWriteTool.Todos.TodoItem(
                        "Analyze case",
                        TodoWriteTool.Todos.Status.completed,
                        "Analyzing case"),
                new TodoWriteTool.Todos.TodoItem(
                        "Retrieve evidence",
                        TodoWriteTool.Todos.Status.in_progress,
                        "Retrieving evidence"),
                new TodoWriteTool.Todos.TodoItem(
                        "Match specialists",
                        TodoWriteTool.Todos.Status.pending,
                        "Matching specialists")));
    }
}
