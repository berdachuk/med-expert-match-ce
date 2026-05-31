# M23: Chat Security & A2A Governance

M22 delivered A2A skill registry, GDPR-style chat data lifecycle, Grafana alert rules, admin session-token UI, and tier-tagged chat metrics. M23 hardens security boundaries and operational governance.

**Prerequisite:** M22 complete (see `.agents/plans/archive/M22-a2a-federation-and-chat-lifecycle.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | A2A stream rate limiting + tier-aware limits (parity with chat SSE) | `feat/a2a-rate-limits` | ⬜ Planned | 3h |
| 2 | Chat export bundle audit action + admin filter | `feat/export-bundle-audit` | ⬜ Planned | 2h |
| 3 | Session-token UI: wire `X-User-Id` via cookie for admin fetch | `feat/admin-cookie-auth` | ⬜ Planned | 2h |
| 4 | Grafana dashboard panels for tier-tagged rate limits | `feat/grafana-tier-panels` | ⬜ Planned | 2h |
| 5 | Chat data lifecycle OpenAPI + contract IT | `feat/chat-lifecycle-openapi` | ⬜ Planned | 2h |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | `feat/a2a-autoconfigure` | ⬜ Blocked — pom approval | 4h |

**Total effort: ~11h (+4h optional)**

---

## Step 1: A2A rate limits

Apply `ChatRateLimitService` (or shared limiter) to `/a2a/v1/stream` with tier from `X-API-Key`.

Return 429 + `Retry-After` consistent with chat SSE.

---

## Step 2: Export bundle audit

Distinct audit action `CHAT_EXPORT_BUNDLE` with hashed user id and chat count.

Expose in `GET /api/v1/admin/audit/chat-exports` with action filter.

---

## Step 3: Admin cookie auth

Set `medexpertmatch-user-id=admin` cookie when `?user=admin` so admin Thymeleaf pages can call REST without manual headers.

---

## Step 4: Grafana tier panels

Add panels for `chat_rate_limited_total{tier=...}` and `chat_turn_duration` by tier in `grafana/dashboard.json`.

---

## Step 5: OpenAPI for chat lifecycle

Document `GET /api/v1/chats/export-bundle` and `DELETE /api/v1/chats/data` in `openapi.yaml`.

Add `ChatDataLifecycleIT` contract assertions for response shape.

---

## Step 6: Optional A2A autoconfigure

**Requires explicit human approval** to add `spring-ai-a2a-server-autoconfigure` to `pom.xml`.

---

## References

- `.agents/plans/archive/M22-a2a-federation-and-chat-lifecycle.md`
- `ChatDataLifecycleService.java`, `A2aJsonRpcController.java`, `SessionTokensWebController.java`
