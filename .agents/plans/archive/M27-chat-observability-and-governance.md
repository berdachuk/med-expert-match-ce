# M27: Chat Observability & Governance — ✅ Complete

M26 delivered A2A agent card federation hints, retention metrics with admin visibility, export bundle schema validation, ops dashboard runbook link, and scoped rate-limit metrics. M27 deepened observability and governance automation.

**Prerequisite:** M26 complete (see `.agents/plans/archive/M26-chat-federation-and-compliance.md`).

## Delivered

| # | Deliverable | Status |
|---|---|---|
| 1 | Grafana alerts for retention purge failures and scope rate-limit spikes | ✅ |
| 2 | Admin API for retention stats (`GET /api/v1/admin/chat-retention`) | ✅ |
| 3 | A2A agent card OpenAPI component + contract IT | ✅ |
| 4 | Export bundle JSON Schema file + CI validation | ✅ |
| 5 | Playwright smoke: admin retention card + runbook link | ✅ |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | ⏭ Skipped — pom approval not granted |

## Key files

- `grafana/chat-alerts.yml` — retention failure + scope imbalance alerts
- `core/service/ChatRetentionQueryService.java`, `chat/service/impl/ChatRetentionQueryServiceImpl.java`
- `core/rest/AdminController.java` — `GET /api/v1/admin/chat-retention`
- `api/openapi.yaml` — `AgentCardResponse`, `/.well-known/agent.json`
- `api/schemas/chat-export-bundle.schema.json`, `ChatExportBundleJsonSchemaTest.java`
- `llm/rest/A2aAgentCardOpenApiIT.java`, `core/rest/AdminChatRetentionIT.java`
- `web/rest/ChatAdminPlaywrightSmokeTest.java` — retention + runbook checks

## Notes

- Modulith-safe retention admin API via core query interface + chat impl.
- JSON Schema validated in unit test + IT via `json-schema-validator` (test scope).
- Step 6 skipped per AGENTS.md pom approval requirement.

## Next

**`.agents/plans/M28-chat-trust-and-interoperability.md`**
