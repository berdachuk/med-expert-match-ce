# M19: Chat Ops, Rate Limits & Browser Interop — ✅ Complete

M18 delivered server-side markdown SSR sanitization, chat turn Micrometer metrics, A2A AgentCard discovery alias, chat E2E smoke ITs, and session compaction observability. M19 focused on operational hardening and optional browser-level verification.

**Prerequisite:** M18 complete (see `.agents/plans/archive/M18-chat-production-readiness.md`).

## Delivered

| # | Deliverable | Status |
|---|---|---|
| 1 | Per-user chat rate limiting on SSE stream + `chat.rate.limited` metric | ✅ |
| 2 | Grafana panels for `chat.turn.*` and `chat.rate.limited` | ✅ |
| 3 | Playwright profile gate (`-Pplaywright`) + testing skill docs | ✅ |
| 4 | Chat session export `GET /api/v1/chats/{id}/export` (PHI-redacted JSON) | ✅ |
| 5 | A2A streaming `POST /a2a/v1/stream` + parity IT | ✅ |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | ⏭ Skipped — pom approval not granted |

## Key files

- `chat/service/ChatRateLimitService.java`, `core/util/TokenBucket.java`
- `chat/service/ChatExportService.java`, `ChatExportServiceImpl.java`
- `chat/rest/ChatController.java` — rate limit + export endpoints
- `llm/rest/A2aJsonRpcController.java` — `/a2a/v1/stream`
- `llm/service/impl/A2AMessageServiceImpl.java` — `streamMessage()`
- `core/config/GlobalExceptionHandler.java` — `ResponseStatusException` (429) handling
- `grafana/dashboard.json` — chat metrics panels
- Tests: `ChatRateLimitServiceTest`, `ChatRateLimitIT`, `ChatExportServiceImplTest`, `ChatExportIT`, `A2aStreamParityIT`, `GlobalExceptionHandlerTest`

## Notes

- Shared `TokenBucket` extracted from `RateLimitingConfig` for reuse.
- Playwright remains optional; `ChatPlaywrightSmokeTest` gates on `playwright.enabled` system property.
- Step 6 skipped per AGENTS.md pom approval requirement.

## Next

**`.agents/plans/M20-chat-governance-and-a2a-contracts.md`**
