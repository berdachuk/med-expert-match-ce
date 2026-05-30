# M08: Agentic Patterns Improvements (Spring AI Series Parts 1–7)

M07 delivered security hardening, web UI integration tests, API docs, and production configs. M08 evolves the medical agent into a fully agentic platform by adopting the **Spring AI Agentic Patterns** series (Parts 1–7), governed by a new mandatory **TDD workflow** rule and built on an upgraded `spring-ai-agent-utils 0.8.0`.

Each pattern is delivered on its **own feature branch** (TDD: write test → review test vs. requirements → implement → re-run until green) and merged to `main` after the unit suite passes. Full pattern-to-codebase analysis lives in `docs/AGENTIC_PATTERNS_IMPROVEMENTS.md`.

## Scope

| # | Pattern (Series Part) | Branch | Status | Effort |
|---|---|---|---|---|
| 0 | TDD project rule (governance) | `feat/tdd-project-rule` | ✅ Done | 0.5h |
| 0b | Upgrade `spring-ai-agent-utils` 0.5.0 → 0.8.0 | (in `feat/agent-skills-registry`) | ✅ Done | 0.5h |
| 1 | Session — turn-safe short-term memory (P7) | `feat/session-turn-safe-compaction` | ✅ Done | 2h |
| 2 | AutoMemory — long-term, PHI-safe memory (P6) | `feat/auto-memory-consolidation` | ✅ Done | 2h |
| 3 | Agent Skills — formal `SkillsTool` registry (P1) | `feat/agent-skills-registry` | ✅ Done | 2h |
| 4 | TodoWrite — multi-step plan tracking (P3) | `feat/todowrite-plan-tracking` | ⬜ Planned | 2h |
| 5 | AskUserQuestion — interactive intake (P2) | `feat/ask-user-question-intake` | ⬜ Planned | 3h |
| 6 | Subagent orchestration — `TaskTool` (P4) | `feat/task-subagent-orchestration` | ⬜ Planned | 4h |
| 7 | A2A integration — interoperable agents (P5) | `feat/a2a-interop-servers` | ⬜ Planned | 4h |

**Completed: ~7.5h · Remaining: ~13h · Total: ~20.5h**

---

## Completed Work

### Step 0: TDD Project Rule — ✅ `feat/tdd-project-rule`

Mandatory four-step TDD loop added to `AGENTS.md` (Key Rules + dedicated section), `docs/CODING_RULES.md`, and `.agents/skills/testing/SKILL.md`:

1. Write the test first.
2. Double-check the test against requirements using an internal review tool/skill.
3. Only then implement.
4. Re-run the test (`mvn verify`) and fix until it passes.

### Step 0b: Dependency Upgrade — ✅ (committed within `feat/agent-skills-registry`)

`spring-ai-agent-utils` upgraded `0.5.0 → 0.8.0` (explicitly user-approved; AGENTS.md otherwise forbids pom version changes). 0.8.0 jar inspected with `javap`: `SkillsTool`/`FileSystemTools`/`ShellTools` builder APIs unchanged (no code adaptation). **`AutoMemoryToolsAdvisor` now exists in 0.8.0** (absent in 0.5.0) — see Follow-ups.

### Step 1: Session — turn-safe short-term memory (P7) — ✅ `feat/session-turn-safe-compaction`

- `AgentSessionProperties` (`agent.session.*`: max-turns=20, max-tokens=4000, default ttl=30).
- `MedicalAgentConfiguration`: `DefaultSessionService` bean + `SessionMemoryAdvisor` with `CompositeCompactionTrigger.anyOf(TurnCountTrigger(20), TokenCountTrigger(4000))` paired with `TurnWindowCompactionStrategy` (non-LLM), shared `JTokkitTokenCountEstimator`.
- Session id threaded via `SESSION_ID_CONTEXT_KEY` in the recommendation workflow; auto-created when absent.
- Tests: `SessionCompactionConfigTest` (incl. trigger-without-strategy guard) + workflow session-id test.

### Step 2: AutoMemory — long-term, PHI-safe memory (P6) — ✅ `feat/auto-memory-consolidation`

- Implemented as **Option B** (explicit `AutoMemoryTools` + memory system prompt) rather than the library advisor, to preserve the project's `PhiGuard` and consolidation trigger.
- `AgentMemoryProperties` (`agent.memory.*`): `dir` (default `${user.home}/.spring-ai-agent/medexpertmatch/memory`, auto-created) + `consolidation.gap-seconds=60` (+ optional turn-count/probabilistic).
- `PhiGuard` rejects/redacts SSN, MRN, DOB, email, phone, patient-name labels; `AutoMemoryService` hard-rejects PHI before any disk write.
- `MemoryConsolidationTrigger` + `TimeGapConsolidationTrigger` (clock-injectable). 24 unit tests.

### Step 3: Agent Skills — formal `SkillsTool` registry (P1) — ✅ `feat/agent-skills-registry`

- `SkillsTool` registered on `medicalAgentChatClient` (classpath-loaded, JAR-safe) via `AgentSkillsProperties` (`agent.skills.dir` default `skills`, `extra-directory`). Progressive disclosure: metadata in tool description, full `SKILL.md` loaded on invocation.
- `FileSystemTools` (Read) enabled; **`ShellTools` intentionally disabled** (HIPAA / no unsandboxed execution).
- Two new advisory, non-PHI skills authored: `clinical-guideline`, `triage` (registry now 9 skills).
- 12 unit tests (registry resolution, frontmatter validation, properties binding, wiring).

