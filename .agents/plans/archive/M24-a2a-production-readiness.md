# M24: A2A Production Readiness — ✅ Complete

M23 delivered A2A stream rate limits, export-bundle audit with admin filters, admin cookie auth, Grafana tier panels, and chat lifecycle OpenAPI. M24 focused on production hardening and operator UX.

**Prerequisite:** M23 complete (see `.agents/plans/archive/M23-chat-security-and-a2a-governance.md`).

## Delivered

| # | Deliverable | Status |
|---|---|---|
| 1 | A2A JSON-RPC rate limiting (non-stream parity) | ✅ |
| 2 | Admin audit UI for chat exports (Thymeleaf) | ✅ |
| 3 | Chat lifecycle web UI (export bundle + delete data) | ✅ |
| 4 | Tier-specific rate-limit alert rules in `grafana/chat-alerts.yml` | ✅ |
| 5 | Session token expiry warnings in admin UI (7-day window) | ✅ |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | ⏭ Skipped — pom approval not granted |

## Key files

- `llm/rest/A2aJsonRpcController.java` — shared `enforceRateLimit()` for jsonrpc + stream
- `web/controller/ChatExportsWebController.java`, `templates/admin/chat-exports.html`
- `templates/fragments/chat-sidebar.html`, `static/js/chat.js` — export/delete lifecycle
- `templates/admin/session-tokens.html` — expiring token highlights
- `grafana/chat-alerts.yml` — DEFAULT/HIGH tier spike rules
- Tests: `A2aJsonRpcRateLimitIT`, `ChatExportsWebControllerIT`, extended `ChatWebControllerIT`, `SessionTokensWebControllerIT`

## Notes

- JSON-RPC and stream share the same per-user rate limit bucket.
- Admin chat export page uses cookie auth (no manual `X-User-Id` header).
- Step 6 skipped per AGENTS.md pom approval requirement.

## Next

**`.agents/plans/M25-chat-platform-hardening.md`**
