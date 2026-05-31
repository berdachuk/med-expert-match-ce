# M26: Chat Federation & Compliance — ✅ Complete

M25 delivered scoped rate-limit buckets, admin dashboard hub, lifecycle audit feedback, Playwright admin smoke, and the chat ops runbook. M26 extended federation contracts and compliance automation.

**Prerequisite:** M25 complete (see `.agents/plans/archive/M25-chat-platform-hardening.md`).

## Delivered

| # | Deliverable | Status |
|---|---|---|
| 1 | A2A agent card endpoint (`GET /.well-known/agent.json`) with stream/skills/rate-limit hints | ✅ |
| 2 | Chat retention metrics + admin visibility | ✅ |
| 3 | Export bundle JSON schema validation IT | ✅ |
| 4 | Admin runbook link in dashboard + Grafana runbook panel | ✅ |
| 5 | Rate-limit scope metrics (`CHAT_SSE` vs `A2A` labels) | ✅ |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | ⏭ Skipped — pom approval not granted |

## Key files

- `llm/service/impl/AgentCardServiceImpl.java` — endpoints, rateLimits, skills URL
- `chat/service/ChatRetentionMetrics.java` — purge counters + last-run snapshot
- `web/controller/AdminDashboardWebController.java`, `templates/admin/index.html` — retention card, runbook link
- `chat/rest/ChatExportBundleSchemaIT.java` — OpenAPI contract validation
- `chat/service/ChatTurnMetrics.java` — `scope` tag on `chat.rate.limited`
- `grafana/dashboard.json` — retention, scope, runbook panels
- `core/config/RateLimitingConfig.java` — exclude chat/A2A from global IP limiter (scoped buckets)

## Notes

- Agent card publishes jsonrpc, stream, skills endpoints and rate-limit window hints.
- Retention metrics: `chat.retention.runs`, `chat.retention.chats_purged`, `chat.retention.messages_purged`.
- Step 6 skipped per AGENTS.md pom approval requirement.

## Next

**`.agents/plans/M27-chat-observability-and-governance.md`**
