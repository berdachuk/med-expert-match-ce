# M22: A2A Federation & Chat Lifecycle — ✅ Complete

M21 delivered admin session-token APIs, export audit queries, chat retention scheduling, session-token auth profile, and Grafana alert documentation. M22 extended agent interoperability and chat lifecycle management.

**Prerequisite:** M21 complete (see `.agents/plans/archive/M21-chat-admin-and-observability.md`).

## Delivered

| # | Deliverable | Status |
|---|---|---|
| 1 | A2A skill registry endpoint + OpenAPI (`GET /a2a/v1/skills`) | ✅ |
| 2 | Chat data lifecycle — user delete-all + export bundle (PHI-redacted) | ✅ |
| 3 | Prometheus alert rules file (`grafana/chat-alerts.yml`) | ✅ |
| 4 | Admin UI page for session tokens (Thymeleaf) | ✅ |
| 5 | Rate-limit tier metrics by tier label | ✅ |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | ⏭ Skipped — pom approval not granted |

## Key files

- `llm/rest/A2aJsonRpcController.java` — `GET /a2a/v1/skills`
- `llm/service/A2aSkillRegistryService.java`, `A2aSkillRegistryServiceImpl.java`
- `chat/service/ChatDataLifecycleService.java`, `ChatDataLifecycleServiceImpl.java`
- `chat/rest/ChatController.java` — `GET /export-bundle`, `DELETE /data`
- `sql/chat/softDeleteMessagesByChatId.sql`, `chat_message.deleted_at` in V1 schema
- `chat/service/ChatTurnMetrics.java` — tier tags on `chat.rate.limited` and `chat.turn.duration`
- `web/controller/SessionTokensWebController.java`, `templates/admin/session-tokens.html`
- `grafana/chat-alerts.yml`
- Tests: `A2aSkillRegistryIT`, `ChatDataLifecycleIT`, `SessionTokensWebControllerIT`

## Notes

- Message soft-delete sets content to `[deleted]` and filters from history queries.
- Export bundle records per-chat audit events with hashed identifiers.
- Step 6 skipped per AGENTS.md pom approval requirement.

## Next

**`.agents/plans/M23-chat-security-and-a2a-governance.md`**
