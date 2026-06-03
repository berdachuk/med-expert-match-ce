# M43: Pipeline Security, Health, and Live Stage Tracking

## Problem

M42 implemented event pipeline observability (metrics, dead letter queue, SSE progress), but three gaps remain: (1) the dead letter admin endpoints lack `AdminAccessGuard` security, (2) pipeline stage events are only sent as a batch after completion rather than streamed live with `in_progress` status, and (3) there is no dedicated health indicator exposing dead letter queue size and pipeline processing health.

## Goals

| # | Deliverable | Description | Effort |
|---|-------------|-------------|--------|
| 1 | **Secure dead letter admin endpoints** | Add `AdminAccessGuard.requireAdmin()` to `DeadLetterEventAdminController` | 1h |
| 2 | **Live pipeline stage tracking** | Emit stage events with `in_progress` status when each agent starts, not just `completed` at the end | 4h |
| 3 | **Pipeline health indicator** | Create `PipelineHealthIndicator` exposing dead letter queue size, stage failure rates, last pipeline completion time | 3h |
| 4 | **Backend tests for security + health** | Unit test `DeadLetterEventAdminController` access guard, unit test `PipelineHealthIndicator` | 2h |

**Total effort: ~10h**

## D1: Secure Dead Letter Admin Endpoints

In `DeadLetterEventAdminController.java`, inject `AdminAccessGuard` and call `requireAdmin()` on each endpoint:

```java
@Tag(name = "Admin", description = "Simulated admin APIs (requires X-User-Id: admin)")
@RestController
@RequestMapping("/api/v1/admin")
public class DeadLetterEventAdminController {

    private final EventDeadLetterQueue deadLetterQueue;
    private final EventRetryService eventRetryService;
    private final AdminAccessGuard adminAccessGuard;

    public DeadLetterEventAdminController(EventDeadLetterQueue deadLetterQueue, EventRetryService eventRetryService,
                                          AdminAccessGuard adminAccessGuard) {
        this.deadLetterQueue = deadLetterQueue;
        this.eventRetryService = eventRetryService;
        this.adminAccessGuard = adminAccessGuard;
    }

    @Operation(summary = "List all dead letter events")
    @GetMapping("/dead-letter-events")
    public ResponseEntity<List<DeadLetterEvent>> listDeadLetters() {
        adminAccessGuard.requireAdmin();
        return ResponseEntity.ok(deadLetterQueue.listAll());
    }

    @Operation(summary = "Replay a dead letter event")
    @PostMapping("/dead-letter-events/{id}/replay")
    public ResponseEntity<Map<String, String>> replayDeadLetter(@PathVariable String id) {
        adminAccessGuard.requireAdmin();
        eventRetryService.replayDeadLetter(id);
        return ResponseEntity.ok(Map.of("status", "replayed", "id", id));
    }
}
```

## D2: Live Pipeline Stage Tracking

### 2a: Add `recordStageInProgress` to PipelineMetricsService

```java
public void recordStageInProgress(String sessionId, String agentName) {
    meterRegistry.counter("pipeline.stage.in_progress", "agent", agentName).increment();
}
```

### 2b: Publish stage events at agent start

Modify the agent event handlers to publish `pipeline_stage` with `in_progress` status at the beginning of execution. Use `PipelineProgressCollector.addStage()`:

- In `PlannerAgent.onGoalIdentified()`: before building the plan, call `pipelineProgressCollector.addStage(sessionId, "PLANNING", "PlannerAgent", "in_progress")`
- In `ContextBuilderAgent.onPlanReady()`: before building context, add stage `"CONTEXT_BUILD" / "in_progress"`
- In `ExecutionAgent.onContextReady()`: before executing, add stage `"EXECUTION" / "in_progress"`
- In `CriticAgent.onResultsReady()`: before verifying, add stage `"CRITIC" / "in_progress"`

Inject `PipelineProgressCollector` into each agent. The collector already has a public `addStage()` method.

### 2c: Stream live pipeline stage events in ChatAssistantServiceImpl

Modify `ChatAssistantServiceImpl.sendPipelineStageEvents()` to also stream `in_progress` stage events immediately when the pipeline is triggered (after the goal classification in `streamMessage()`):

