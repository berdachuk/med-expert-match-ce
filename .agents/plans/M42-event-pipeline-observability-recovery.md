# M42: Event Pipeline Observability, Recovery, and UI Integration

## Problem

M39 wired the event-driven agents into the chat pipeline, but there is no observability into pipeline execution (no per-stage metrics, no failure tracking), no recovery mechanism for failed agent steps (no dead letter handling), and the inline agent activity panel from M40 is not yet wired to show event pipeline stages.

## Goals

| # | Deliverable | Description | Effort |
|---|-------------|-------------|--------|
| 1 | **Event pipeline metrics** | Track per-agent stage duration, success/failure rates via Micrometer counters/timers | 6h |
| 2 | **Dead letter event handling** | Capture unhandled/failed events to a dead letter queue for replay or alerting | 6h |
| 3 | **Pipeline progress via SSE** | Stream agent pipeline stage events (plan → context → execute → critic → done) to the chat SSE stream | 6h |
| 4 | **Inline panel shows pipeline stages** | Wire the M40 inline activity panel to display real-time pipeline stage names and status | 4h |
| 5 | **Integration test with metrics** | Verify metrics are incremented on pipeline success/failure end-to-end | 3h |

**Total effort: ~25h**

## D1: Event Pipeline Metrics

Create a `PipelineMetricsService` that records Micrometer metrics for each agent stage:

```java
@Component
public class PipelineMetricsService {

    private final MeterRegistry meterRegistry;

    public PipelineMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordStageStarted(String sessionId, String agentName) {
        meterRegistry.counter("pipeline.stage.started", "agent", agentName).increment();
    }

    public void recordStageCompleted(String sessionId, String agentName, long durationMs) {
        meterRegistry.counter("pipeline.stage.completed", "agent", agentName).increment();
        meterRegistry.timer("pipeline.stage.duration", "agent", agentName)
                .record(Duration.ofMillis(durationMs));
    }

    public void recordStageFailed(String sessionId, String agentName, String reason) {
        meterRegistry.counter("pipeline.stage.failed", "agent", agentName, "reason", reason).increment();
    }

    public void recordPipelineCompleted(String sessionId, boolean success) {
        meterRegistry.counter("pipeline.completed", "status", success ? "success" : "failure").increment();
    }
}
```

Inject `PipelineMetricsService` into all 4 event agents. Each agent calls `recordStageStarted` on entry and `recordStageCompleted` or `recordStageFailed` on exit.

## D2: Dead Letter Event Handling

Create `EventDeadLetterQueue` and `EventRetryService`:

- `EventDeadLetterQueue` — stores failed events (after 3 retries) in a `ConcurrentLinkedQueue` with metadata (sessionId, agent, error, timestamp)
- `EventRetryService` — wraps agent `@EventListener` methods with retry logic (max 3 attempts with exponential backoff); on final failure, publishes to dead letter queue
- Add admin REST endpoint `GET /api/v1/admin/dead-letter-events` to list dead letters
- Add admin REST endpoint `POST /api/v1/admin/dead-letter-events/{id}/replay` to replay a dead letter

## D3: Pipeline Progress via SSE

In `ChatAssistantServiceImpl.streamMessage()`, after the event pipeline completes via `DoneEvent`, send additional SSE events showing each pipeline stage:

- `event: pipeline_stage` with payload `{"stage": "PLANNING", "status": "completed", "agent": "PlannerAgent", "durationMs": 1200}`
- `event: pipeline_stage` with payload `{"stage": "CONTEXT_BUILD", "status": "completed", "agent": "ContextBuilderAgent", "durationMs": 800}`
- etc.

The agent pipeline stages are already published as events (`PlanReadyEvent`, `ContextReadyEvent`, `ResultsReadyEvent`, `DoneEvent`). Create a `PipelineProgressCollector` that collects stage events per session and makes them available as SSE payloads.

## D4: Inline Panel Shows Pipeline Stages

In `chat.js`, modify the inline agent activity panel rendering to handle `pipeline_stage` events:

- Add a new SSE event handler for `pipeline_stage`
- Render each stage as a row in the inline panel with a status badge (spinner for in-progress, checkmark for done, X for failed)
- Use the same `addActivityEntryToPanel` mechanism but with `kind='pipeline'` and distinct border color

In `chat.css`, add styles for pipeline stage entries:
```css
.agent-activity-entry.pipeline {
    border-left-color: #0d6efd;
}
.agent-activity-entry.pipeline.completed {
    border-left-color: #198754;
}
.agent-activity-entry.pipeline.failed {
    border-left-color: #dc3545;
}
```

## D5: Integration Test with Metrics

Create `EventPipelineMetricsIT`:

```java
@SpringBootTest
@ActiveProfiles("event-driven")
class EventPipelineMetricsIT {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("pipeline metrics are recorded on GoalIdentifiedEvent publish")
    void pipelineMetricsRecorded() {
        var goal = new GoalClassification(GoalType.MATCH_DOCTORS, Optional.of("case-metrics-1"), Optional.empty(), "test");
        var event = new GoalIdentifiedEvent("session-metrics-1", goal, "case-metrics-1", Instant.now());

        eventPublisher.publishEvent(event);

        // Wait for async event processing
        Thread.sleep(1000);

        assertTrue(meterRegistry.get("pipeline.stage.started").tag("agent", "PlannerAgent").counter().count() > 0);
    }
}
```

## Files Changed

| File | Change |
|------|--------|
| `llm/metrics/PipelineMetricsService.java` | New — Micrometer metrics for pipeline stages |
| `llm/agent/PlannerAgent.java` | Inject PipelineMetricsService, record stage metrics |
| `llm/agent/ContextBuilderAgent.java` | Inject PipelineMetricsService, record stage metrics |
| `llm/agent/ExecutionAgent.java` | Inject PipelineMetricsService, record stage metrics |
| `llm/agent/CriticAgent.java` | Inject PipelineMetricsService, record stage metrics |
| `llm/event/EventDeadLetterQueue.java` | New — dead letter storage |
| `llm/event/EventRetryService.java` | New — retry with backoff |
| `llm/rest/DeadLetterEventAdminController.java` | New — admin REST endpoints |
| `llm/service/PipelineProgressCollector.java` | New — collect stage events per session |
| `service/impl/ChatAssistantServiceImpl.java` | Send pipeline_stage SSE events |
| `static/js/chat.js` | Handle `pipeline_stage` SSE events in inline panel |
| `static/css/chat.css` | Styles for pipeline stage entries |
| `llm/agent/EventPipelineMetricsIT.java` | New — integration test |

## Acceptance Criteria

- Metrics counters are visible via `/actuator/metrics/pipeline.stage.started`
- Failed agent events go to dead letter queue after 3 retries
- Chat SSE stream includes `pipeline_stage` events during event-driven pipeline execution
- Inline agent activity panel shows pipeline stages with status badges
- Admin can list and replay dead letter events