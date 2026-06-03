# M39: Wire Event-Driven Agents into Chat Pipeline

Connects the event-driven agents (PlannerAgent, ContextBuilderAgent, ExecutionAgent, CriticAgent) from M38 into the actual chat/classification flow. The `GoalClassifier` and `MedicalAgentService` currently use synchronous `DoctorMatchWorkflowEngine`/`RoutingWorkflowEngine` directly — this phase makes the event pipeline the primary path with the sync path as fallback.

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|-------------|--------|--------|--------|
| 1 | Wire `GoalClassifier` to publish `GoalIdentifiedEvent` instead of returning classification directly | `feat/m39-wire-classifier` | ⬜ Planned | 4h |
| 2 | Add `GoalIdentifiedEventPublisher` service that bridges classification → event emission | `feat/m39-event-publisher` | ⬜ Planned | 3h |
| 3 | Make `PlannerAgent` persist plan + return synchronous fallback via `Future`/`CompletableFuture` | `feat/m39-planner-fallback` | ⬜ Planned | 6h |
| 4 | Add `AgentCoordinatorService` — publishes `GoalIdentifiedEvent` and subscribes to `DoneEvent` for sync response | `feat/m39-coordinator` | ⬜ Planned | 8h |
| 5 | Add `@Profile("event-driven")` condition on event agents; sync path remains default | `feat/m39-profile-gate` | ⬜ Planned | 2h |
| 6 | Archive M38 plan | `feat/m39-archive-m38` | ✅ Done | 0h |

**Total effort: ~23h**

## D1: Wire GoalClassifier

- `GoalClassifier.classify()` currently returns `GoalClassification` directly
- Add optional `ApplicationEventPublisher` injection
- After classification, call `eventPublisher.publishEvent(new GoalIdentifiedEvent(...))` when a routable goal is detected
- Keep the return value for backward compatibility

## D2: GoalIdentifiedEventPublisher

```java
@Service
public class GoalIdentifiedEventPublisher {
    private final ApplicationEventPublisher eventPublisher;
    private final GoalClassifier goalClassifier;

    public GoalClassification classifyAndPublish(String message) {
        GoalClassification result = goalClassifier.classify(message);
        if (result.isRoutableToEngine()) {
            String caseId = result.caseId().orElse("");
            eventPublisher.publishEvent(new GoalIdentifiedEvent(
                OrchestrationContextHolder.sessionIdOrNull(),
                result, caseId, Instant.now()));
        }
        return result;
    }
}
```

## D3: PlannerAgent Synchronous Fallback

- `PlannerAgent` stores `CompletableFuture<MedicalAgentService.AgentResponse>` per sessionId
- `CriticAgent` completes the future on `DoneEvent`
- `AgentCoordinatorService` blocks on the future for sync callers (with timeout)

## D4: AgentCoordinatorService

```java
@Service
public class AgentCoordinatorService {

    public MedicalAgentService.AgentResponse process(String message) {
        GoalClassification goal = classifyAndPublish(message);
        if (!goal.isRoutableToEngine()) {
            return handleNonRoutable(goal);
        }
        CompletableFuture<AgentResponse> future = new CompletableFuture<>();
        pendingFutures.put(sessionId, future);
        eventPublisher.publishEvent(new GoalIdentifiedEvent(...));
        return future.get(30, TimeUnit.SECONDS); // sync timeout
    }

    @EventListener
    void onDone(DoneEvent event) {
        CompletableFuture<AgentResponse> future = pendingFutures.remove(event.sessionId());
        if (future != null) future.complete(event.finalResponse());
    }
}
```

This replaces the direct `DoctorMatchWorkflowEngine.execute()` call in `MedicalAgentServiceImpl` with the event-driven pipeline.

## D5: Profile Gate

- All 4 event agents + `AgentCoordinatorService` = `@Profile("event-driven")`
- Default profile keeps existing synchronous `MedicalAgentServiceImpl` path
- Config: `spring.profiles.active=event-driven` to enable

## TDD

| Test | Type | What it covers |
|------|------|---------------|
| `GoalIdentifiedEventPublisherTest` | Unit | Classification + event emission |
| `PlannerFallbackTest` | Unit | CompletableFuture management |
| `AgentCoordinatorServiceTest` | Unit | Full sync flow via future + DoneEvent |
| `EventDrivenPipelineIT` | Integration | Profile-gated end-to-end: message → DoneEvent |

## Out of Scope

- Web UI for event pipeline visualization
- Event sourcing / event store persistence
- Distributed event bus (Kafka/RabbitMQ) — in-process only
- Horizontal scaling of event agents