```java
// After classifying the goal, emit initial pipeline stage
if (goal.isRoutableToEngine() && goal.hasCaseId()) {
    var stages = pipelineProgressCollector.drainStages(ctx.sessionId());
    // The agents themselves will add in_progress stages as they start
}
```

To enable real-time streaming, the existing `pipeline_stage` events in `chat.js` already handle any status string - `in_progress` will render with the spinner/play icon via the `statusIcon` logic at line 392.

## D3: Pipeline Health Indicator

Create `PipelineHealthIndicator` in `llm/health/` implementing `HealthIndicator`:

```java
@Component
public class PipelineHealthIndicator implements HealthIndicator {

    private final MeterRegistry meterRegistry;
    private final EventDeadLetterQueue deadLetterQueue;

    public PipelineHealthIndicator(MeterRegistry meterRegistry, EventDeadLetterQueue deadLetterQueue) {
        this.meterRegistry = meterRegistry;
        this.deadLetterQueue = deadLetterQueue;
    }

    @Override
    public Health health() {
        int dlqSize = deadLetterQueue.size();
        double failureRate = 0;
        try {
            double completed = meterRegistry.get("pipeline.stage.completed").counter().count();
            double failed = meterRegistry.get("pipeline.stage.failed").counter().count();
            double total = completed + failed;
            failureRate = total > 0 ? failed / total : 0;
        } catch (Exception ignored) {}

        var detail = new LinkedHashMap<String, Object>();
        detail.put("deadLetterQueueSize", dlqSize);
        detail.put("stageFailureRate", failureRate);

        if (dlqSize > 50 || failureRate > 0.5) {
            return Health.down().withDetails(detail).build();
        }
        if (dlqSize > 0 || failureRate > 0.1) {
            return Health.status("WARN").withDetails(detail).build();
        }
        return Health.up().withDetails(detail).build();
    }
}
```

## D4: Backend Tests

### DeadLetterEventAdminController Security Test

Create `DeadLetterEventAdminControllerTest` that verifies endpoints are guarded. Mock `AdminAccessGuard` and verify `requireAdmin()` is called:

```java
class DeadLetterEventAdminControllerTest {

    @Test
    @DisplayName("listDeadLetters requires admin access")
    void listRequiresAdmin() {
        var guard = mock(AdminAccessGuard.class);
        var controller = new DeadLetterEventAdminController(
                mock(EventDeadLetterQueue.class), mock(EventRetryService.class), guard);
        controller.listDeadLetters();
        verify(guard).requireAdmin();
    }

    @Test
    @DisplayName("replayDeadLetter requires admin access")
    void replayRequiresAdmin() {
        var guard = mock(AdminAccessGuard.class);
        var retry = mock(EventRetryService.class);
        var controller = new DeadLetterEventAdminController(
                mock(EventDeadLetterQueue.class), retry, guard);
        controller.replayDeadLetter("dlq-1");
        verify(guard).requireAdmin();
    }
}
```

### PipelineHealthIndicator Test

Create `PipelineHealthIndicatorTest` that verifies UP, WARN, and DOWN health states based on DLQ size and failure rate.

## Files Changed

| File | Change |
|------|--------|
| `llm/event/DeadLetterEventAdminController.java` | D1 — add AdminAccessGuard |
| `llm/metrics/PipelineMetricsService.java` | D2a — add recordStageInProgress |
| `llm/agent/PlannerAgent.java` | D2b — inject PipelineProgressCollector, emit in_progress |
| `llm/agent/ContextBuilderAgent.java` | D2b — inject PipelineProgressCollector, emit in_progress |
| `llm/agent/ExecutionAgent.java` | D2b — inject PipelineProgressCollector, emit in_progress |
| `llm/agent/CriticAgent.java` | D2b — inject PipelineProgressCollector, emit in_progress |
| `llm/health/PipelineHealthIndicator.java` | D3 — new health indicator |
| `llm/event/DeadLetterEventAdminControllerTest.java` | D4 — new |
| `llm/health/PipelineHealthIndicatorTest.java` | D4 — new |

## Acceptance Criteria

- `GET /api/v1/admin/dead-letter-events` requires `X-User-Id: admin`
- `POST /api/v1/admin/dead-letter-events/{id}/replay` requires `X-User-Id: admin`
- Pipeline stage events show `in_progress` status during agent execution in the chat SSE stream
- `GET /actuator/health` includes `pipeline` health indicator with DLQ size and failure rate
- All unit tests pass