# M21: Chat Admin & Observability — ✅ Complete

M20 delivered tier-aware chat rate limits, export audit with hashed identifiers, A2A OpenAPI schemas, Retry-After headers, and optional Playwright navigation. M21 focused on operability: session-token admin API, Grafana export/audit visibility, chat retention, and session-token auth profile.

**Prerequisite:** M20 complete (see `.agents/plans/archive/M20-chat-governance-and-a2a-contracts.md`).

## Delivered

| # | Deliverable | Status |
|---|---|---|
| 1 | Grafana panel for `chat.export.count` + rate-limit alert hints | ✅ |
| 2 | Admin REST for API session tokens (list/create/revoke, no key echo) | ✅ |
| 3 | Audit log query endpoint for `CHAT_EXPORT` (hashed ids only) | ✅ |
| 4 | Chat retention job — purge idle chats older than configurable TTL | ✅ |
| 5 | Wire `ApiKeyAuthFilter` to `ApiSessionTokenRepository` (optional profile) | ✅ |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | ⏭ Skipped — pom approval not granted |

## Key files

- `core/rest/AdminController.java` — `/api/v1/admin/session-tokens`, `/api/v1/admin/audit/chat-exports`
- `core/security/AdminAccessGuard.java` — `X-User-Id: admin` gate
- `core/service/ApiSessionTokenAdminService.java`, `ChatExportAuditQueryService.java`
- `core/config/SessionTokenApiKeyAuthFilter.java` — `medexpertmatch.auth.session-tokens.enabled`
- `chat/config/ChatRetentionProperties.java`, `ChatRetentionServiceImpl.java`, `ChatRetentionScheduler.java`
- `docs/grafana-chat-alerts.md`
- Tests: `AdminSessionTokenIT`, `AdminChatExportAuditIT`, `ChatRetentionServiceImplTest`, `SessionTokenApiKeyAuthFilterIT`

## Notes

- Session token auth replaces static `api-keys` when `session-tokens.enabled=true`.
- Chat retention disabled by default (`chat.retention.idle-days=0`).
- Step 6 skipped per AGENTS.md pom approval requirement.

## Next

**`.agents/plans/M22-a2a-federation-and-chat-lifecycle.md`**
