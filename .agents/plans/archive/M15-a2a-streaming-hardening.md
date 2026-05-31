# M15: A2A Interop, Chat Streaming & Production Hardening — ✅ Complete

M14 delivered LLM-backed chat with TaskTool subagents. M15 added discovery, streaming, session branch isolation, Docker ITs, and AutoMemory evaluation.

## Delivered

| # | Deliverable | Status |
|---|---|---|
| 1 | AgentCard at `/.well-known/agent-card.json` + A2A stub `POST /a2a/v1/sendMessage` | ✅ (full JSON-RPC deferred — no pom change) |
| 2 | Chat SSE streaming `POST /api/v1/chats/{id}/messages/stream` | ✅ |
| 3 | Session branch isolation (`EventFilter.forBranch("orch")`) | ✅ |
| 4 | `ChatOwnershipIT`, `ChatAgenticUxIT` | ✅ |
| 5 | AutoMemory advisor evaluation | ✅ — keep Option B (`docs/decisions/M15-automemory-advisor-decision.md`) |

## Key files

- `llm/rest/AgentCardController.java`, `A2AMessageController.java`
- `llm/agent/SessionAdvisorSupport.java`, `AgentSessionBranches.java`
- `ChatAssistantService.streamMessage()` + `ChatController` stream endpoint
- `static/js/chat.js` — SSE stream + execution trace panel
- Tests: `A2AAgentCardTest`, `A2ASendMessageTest`, `SessionBranchIsolationTest`, `ChatOwnershipIT`, `ChatAgenticUxIT`

## Deferred to M16

- Full `spring-ai-a2a-server-autoconfigure` JSON-RPC executor (requires pom approval)
- Session turn-safety IT (>20 turns)
- M08 milestone archive

## Next

**`.agents/plans/M17-chat-agent-polish-and-a2a-hardening.md`**
