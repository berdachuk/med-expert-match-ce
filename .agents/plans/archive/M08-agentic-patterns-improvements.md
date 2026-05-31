# M08: Agentic Patterns Improvements (Spring AI Series Parts 1–7) — ✅ Complete

M07 delivered security hardening, web UI integration tests, API docs, and production configs. M08 evolved the medical agent into a fully agentic platform by adopting the **Spring AI Agentic Patterns** series (Parts 1–7).

Each pattern was delivered on its **own feature branch** (TDD) and merged to `main` after the unit suite passes. Full pattern-to-codebase analysis lives in `docs/AGENTIC_PATTERNS_IMPROVEMENTS.md`.

## Scope — all complete

| # | Pattern (Series Part) | Status |
|---|---|---|
| 0 | TDD project rule (governance) | ✅ |
| 0b | Upgrade `spring-ai-agent-utils` 0.5.0 → 0.8.0 | ✅ |
| 1 | Session — turn-safe short-term memory (P7) | ✅ |
| 2 | AutoMemory — long-term, PHI-safe memory (P6) | ✅ |
| 3 | Agent Skills — formal `SkillsTool` registry (P1) | ✅ |
| 4 | TodoWrite — multi-step plan tracking (P3) | ✅ |
| 5 | AskUserQuestion — interactive intake (P2) | ✅ |
| 6 | Subagent orchestration — `TaskTool` (P4) | ✅ |
| 7 | A2A integration — interoperable agents (P5) | ✅ — stub M15, full JSON-RPC M16 |
| 8 | AI Chat tab + per-user sessions + agent picker | ✅ — see `.agents/plans/archive/M14-ai-chat-agent-routing.md` |

**Total: ~49.5h**

## M16 closeout items (formerly open)

- **Chat agent activity panel (W-01):** Delivered in M16 Step 4.
- **Markdown in chat responses (W-02):** Delivered in M16 Step 5.
- **Full A2A JSON-RPC:** Delivered in M16 Step 1 (custom controller, no pom bump).

## Follow-ups moved to M17

- Rich SSE events (`tool_call`, `reasoning`) for activity panel parity with Cursor
- Full >20-turn JDBC session compaction IT
- Optional migration to `spring-ai-a2a-server-autoconfigure` (requires pom approval)
- Markdown XSS integration test

## References

- Series Parts 1–7: `docs/AGENTIC_PATTERNS_IMPROVEMENTS.md`
- AI Chat tab: `.agents/plans/archive/M14-ai-chat-agent-routing.md`
- A2A + chat UX: `.agents/plans/archive/M16-a2a-full-integration-and-m08-closeout.md`
- AutoMemory decision: `docs/decisions/M15-automemory-advisor-decision.md`
