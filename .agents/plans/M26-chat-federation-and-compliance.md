# M26: Chat Federation & Compliance

M25 delivered scoped rate-limit buckets, admin dashboard hub, lifecycle audit feedback, Playwright admin smoke, and the chat ops runbook. M26 extends federation contracts and compliance automation.

**Prerequisite:** M25 complete (see `.agents/plans/archive/M25-chat-platform-hardening.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | A2A agent card endpoint (`GET /.well-known/agent.json`) | `feat/a2a-agent-card` | ⬜ Planned | 3h |
| 2 | Chat retention metrics + admin visibility | `feat/retention-metrics` | ⬜ Planned | 3h |
| 3 | Export bundle JSON schema validation IT | `feat/export-schema-it` | ⬜ Planned | 2h |
| 4 | Admin runbook link in dashboard + Grafana runbook panel | `feat/ops-dashboard-polish` | ⬜ Planned | 2h |
| 5 | Rate-limit scope metrics (`CHAT_SSE` vs `A2A` labels) | `feat/scope-metrics` | ⬜ Planned | 3h |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | `feat/a2a-autoconfigure` | ⬜ Blocked — pom approval | 4h |

**Total effort: ~13h (+4h optional)**

---

## Step 1: A2A agent card

Publish well-known agent metadata (skills URL, stream/jsonrpc endpoints, rate-limit hints).

---

## Step 2: Retention metrics

Micrometer counters for purged chats/messages; admin dashboard card with last run stats.

---

## Step 3: Export schema validation

Contract IT asserting export bundle JSON shape matches OpenAPI components.

---

## Step 4: Ops dashboard polish

Link runbook from admin hub; Grafana annotation panel pointing to `docs/chat-ops-runbook.md`.

---

## Step 5: Scope metrics

Tag `chat.rate.limited` with `scope` label (`CHAT_SSE`, `A2A`).

---

## Step 6: Optional A2A autoconfigure

**Requires explicit human approval** to add `spring-ai-a2a-server-autoconfigure` to `pom.xml`.

---

## References

- `.agents/plans/archive/M25-chat-platform-hardening.md`
- `RateLimitScope.java`, `docs/chat-ops-runbook.md`, `A2aJsonRpcController.java`
