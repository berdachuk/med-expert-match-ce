# M28: Chat Trust & Interoperability

M27 delivered retention/scope Grafana alerts, admin retention API, agent card OpenAPI contract, export JSON Schema CI validation, and Playwright admin polish. M28 focuses on trust boundaries and cross-system interoperability.

**Prerequisite:** M27 complete (see `.agents/plans/archive/M27-chat-observability-and-governance.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | Agent card JSON Schema + contract validation IT | `feat/agent-card-schema` | ⬜ Planned | 3h |
| 2 | Export bundle OpenAPI `$ref` to standalone JSON Schema | `feat/openapi-schema-ref` | ⬜ Planned | 2h |
| 3 | Admin retention API linked from dashboard + OpenAPI admin tag expansion | `feat/admin-ui-api-link` | ⬜ Planned | 2h |
| 4 | Retention scheduler health indicator (Actuator) | `feat/retention-health` | ⬜ Planned | 3h |
| 5 | A2A stream parity contract IT (SSE envelope vs chat stream) | `feat/a2a-stream-parity` | ⬜ Planned | 3h |
| 6 | *(Optional)* `spring-ai-a2a-server-autoconfigure` migration | `feat/a2a-autoconfigure` | ⬜ Blocked — pom approval | 4h |

**Total effort: ~13h (+4h optional)**

---

## Step 1: Agent card JSON Schema

Publish `agent-card.schema.json`; validate `/.well-known/agent.json` responses in CI.

---

## Step 2: OpenAPI schema ref

Link `ChatExportBundleResponse` to `api/schemas/chat-export-bundle.schema.json` for single source of truth.

---

## Step 3: Admin UI ↔ API link

Wire admin retention card to live `/api/v1/admin/chat-retention` fetch; document remaining admin endpoints in OpenAPI.

---

## Step 4: Retention health indicator

Actuator indicator reporting last run age and failure count for operators.

---

## Step 5: A2A stream parity IT

Assert A2A `/a2a/v1/stream` SSE token envelope matches chat stream contract.

---

## Step 6: Optional A2A autoconfigure

**Requires explicit human approval** to add `spring-ai-a2a-server-autoconfigure` to `pom.xml`.

---

## References

- `.agents/plans/archive/M27-chat-observability-and-governance.md`
- `AgentCardServiceImpl.java`, `api/schemas/chat-export-bundle.schema.json`, `A2aStreamParityIT.java`
