# M44: Test Coverage for Core Infrastructure and Missing E2E Tests

## Problem

M41–M43 added pipeline security, health tracking, and SSE infrastructure, but several critical gaps remain: (1) `PipelineMetricsService.recordStageInProgress()` was added in M43 D2a but never wired into agents, (2) `LogStreamService` — the foundational SSE streaming component — has zero test coverage, (3) the M41 chat follow-up context fix lacks an end-to-end integration test (`ChatFollowUpContextIT`), and (4) two health indicators (`SessionCompactionHealthIndicator`, `GraphDatabaseHealthCheck`) have no dedicated tests.

## Goals

| # | Deliverable | Description | Effort |
|---|-------------|-------------|--------|
| 1 | **Wire `recordStageInProgress` into agents** | Call `pipelineMetrics.recordStageInProgress()` alongside `pipelineProgressCollector.addStage(... "in_progress")` in all 4 event-driven agents | 1h |
| 2 | **LogStreamService unit tests** | Test emitter lifecycle, concurrent access, thread-local cleanup, error/callbacks | 4h |
| 3 | **ChatFollowUpContextIT integration test** | End-to-end test verifying turn-1 to turn-2 follow-up context survival across all M41 fixes | 3h |
| 4 | **SessionCompactionHealthIndicator test** | Unit test for UP status, compaction count, failure count, last compaction time | 1h |
| 5 | **GraphDatabaseHealthCheck test** | Unit test for connectivity health check results | 1h |

**Total effort: ~10h**

## D1: Wire recordStageInProgress Into Agents

In each of the 4 event-driven agents, add a call to `pipelineMetrics.recordStageInProgress()` immediately after the `pipelineProgressCollector.addStage(... "in_progress")` call:

- **PlannerAgent.java** — in `onGoalIdentified()` after `addStage(..., "PLANNING", "PlannerAgent", "in_progress")` → `pipelineMetrics.recordStageInProgress(event.sessionId(), "PlannerAgent")`
- **ContextBuilderAgent.java** — in `onPlanReady()` after `addStage(..., "CONTEXT_BUILD", "ContextBuilderAgent", "in_progress")` → `pipelineMetrics.recordStageInProgress(event.sessionId(), "ContextBuilderAgent")`
- **ExecutionAgent.java** — in `onContextReady()` after `addStage(..., "EXECUTION", "ExecutionAgent", "in_progress")` → `pipelineMetrics.recordStageInProgress(event.sessionId(), "ExecutionAgent")`
- **CriticAgent.java** — in `onResultsReady()` after `addStage(..., "CRITIC", "CriticAgent", "in_progress")` → `pipelineMetrics.recordStageInProgress(event.sessionId(), "CriticAgent")`

## D2: LogStreamService Unit Tests

Create `LogStreamServiceTest` in `core/service/` test package. Cover:
- `register(sessionId, emitter)` stores emitter and sets current session
- `unregister(sessionId)` removes emitter and clears current session
- `sendLog(sessionId, level, source, message)` publishes event (verify via spy/mock)
- `clearCurrentSessionId()` cleans up ThreadLocal
- Concurrent register/unregister from multiple threads
- Error callback handling

## D3: ChatFollowUpContextIT Integration Test

Create `ChatFollowUpContextIT` in `llm/service/` test package. This IT should:
1. Create a chat session with turn 1 (message + assistant reply)
2. Send turn 2 with follow-up intent (classified as `follow-up:` prefix)
3. Verify the assistant reply references the previous conversation context
4. Verify no context loss: caseId, goal, history are all preserved

Use `@SpringBootTest` with mocked LLM responses.

## D4: SessionCompactionHealthIndicator Test

Create `SessionCompactionHealthIndicatorTest` following the same pattern as `PipelineHealthIndicatorTest`. Test:
- UP status when no failures
- Compaction count and failure count are reported in details
- Last compaction time is reported

## D5: GraphDatabaseHealthCheck Test

Create `GraphDatabaseHealthCheckTest`. Test:
- Health check returns expected status
- Connection details include relevant metadata

## Files Changed

| File | Change |
|------|--------|
| `llm/agent/PlannerAgent.java` | D1 — wire recordStageInProgress |
| `llm/agent/ContextBuilderAgent.java` | D1 — wire recordStageInProgress |
| `llm/agent/ExecutionAgent.java` | D1 — wire recordStageInProgress |
| `llm/agent/CriticAgent.java` | D1 — wire recordStageInProgress |
| `core/service/LogStreamServiceTest.java` | D2 — new |
| `llm/service/ChatFollowUpContextIT.java` | D3 — new |
| `llm/config/SessionCompactionHealthIndicatorTest.java` | D4 — new |
| `graph/health/GraphDatabaseHealthCheckTest.java` | D5 — new |

## Acceptance Criteria

- `pipeline.stage.in_progress` counter is incremented for each agent in the pipeline
- `LogStreamService` has 5+ unit tests covering emitter lifecycle, concurrency, cleanup
- `ChatFollowUpContextIT` verifies turn-1 → turn-2 context survival end-to-end
- `SessionCompactionHealthIndicatorTest` and `GraphDatabaseHealthCheckTest` pass
- All 449+ existing tests still pass