# M15 AutoMemory Advisor Decision

**Date:** 2026-05-30  
**Status:** Keep Option B (explicit `AutoMemoryTools` + memory system prompt)

## Context

`spring-ai-agent-utils` 0.8.0 ships `AutoMemoryToolsAdvisor`, which would simplify long-term memory wiring. The project currently uses **Option B**: explicit `AutoMemoryTools` bean + `MEMORY_SYSTEM_PROMPT` + `PhiGuard` hard-reject before any disk write + `TimeGapConsolidationTrigger`.

## Evaluation

| Criterion | Option B (current) | AutoMemoryToolsAdvisor |
|---|---|---|
| PHI hard-reject before write | ✅ `PhiGuard` in `AutoMemoryService` | ❌ Not wired; would need custom advisor wrapper |
| Consolidation trigger control | ✅ `TimeGapConsolidationTrigger` | ⚠️ Library defaults; harder to align with HIPAA policy |
| HIPAA regression risk | Low | High without custom wrapper |
| Maintenance | Slightly more code | Cleaner if wrapper exists |

## Decision

**Do not migrate** to `AutoMemoryToolsAdvisor` in M15. Revisit in a future milestone only after a `PhiGuardAutoMemoryAdvisor` wrapper is designed and tested.

## References

- `llm/automemory/AutoMemoryService.java`, `PhiGuard.java`
- `.agents/plans/M08-agentic-patterns-improvements.md` Follow-ups
