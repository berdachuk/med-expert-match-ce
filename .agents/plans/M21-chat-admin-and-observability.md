# M21: Chat Admin & Observability

M20 delivered tier-aware chat rate limits, export audit with hashed identifiers, A2A OpenAPI schemas, Retry-After headers, and optional Playwright navigation. M21 focuses on operability: session-token admin API, Grafana export/audit visibility, and chat retention hygiene.

**Prerequisite:** M20 complete (see `.agents/plans/archive/M20-chat-governance-and-a2a-contracts.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | Grafana panel for `chat.export.count` + rate-limit alert hints | `feat/chat-grafana-export` | ⬜ Planned | 2h |
| 2 | Admin REST for API session tokens (list/create/revoke, no key echo) | `feat/api-session-admin` | ⬜ Planned | 4h |
| 3 | Audit log query endpoint for `CHAT_EXPORT` (hashed ids only) | `feat/chat-export-audit-api` | ⬜ Planned | 3h |
| 4 | Chat retention job — purge idle chats older than configurable TTL | `feat/chat-retention` | ⬜ Planned | 4h |
| 5 | Wire `ApiKeyAuthFilter` to `ApiSessionTokenRepository` (optional profile) | `feat/session-token-auth` | ⬜ Planned | 3h |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | `feat/a2a-autoconfigure` | ⬜ Blocked — pom approval | 4h |

**Total effort: ~16h (+4h optional)**

---

## Step 1: Grafana export observability

Add dashboard panel for `rate(chat_export_count_total[5m])` alongside existing chat panels.

Document alert threshold suggestions for `chat.rate.limited` spikes.

---

## Step 2: API session token admin

`GET/POST/DELETE /api/v1/admin/session-tokens` (admin-gated via existing simulated admin or API key tier).

Never return full API keys after creation; show prefix + tier + expiry only.

---

## Step 3: Export audit API

`GET /api/v1/admin/audit/chat-exports` — paginated, hashed actor/resource ids, message counts only.

Integration test with `ChatExportAuditIT` pattern.

---

## Step 4: Chat retention

Configurable `chat.retention.idle-days` (default off in prod profile).

Scheduled purge of non-default chats with no activity; never log message content.

---

## Step 5: Session token auth profile

When `medexpertmatch.auth.session-tokens.enabled=true`, validate `X-API-Key` against DB tokens instead of static property list.

Preserve static-key mode for local/dev.

---

## Step 6: Optional A2A autoconfigure

**Requires explicit human approval** to add `spring-ai-a2a-server-autoconfigure` to `pom.xml`.

---

## References

- `.agents/plans/archive/M20-chat-governance-and-a2a-contracts.md`
- `ApiSessionTokenRepository.java`, `AuditLogRepository.java`
- `grafana/dashboard.json`
