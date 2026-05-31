# M14: AI Chat Agent Routing & Specialist Subagents — ✅ Complete

M13 Phase A delivered the chat domain module, per-user isolation, AI Chat tab, and agent picker. M14 wired real LLM/agent behavior.

## Delivered

| # | Deliverable | Status |
|---|---|---|
| 1 | Wire chat messages to `medicalAgentChatClient` via `ChatAssistantService` | ✅ |
| 2 | Agent picker → skill routing (`ChatAgentProfile`) | ✅ |
| 3 | Auto orchestrator (TodoWrite + TaskTool + intent hints) | ✅ |
| 4 | Seven agent definitions under `resources/agents/` | ✅ |
| 5 | M08 Step 6 TaskTool on main agent | ✅ |
| 6 | Chat agentic UX (questions, todos polling in `chat.js`) | ✅ |
| 7 | `ChatWebControllerIT` + `ChatControllerIT` | ✅ |
| 8 | M08 Step 7 A2A | ⬜ Deferred to **M15** (pom approval) |

## Key files

- `llm/service/impl/ChatAssistantServiceImpl.java` — LLM turn orchestration
- `llm/chat/ChatAgentProfile.java` — agent picker routing
- `llm/config/MedicalAgentConfiguration.java` — TaskTool + AskUserQuestion on chat client
- `resources/agents/*.md` — Auto + 6 specialists
- `resources/prompts/chat-agent-system.st`
- `static/js/chat.js` — agentic UX polling

## Tests

- `ChatAgentProfileTest`, `MedicalSubagentRoutingTest`, `SpecialistAgentScopeTest`
- `ChatAssistantServiceImplTest`, `ChatWebControllerIT`, `ChatControllerIT`

## Next

**`.agents/plans/M15-a2a-streaming-hardening.md`**
