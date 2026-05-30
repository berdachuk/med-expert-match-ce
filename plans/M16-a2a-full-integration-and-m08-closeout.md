# M16: Full A2A Integration & M08 Closeout

M15 delivered AgentCard discovery, A2A message stub (PHI-safe), chat SSE streaming, session branch isolation, chat Docker ITs, and an AutoMemory advisor decision (keep Option B).

M16 completes **M08 Step 7** with full A2A JSON-RPC (pending pom approval), session hardening ITs, and archives the M08 agentic patterns milestone.

**Prerequisite:** M15 complete (see `plans/archive/M15-a2a-streaming-hardening.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | Add `spring-ai-a2a-server-autoconfigure` + JSON-RPC executor | `feat/a2a-full-jsonrpc` | ⬜ Planned | 4h |
| 2 | Bridge A2A `sendMessage` to `MedicalAgentTools` / match workflow | `feat/a2a-executor-bridge` | ⬜ Planned | 3h |
| 3 | Session turn-safety IT (>20 turns, USER window) | `feat/session-turn-safety-it` | ⬜ Planned | 2h |
| 4 | Chat stream UX polish (inline token rendering) | `feat/chat-stream-ui-polish` | ⬜ Planned | 2h |
| 5 | Archive M08 agentic patterns plan | `docs/m08-closeout` | ⬜ Planned | 0.5h |

**Total effort: ~11.5h**

---

## Step 1: Full A2A server (M08 Step 7)

**Requires explicit human approval** to add `spring-ai-a2a-server-autoconfigure` to `pom.xml` per AGENTS.md.

- Replace stub `A2AMessageController` with `DefaultAgentExecutor`
- Keep `PhiGuard` validation on all inbound payloads
- Tests: `A2ASendMessageIT`, `A2AAgentCardIT`

---

## Step 2: A2A → domain bridge

- Route `doctor_match` skill to matching workflow
- Route `evidence_search` skill to evidence retrieval
- Structured JSON responses (no PHI)

---

## Step 3: Session turn-safety IT

- Drive >20 turns against `SessionMemoryAdvisor`
- Assert retained window starts on USER message
- Requires Docker Testcontainers in CI

---

## Step 4: Chat stream UI

- Render streaming tokens inline in `#messagePanel` (avoid full page reload)
- Collapse execution trace by default

---

## Step 5: M08 archive

When Steps 1–4 verified, move `plans/M08-agentic-patterns-improvements.md` to `plans/archive/`.

---

## References

- `plans/M08-agentic-patterns-improvements.md` Step 7
- `docs/AGENTIC_PATTERNS_IMPROVEMENTS.md`
- `docs/decisions/M15-automemory-advisor-decision.md`
