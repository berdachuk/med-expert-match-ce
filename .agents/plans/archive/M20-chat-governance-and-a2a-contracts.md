# M20: Chat Governance & A2A Contract Hardening — ✅ Complete

M19 delivered per-user chat rate limits, Grafana chat panels, PHI-safe export, A2A SSE stream parity, and Playwright profile documentation. M20 strengthened governance (tier-aware limits, audit), expanded A2A contract coverage, and added full Playwright navigation.

**Prerequisite:** M19 complete (see `.agents/plans/archive/M19-chat-ops-and-interop.md`).

## Delivered

| # | Deliverable | Status |
|---|---|---|
| 1 | Tier-aware chat limits via `ApiSessionToken.rateLimitTier` | ✅ |
| 2 | Chat export audit log (hashed user/chat id, no PHI) + `chat.export.count` | ✅ |
| 3 | OpenAPI + JSON schema for A2A stream/jsonrpc responses | ✅ |
| 4 | Full Playwright chat navigation test (optional CI) | ✅ |
| 5 | Rate-limit integration with `Retry-After` response headers | ✅ |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | ⏭ Skipped — pom approval not granted |

## Key files

- `core/security/UserContext.java`, `HeaderBasedUserContext.java` — tier resolution from `X-API-Key`
- `core/exception/RateLimitExceededException.java`, `GlobalExceptionHandler.java` — `Retry-After` on 429
- `chat/service/ChatExportAuditor.java`, `ChatExportAuditorImpl.java` — audit + metric
- `core/util/IdentifierHasher.java` — SHA-256 digests for audit actors/resources
- `src/main/resources/api/openapi.yaml` — `/a2a/v1/jsonrpc`, `/a2a/v1/stream` schemas
- `pom.xml` — Playwright test dependency + `-Pplaywright` profile
- Tests: `ChatRateLimitTierIT`, `ChatExportAuditIT`, `A2aContractIT`, `ChatPlaywrightSmokeTest`, `HeaderBasedUserContextTest`, `IdentifierHasherTest`

## Notes

- Per-user buckets keyed by `userId:tier` in `ChatRateLimitService`.
- Export audit uses `CHAT_EXPORT` action; no raw user/chat ids in `audit_log`.
- Playwright test gates on `playwright.enabled`; default CI uses MockMvc ITs.
- Step 6 skipped per AGENTS.md pom approval requirement.

## Next

**`.agents/plans/M21-chat-admin-and-observability.md`**
