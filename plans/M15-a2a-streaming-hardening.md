# M15: A2A Interop, Chat Streaming & Production Hardening

M14 delivered **LLM-backed chat**, **TaskTool subagent delegation**, **six specialist agents**, **Auto orchestrator routing**, **agentic UX** (AskUserQuestion/TodoWrite polling), and **chat IT coverage**. M08 Step 6 (TaskTool) is complete.

M15 focuses on **A2A interoperability** (M08 Step 7, requires pom approval), **optional SSE token streaming** in chat, and **production hardening**.

**Prerequisite:** M14 complete (see `plans/archive/M14-ai-chat-agent-routing.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | A2A server + AgentCard (M08 Step 7) | `feat/a2a-interop-servers` | ⬜ Planned | 4h |
| 2 | Chat SSE streaming for assistant tokens | `feat/chat-sse-streaming` | ⬜ Planned | 3h |
| 3 | Session branch isolation for subagents | `feat/session-branch-isolation` | ⬜ Planned | 2h |
| 4 | Chat ownership + agentic UX ITs (Docker) | `feat/chat-docker-its` | ⬜ Planned | 2h |
| 5 | AutoMemory advisor migration evaluation | `feat/automemory-advisor-eval` | ⬜ Planned | 2h |

**Total effort: ~13h**

---

## Step 1: A2A Integration (M08 Step 7)

**Requires explicit pom approval** per AGENTS.md (`spring-ai-a2a-server-autoconfigure`).

- Publish `AgentCard` at `/.well-known/agent-card.json` with skills `doctor_match`, `evidence_search`
- `DefaultAgentExecutor` bridging A2A to matching ChatClient + MedicalAgentTools
- Tests: `A2AAgentCardTest`, `A2ASendMessageTest` (PHI never in payloads)

---

## Step 2: Chat SSE streaming

Optional streaming endpoint mirroring aist-expertmatch `QueryStreamController`:
- Stream assistant tokens + tool steps to chat UI
- Collapsible execution trace via existing `/api/v1/logs/stream`

---

## Step 3: Session branch isolation

Wire `EventFilter.forBranch` for orchestrator vs subagent lineages (pairs with TaskTool).

---

## Step 4: Docker IT coverage

- `ChatOwnershipIT`, `ChatAgenticUxIT` in CI with Testcontainers
- `mvn verify -Dtest="*Chat*"`

---

## Step 5: AutoMemory advisor evaluation

Assess migrating Option B → `AutoMemoryToolsAdvisor` with `PhiGuard` wrapper (HIPAA).

---

## References

- `plans/M08-agentic-patterns-improvements.md` Step 7
- `docs/AGENTIC_PATTERNS_IMPROVEMENTS.md` Feature 5
- aist-expertmatch `QueryStreamController`
