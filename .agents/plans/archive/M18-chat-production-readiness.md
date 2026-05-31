# M18: Chat Production Readiness & Observability — ✅ Complete

M17 delivered rich SSE activity events, activity panel collapse summary, JDBC session compaction IT, chat UX hardening ITs, and A2A JSON-RPC contract/error tests. M18 focused on production readiness: server-side markdown safety, chat metrics, E2E smoke coverage, and A2A AgentCard discovery.

**Prerequisite:** M17 complete (see `.agents/plans/archive/M17-chat-agent-polish-and-a2a-hardening.md`).

## Delivered

| # | Deliverable | Status |
|---|---|---|
| 1 | Server-side markdown allowlist for SSR assistant messages (W-02) | ✅ |
| 2 | Chat turn metrics (`chat.turn.duration`, `chat.turn.tool_calls`, `chat.stream.errors`) | ✅ |
| 3 | A2A AgentCard `/.well-known/agent.json` alias + JSON-RPC endpoint in card | ✅ |
| 4 | Chat E2E smoke IT (`ChatE2ESmokeIT`) — page assets + SSE lifecycle | ✅ |
| 5 | Session compaction observability (hashed logs + `sessionCompaction` health) | ✅ |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | ⏭ Skipped — pom approval not granted |

## Key files

- `web/service/ChatMarkdownRenderer.java`, `ChatMarkdownRendererImpl.java`, `web/domain/ChatMessageDisplay.java`
- `web/controller/ChatWebController.java` — pre-renders assistant history for SSR
- `chat/service/ChatTurnMetrics.java` — Micrometer timers/counters
- `llm/config/ObservingCompactionStrategy.java`, `SessionCompactionObservability.java`, `SessionCompactionHealthIndicator.java`
- `llm/rest/AgentCardController.java` — `/.well-known/agent.json` alias
- `llm/service/impl/AgentCardServiceImpl.java` — `endpoints.jsonrpc`
- Tests: `ChatMarkdownRendererTest`, `ChatTurnMetricsTest`, `A2aAgentCardIT`, `ChatE2ESmokeIT`, `SessionCompactionObservabilityTest`

## Notes

- SSR markdown uses a lightweight allowlist renderer (no new pom deps); client `marked` + DOMPurify remains for streaming.
- Playwright/browser automation deferred to M19; MockMvc smoke IT covers send/stream lifecycle in CI.
- Step 6 skipped per AGENTS.md pom approval requirement.

## Next

**`.agents/plans/M19-chat-ops-and-interop.md`**
