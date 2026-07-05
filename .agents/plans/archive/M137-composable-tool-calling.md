# M137 — Composable Tool Calling: Progressive Disclosure, Inner Thinking, Advisor Ordering

- **Milestone:** M137
- **REQ:** REQ-134
- **Status:** Active
- **Date:** 2026-06-24
- **Follows:** M136 (self-correcting structured output), M132 (ultra-compact JSON)

## Goal

Apply three improvements from Spring AI 2.0's composable tool calling architecture: (1) replace `ToolCallingAdvisor` with `ToolSearchToolCallingAdvisor` for progressive tool disclosure (34-64% token reduction), (2) add `AugmentedToolCallbackProvider` for inner-thinking traceability on all medical tools, and (3) move `SessionMemoryAdvisor` inside the tool loop to capture full tool request/response transcript.

## Background

The codebase has 32 `@Tool`-annotated methods across 9 tool classes, all registered on every LLM request. At 30+ tools, this causes context bloat and accuracy degradation. The blog post at https://spring.io/blog/2026/06/15/spring-ai-composable-tool-calling describes `ToolSearchToolCallingAdvisor` (progressive disclosure), `AugmentedToolCallbackProvider` (inner thinking), and advisor ordering (memory inside the tool loop).

## Scope

- **In scope:**
  1. Replace `ToolCallingAdvisor` with `ToolSearchToolCallingAdvisor` in `MedicalAgentConfiguration`.
  2. Configure `ToolSearchToolCallingAdvisor` with `vector` index strategy (PgVector).
  3. Add `spring.ai.chat.client.tool-search-advisor.*` properties to `application.yml`.
  4. Create `AgentThinking` record for tool argument augmentation.
  5. Wrap all tool objects with `AugmentedToolCallbackProvider<AgentThinking>` in `MedicalAgentConfiguration` and `AgentToolCallingConfiguration`.
  6. Add `.order(ToolCallingAdvisor.DEFAULT_ORDER + 1)` to `SessionMemoryAdvisor` builder.
  7. Verify `conversationHistoryEnabled(false)` is still correct (it is — memory inside loop).
  8. Update tests for new advisor wiring.
  9. Run `mvn verify`, fix failures.
  10. Security review.
  11. Commit + merge to develop.
- **Out of scope:**
  - MCP tools (no external MCP servers configured).
  - `FunctionToolCallback.builder()` (current `@Tool` + `MethodToolCallbackProvider` is sufficient).
  - Changing the custom `ToolCallbackResolver` chain (it works with progressive disclosure).

## Tasks

1. [ ] Create `AgentThinking` record in `llm/domain/`.
2. [ ] Add `ToolSearchToolCallingAdvisor` bean in `MedicalAgentConfiguration`, replacing `ToolCallingAdvisor`.
3. [ ] Add tool-search-advisor properties to `application.yml` (enable, index-type=vector, session-id-key).
4. [ ] Wrap tool objects with `AugmentedToolCallbackProvider<AgentThinking>` in `MedicalAgentConfiguration.medicalAgentChatClient()`.
5. [ ] Wrap tool objects with `AugmentedToolCallbackProvider<AgentThinking>` in `AgentToolCallingConfiguration.toolCallbackResolver()`.
6. [ ] Add `.order(ToolCallingAdvisor.DEFAULT_ORDER + 1)` to `SessionMemoryAdvisor` builder.
7. [ ] Update `MedicalAgentMemoryWiringTest` and `MedicalAgentTodoWiringTest` for new advisor wiring.
8. [ ] Run `mvn verify`, fix failures.
9. [ ] Security review.
10. [ ] Run `sync-memory-index.sh --check`.
11. [ ] Commit + merge to develop.

## Verification

- `mvn verify` passes (all unit + integration tests).
- `ToolSearchToolCallingAdvisor` is registered in place of `ToolCallingAdvisor`.
- `SessionMemoryAdvisor` has order > `ToolCallingAdvisor.DEFAULT_ORDER`.
- `AugmentedToolCallbackProvider` wraps all 9 tool objects.
- `sync-memory-index.sh --check` passes.

## Risks

- RISK-138: `ToolSearchToolCallingAdvisor` with `vector` index requires a `VectorStore` bean. The codebase has PgVector but the bean name/type must match. Mitigation: fall back to `regex` index if vector store is unavailable.
- RISK-139: `AugmentedToolCallbackProvider` adds tokens per tool call (inner thinking). Mitigation: monitor via `LlmUsageTelemetryService`; the thinking field is a small record.
- RISK-140: Moving `SessionMemoryAdvisor` inside the tool loop may increase session storage size. Mitigation: compaction trigger/strategy already configured; monitor session sizes.

## Traceability

- REQ-134 — Composable Tool Calling Improvements
- Extends DEC-007 (role-separated LLM endpoints) — tool search uses utility endpoint
- Extends DEC-012 (multi-tier LLM pipeline) — progressive disclosure is a tier optimization
- RISK-138 — vector store availability
- RISK-139 — inner thinking token cost
- RISK-140 — session storage growth
