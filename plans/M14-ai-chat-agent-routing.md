# M14: AI Chat Agent Routing & Specialist Subagents

M13 Phase A delivered the **chat domain module** (Flyway V3), **per-user session isolation**, **AI Chat tab** with sidebar, message history, and **agent picker** UI. Messages persist; assistant replies are placeholders until this milestone.

M14 wires real LLM/agent behavior: **Auto orchestrator**, **six specialist agents**, **M08 TaskTool subagents**, and **agentic UX** (AskUserQuestion, TodoWrite, SSE trace).

**Prerequisite:** M13 Steps 1–3 complete (see `plans/archive/M13-ai-chat-tab-phase-a.md` after archive).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|---|---|---|---|
| 1 | Wire chat messages to `medicalAgentChatClient` | `feat/chat-llm-integration` | ⬜ Planned | 4h |
| 2 | Agent picker → skill/subagent routing | `feat/chat-agent-routing` | ⬜ Planned | 3h |
| 3 | Auto orchestrator (TodoWrite + TaskTool) | `feat/chat-auto-orchestrator` | ⬜ Planned | 4h |
| 4 | Six specialist agent definitions (`resources/agents/`) | `feat/chat-specialist-agents` | ⬜ Planned | 4h |
| 5 | M08 Step 6 TaskTool on main agent | `feat/task-subagent-orchestration` | ⬜ Planned | 4h |
| 6 | Chat agentic UX (questions, todos, SSE) | `feat/chat-agentic-ux` | ⬜ Planned | 3h |
| 7 | ChatWebControllerIT + ChatControllerIT | `feat/chat-it-coverage` | ⬜ Planned | 2h |
| 8 | M08 Step 7 A2A (optional, pom approval) | `feat/a2a-interop-servers` | ⬜ Planned | 4h |

**Total effort: ~28h**

---

## Step 1: Chat → LLM integration

Replace placeholder assistant message in `ChatController.postMessage()` with:
- Session id `{userId}-{chatId}` via `OrchestrationContextHolder`
- Agent-specific system prompt from selected `agent_id`
- Invoke `medicalAgentChatClient` with conversation history from `chat_message` table
- Stream or sync response; persist assistant message

**Verification:** `ChatMessageIntegrationTest` (mock ChatModel)

---

## Step 2: Agent picker routing

Map picker values to constrained tool/skill sets:

| `agent_id` | Route |
|---|---|
| `auto` | Auto orchestrator (Step 3) |
| `triage-intake` | `triage` skill + AskUserQuestion |
| `case-analyzer` | `case-analyzer`, `clinical-advisor` |
| `evidence-scout` | `evidence-retriever`, `clinical-guideline` |
| `specialist-matcher` | `doctor-matcher`, `recommendation-engine` |
| `routing-planner` | `routing-planner` + facility tools |
| `network-analyst` | `network-analyzer` |

Persist `agent_id` on chat row when picker changes.

---

## Step 3: Auto orchestrator

- Classify intent (keyword + optional LLM)
- Multi-step: `TodoWriteTool` plan → `TaskTool` delegation → merged reply
- Aligns with M08 Step 6

**Verification:** `AutoOrchestratorRoutingTest`

---

## Step 4–8

See `plans/M13-ai-chat-tab-and-specialized-agents.md` Steps 5–8 for full detail.

---

## Relationship to M08

| M08 step | M14 item |
|---|---|
| Step 6 TaskTool | Steps 3, 5 |
| Step 7 A2A | Step 8 (optional) |

After M14, archive `M08-agentic-patterns-improvements.md` Steps 6–7 as done.