**Test baseline after Step 3: 173 unit tests passing, 0 failures.**

---

## Planned Work

### Step 4: TodoWrite — multi-step plan tracking (P3)

**Goal:** Make the recommendation pipeline (analyze → retrieve evidence → match → rank → route) explicit and observable; stream live progress to the web UI.

**Changes**
- Register `TodoWriteTool.builder().todoEventHandler(...)` on the recommendation `ChatClient`.
- Per Part 3 guidance, pair with `ToolCallAdvisor(conversationHistoryEnabled=false)` so todo updates persist in chat memory (coexist with the Session advisor).
- Publish `TodoUpdateEvent` → `@EventListener` → SSE → progress bar in `web/`.

**Test first:** `RecommendationTodoTrackingTest` — prompt with ≥3 steps; assert a todo list is created, exactly one task `in_progress` at a time, completion event per step.

**Verification:** `mvn test -Dtest="*TodoTracking*"`

### Step 5: AskUserQuestion — interactive intake (P2)

**Goal:** Convert case intake from assumption-based to clarifying — ask structured questions (urgency, specialty hint, prior history) before matching.

**Changes**
- Add `AskUserQuestionTool.builder().questionHandler(...)` to `MedicalAgentCaseIntakeWorkflowServiceImpl`.
- Web handler: bridge async UI via `CompletableFuture` — push `Question`/`Option` over SSE, block on `future.get()`, complete from a REST endpoint when answered. Optionally expose via `@McpElicitation`.

**Test first:** `CaseIntakeClarificationTest` — stub `QuestionHandler` returns canned answers; assert workflow uses answers (no assumption defaults); free-text/unanswered paths handled.

**Verification:** `mvn test -Dtest="*CaseIntakeClarification*"`

### Step 6: Subagent Orchestration — `TaskTool` (P4)

**Goal:** Replace ad-hoc workflow-service chaining with a formal Task-tool orchestrator + registered specialized subagents in isolated context windows, with multi-model routing.

**Changes**
- Define subagents as Markdown + YAML frontmatter under `src/main/resources/agents/`: `case-analyzer`, `evidence-retriever`, `doctor-matcher`, `network-analyzer`, `routing-planner`.
- Configure `TaskToolCallbackProvider` with `ClaudeSubagentReferences.fromRootDirectory("classpath:agents")`.
- Multi-model routing: fast/cheap model for triage; strong model (MedGemma) for clinical reasoning via per-subagent `model:`.
- Constraint: no `Task` in subagent `tools` (no nested subagents). Pair with Session **branch isolation** (`EventFilter.forBranch`).

**Test first:** `MedicalSubagentRoutingTest` — assert "explore evidence" delegates to `evidence-retriever` only; context isolation (no cross-leak of patient data).

**Verification:** `mvn test -Dtest="*SubagentRouting*"`

### Step 7: A2A Integration — interoperable agents (P5)

**Goal:** Expose match/evidence capabilities as A2A servers so external hospital systems / partner agents can discover and call them over an open standard (HTTP + JSON-RPC).

**Changes**
- Add `spring-ai-a2a-server-autoconfigure` (test scope first; promote with approval — pom version rule).
- Publish `AgentCard` at `/.well-known/agent-card.json` with skills `doctor_match`, `evidence_search`.
- `DefaultAgentExecutor` bridging A2A requests to the matching `ChatClient` + `MedicalAgentTools`.
- (Future) Host/router agent via `RemoteAgentConnections.sendMessage` for cross-institution delegation.

**Test first:** `A2AAgentCardTest` + `A2ASendMessageTest` — card served with correct skills; JSON-RPC `sendMessage` returns a structured match; **PHI never in A2A payloads** (assert anonymization).

**Verification:** `mvn test -Dtest="A2A*"`

---

## Follow-ups / Open Decisions

- **AutoMemory advisor migration:** 0.8.0 ships `AutoMemoryToolsAdvisor`. Migrating from Option B would be cleaner but currently bypasses the project's `PhiGuard` + consolidation trigger (HIPAA regression). Decision: keep Option B, or wrap `PhiGuard` into a custom advisor before migrating.
- **Integration tests:** `*IT.java` / `mvn verify` require the custom Postgres+AGE+PgVector Testcontainers image and must run in CI (Docker unavailable in the dev sandbox used for M08). Add the ">20 turns window starts on USER message" turn-safety IT and the A2A/PHI ITs to CI.

---

## Cross-Cutting Compliance Guardrails (per AGENTS.md)

- All patient data anonymized; **no PHI** in logs, errors, tests, memory files, or A2A payloads.
- OpenAI-compatible providers only (no Ollama).
- `ShellTools` kept disabled (no unsandboxed script execution).
- No pom dependency **version** changes without explicit human approval (the 0.8.0 bump was approved).
- No auto-merge of PRs / deploys without human approval.

## References

- Series Parts 1–7 and SDK links: see `docs/AGENTIC_PATTERNS_IMPROVEMENTS.md`.
- Spring AI Reference: https://docs.spring.io/spring-ai/reference/
