# M25: Chat Platform Hardening — ✅ Complete

M24 delivered A2A JSON-RPC rate limits, admin export audit UI, chat lifecycle web controls, tier alert rules, and session token expiry warnings. M25 strengthened platform reliability and operator workflows.

**Prerequisite:** M24 complete (see `.agents/plans/archive/M24-a2a-production-readiness.md`).

## Delivered

| # | Deliverable | Status |
|---|---|---|
| 1 | Separate A2A vs chat SSE rate-limit buckets | ✅ |
| 2 | Admin dashboard landing page (`/admin?user=admin`) | ✅ |
| 3 | Chat export/delete lifecycle audit feedback toast | ✅ |
| 4 | Playwright smoke: admin + chat lifecycle | ✅ |
| 5 | Operator runbook `docs/chat-ops-runbook.md` | ✅ |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | ⏭ Skipped — pom approval not granted |

## Key files

- `chat/service/RateLimitScope.java`, `ChatRateLimitService.java` — scoped buckets `CHAT_SSE` / `A2A`
- `web/controller/AdminDashboardWebController.java`, `templates/admin/index.html`
- `chat/service/impl/ChatExportAuditorImpl.java` — `CHAT_DATA_DELETE`, `auditReferenceHash`
- `static/js/chat.js` — lifecycle toast with hashed audit ref
- `docs/chat-ops-runbook.md`
- `web/rest/ChatAdminPlaywrightSmokeTest.java`
- `core/config/RateLimitingConfig.java` — exclude `/api/v1/admin/` from global IP limiter

## Notes

- A2A and chat SSE no longer share per-user quota.
- Export/delete API responses include `auditReferenceHash`.
- Step 6 skipped per AGENTS.md pom approval requirement.

## Next

**`.agents/plans/M26-chat-federation-and-compliance.md`**
