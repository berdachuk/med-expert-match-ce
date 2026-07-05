# Chat & A2A Operator Runbook

Operational guide for chat SSE, A2A federation, exports, retention, and audit. All identifiers in logs and audit APIs are hashed — never raw user or chat ids.

## Rate limiting

| Scope | Endpoint(s) | Bucket key | Default tier |
|-------|-------------|------------|--------------|
| Chat SSE | `POST /api/v1/chats/{id}/messages/stream` | `userId:TIER:CHAT_SSE` | 10/min |
| A2A | `POST /a2a/v1/jsonrpc`, `POST /a2a/v1/stream` | `userId:TIER:A2A` | 10/min (separate bucket) |

- **429 response** includes `Retry-After: 60` (seconds).
- Tier from `X-API-Key` session token: `DEFAULT`, `HIGH`, or `UNLIMITED`.
- Metrics: `chat_rate_limited_total{tier=..., scope=CHAT_SSE|A2A}`, `chat_turn_duration_seconds{tier=...}`.
- Retention metrics: `chat_retention_runs_total`, `chat_retention_chats_purged_total`, `chat_retention_messages_purged_total`.

## Chat retention

- Config: `chat.retention.idle-days` (default `0` = disabled).
- Scheduler purges idle **non-default** chats past the cutoff.
- Default chat is kept; messages may be soft-deleted via user data delete.
- Metrics: `chat_retention_runs_total`, `chat_retention_chats_purged_total`, `chat_retention_messages_purged_total`, `chat_retention_failures_total`.
- Admin API: `GET /api/v1/admin/chat-retention` (requires `X-User-Id: admin`).

## Exports & data lifecycle

| Action | API | Audit action |
|--------|-----|--------------|
| Single chat export | `GET /api/v1/chats/{id}/export` | `CHAT_EXPORT` |
| User bundle export | `GET /api/v1/chats/export-bundle` | `CHAT_EXPORT_BUNDLE` |
| Delete all user data | `DELETE /api/v1/chats/data` | `CHAT_DATA_DELETE` |

- Responses include `auditReferenceHash` (SHA-256 of audit log id) for UI feedback.
- Web UI: `/chat` sidebar — **Export All** / **Delete All Data**.

## Admin tools

Access with `?user=admin` (sets `medexpertmatch-user-id=admin` cookie).

| Page | URL |
|------|-----|
| Admin hub | `/admin?user=admin` |
| Session tokens | `/admin/session-tokens?user=admin` |
| Export audit | `/admin/chat-exports?user=admin` |

Audit API: `GET /api/v1/admin/audit/chat-exports?action=CHAT_EXPORT_BUNDLE` (optional filter).

## LLM harness metrics

Canonical harness documentation: [HARNESS.md](HARNESS.md). Tool-calling model: [FUNCTIONGEMMA.md](FUNCTIONGEMMA.md).

Micrometer counters (no PHI in labels):

| Metric | Meaning |
|--------|---------|
| `harness.verify.failure` | Post-tool verification failed (doctor/routing) |
| `harness.policy_gate.failure` | Policy gate rejected or policy violation |

Workflow logs emit `HARNESS_STATE` transitions on the active session id (doctor match, routing, case intake).

- Config: `medexpertmatch.llm.harness.*` in `application.yml`
- Human checkpoint: `POST /api/v1/workflows/{runId}/checkpoint` with `X-User-Id: admin` or `clinician`, body `{ "decision": "APPROVE"|"REJECT", "resumeToken": "..." }` (enable via `human-checkpoint-enabled`)
- Admin UI: `/admin/harness-runs?user=admin` lists `NEEDS_HUMAN` runs; `/admin/harness-chains?user=admin` traces analysis→match→recommend
- REST: `GET /api/v1/workflows/runs?state=NEEDS_HUMAN` (admin/clinician)
- Analysis→match handoff: `chain-analysis-to-match`; match→recommend: `chain-match-to-recommend`
- Eval gate: `scripts/run-eval-harness.sh` (CI after unit tests)
- Harness backlog template: `.agents/templates/harness-backlog-item.md`

## Observability

- Dashboard: `grafana/dashboard.json`
- Alerts: `grafana/chat-alerts.yml`, hints in `docs/grafana-chat-alerts.md`
- Key panels: tier rate limits, turn duration by tier, export rate

### Composable tool calling & structured output (RISK-137..140)

Micrometer metrics for Spring AI composable tool calling and structured-output risks. No PHI in labels.

| Metric | Tags | Risk | Meaning |
|--------|------|------|---------|
| `llm.structured-output.validation.retry` | `operation`, `client_type`, `attempt` | RISK-137 | `validateSchema()` re-issued a call after schema mismatch |
| `llm.structured-output.validation.failure` | `operation`, `client_type` | RISK-137 | All validation retries exhausted |
| `llm.tool-search.fallback.total` | `requested_index`, `resolved_index` | RISK-138 | Tool search index fell back (e.g. vector → regex) |
| `llm.tokens.total` | `client_type`, `tier`, `goal_type`, `direction` | RISK-139 | Token volume; watch utility/tool-calling tier after `AugmentedToolCallbackProvider` inner-thought args |
| `session.compaction.total` | — | RISK-140 | Session memory compactions (tool loop transcript growth) |
| `session.compaction.events_removed` | — | RISK-140 | Events removed per compaction |
| `session.events.count` | `session_hash` (sampled) | RISK-140 | Remaining events after compaction on sampled sessions |

**Prometheus queries (examples):**

- Schema retry rate: `rate(llm_structured_output_validation_retry_total[5m])`
- Tool-search fallback: `increase(llm_tool_search_fallback_total[1h])`
- Inner-thought token drift: compare `llm_tokens_total{client_type="UTILITY"}` before/after deploy
- Session growth: `session_compaction_events_removed_sum / session_compaction_total`

Structured-output counters are emitted from M140 call sites; tool-search fallback from `ToolSearchToolCallingAdvisor` wiring when vector index is unavailable.

## PHI safety

- Never log message content, patient identifiers, or raw chat/user ids.
- Exports run through `PhiGuard` redaction before persistence or download.
- Audit stores hashed resource and actor ids only.

## Local verification

```bash
mvn verify
mvn test -Pplaywright -Dplaywright.enabled=true   # optional browser smoke
```
