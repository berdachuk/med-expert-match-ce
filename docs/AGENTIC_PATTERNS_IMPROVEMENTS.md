# Agentic Patterns Improvement Proposal — MedExpertMatch

> Roadmap for evolving **MedExpertMatch** (Spring Boot 4.0 + Spring Modulith 2.0 + Spring AI **2.0.0-M6** + GraphRAG) into a fully agentic medical-expert recommendation platform, based on the **Spring AI Agentic Patterns** series (Parts 1–7).

**Status date:** 2026-05-30
**Target module:** primarily `llm/`, with touch points in `retrieval/`, `web/`, and `core/`
**Toolkit:** `org.springaicommunity:spring-ai-agent-utils` + `spring-ai-session-*` (already on the classpath)

---

## 1. Executive Summary

MedExpertMatch already adopts several agentic building blocks ahead of this proposal:

- **Agent Skills (Part 1)** — runtime skills exist in `src/main/resources/skills/` (`case-analyzer`, `doctor-matcher`, `clinical-advisor`, `evidence-retriever`, `network-analyzer`, `recommendation-engine`, `routing-planner`).
- **AutoMemory (Part 6)** — `llm/automemory/AutoMemoryService.java` + `AutoMemoryTools.java` already present.
- **Session (Part 7)** — `spring-ai-session-bom` and `spring-ai-starter-session-jdbc` already declared in `pom.xml`.
- **Subagent foundations (Part 4)** — `MedicalAgentTools`, `OrchestrationContextHolder`, and 9 workflow services under `llm/service/`.

The biggest leverage now comes from the **three patterns not yet present** — **AskUserQuestionTool (Part 2)**, **TodoWriteTool (Part 3)**, and **A2A Integration (Part 5)** — plus **hardening and fully wiring** the patterns that exist only partially (formal `TaskTool` subagent registry, `SessionMemoryAdvisor` turn-safe compaction, AutoMemory consolidation triggers).

Each improvement below is scoped as its **own feature branch**, developed strictly under the new **TDD project rule** (test → verify test against requirements → implement → re-verify), and merged to `main` only after `mvn verify` passes.

---

## 2. Current-State Gap Matrix

| # | Pattern (Series Part) | Toolkit API | Current state in repo | Gap / Action |
|---|----------------------|-------------|------------------------|--------------|
| 1 | Agent Skills (P1) | `SkillsTool`, `FileSystemTools`, `ShellTools` | **Present** — 7 runtime skills in `resources/skills/` | Wire `SkillsTool` registry + progressive disclosure; add `clinical-guideline` & `triage` skills |
| 2 | AskUserQuestion (P2) | `AskUserQuestionTool`, `QuestionHandler`, `Question`, `Option` | **Missing** | Add interactive clarification to case-intake workflow (SSE → Thymeleaf UI) |
| 3 | TodoWrite (P3) | `TodoWriteTool`, `ToolCallAdvisor`, `MessageChatMemoryAdvisor` | **Missing** | Expose multi-step recommendation plan to UI via `TodoUpdateEvent` |
| 4 | Subagent Orchestration (P4) | `TaskToolCallbackProvider`, `TaskTool`, `ClaudeSubagentReferences` | **Partial** — manual workflow services, no formal registry | Convert workflow services into registered subagents w/ multi-model routing |
| 5 | A2A Integration (P5) | `spring-ai-a2a-server-autoconfigure`, `AgentCard`, `DefaultAgentExecutor` | **Missing** | Expose matching/evidence agents as A2A servers for hospital-network interop |
| 6 | AutoMemory (P6) | `AutoMemoryToolsAdvisor`, `AutoMemoryTools` | **Present** — service + tools exist | Add `AutoMemoryToolsAdvisor` wiring + consolidation triggers; typed memory files |
| 7 | Session (P7) | `SessionMemoryAdvisor`, `SessionService`, compaction triggers/strategies | **Partial** — deps present, advisor not wired | Wire `SessionMemoryAdvisor` w/ turn-safe compaction + branch isolation for subagents |

---

## 3. Per-Pattern Improvement Plan

### Feature 1 — `feat/agent-skills-registry` (Part 1)

**Goal:** Formalize the existing `resources/skills/` folder behind `SkillsTool` so the medical agent discovers and loads skills on demand with progressive disclosure (keeps the context window lean even as skill count grows).

**Changes**
- Register `SkillsTool.builder().addSkillsResource(classpath:skills)` in `MedicalAgentConfiguration`.
- Add `FileSystemTools` (Read) for skill reference files and keep `ShellTools` **disabled** (HIPAA/no-sandbox risk — see AGENTS.md forbidden list).
- Author two new skills: `clinical-guideline` (loads guideline references) and `triage` (urgency assessment).

**TDD test first**
- `MedicalAgentSkillsIT`: given a case-analysis prompt, assert the `Skill` tool is invoked with `case-analyzer` and that skill instructions reach the model (mock `ChatModel`, assert tool-call envelope).

---

### Feature 2 — `feat/ask-user-question-intake` (Part 2)

**Goal:** Turn the case-intake workflow from assumption-based into a clarifying agent — ask the clinician structured multiple-choice questions (urgency, specialty hint, prior history) before producing a match.

**Changes**
- Add `AskUserQuestionTool.builder().questionHandler(...)` to `MedicalAgentCaseIntakeWorkflowServiceImpl`.
- Web-app handler: bridge async UI via `CompletableFuture` — push `Question`/`Option` to the Thymeleaf UI over SSE, block on `future.get()`, complete from a REST endpoint when the clinician answers.
- Optionally expose the same via `@McpElicitation` for server-driven MCP scenarios.

**TDD test first**
- `CaseIntakeClarificationIT`: stub `QuestionHandler` returns canned answers; assert the workflow incorporates answers (no assumption defaults) and that unanswered/free-text paths are handled.

---

### Feature 3 — `feat/todowrite-plan-tracking` (Part 3)

**Goal:** Make the multi-step recommendation pipeline (analyze → retrieve evidence → match → rank → route) explicit and trackable; stream live progress to the UI.

**Changes**
- Register `TodoWriteTool.builder().todoEventHandler(...)` on the recommendation `ChatClient`.
- Pair with `ToolCallAdvisor.builder().conversationHistoryEnabled(false)` + `MessageChatMemoryAdvisor` (per Part 3 guidance) so todo updates persist in chat memory.
- Publish `TodoUpdateEvent` → `@EventListener` → SSE → progress bar in `web/`.

**TDD test first**
- `RecommendationTodoTrackingIT`: prompt with ≥3 steps; assert a todo list is created, exactly one task is `in_progress` at a time, and a completion event fires for each step.

---

### Feature 4 — `feat/task-subagent-orchestration` (Part 4)

**Goal:** Replace ad-hoc workflow-service chaining with a formal **Task tool** orchestrator + registered specialized subagents, each in an isolated context window with its own model.

**Changes**
- Define subagents as Markdown w/ YAML frontmatter under `src/main/resources/agents/`: `case-analyzer`, `evidence-retriever`, `doctor-matcher`, `network-analyzer`, `routing-planner`.
- Configure `TaskToolCallbackProvider` with `ClaudeSubagentReferences.fromRootDirectory("classpath:agents")`.
- **Multi-model routing**: route lightweight triage to a fast/cheap model and complex clinical reasoning (MedGemma) to the strong model via per-subagent `model:` field.
- Constraint: no `Task` in subagent `tools` (subagents cannot spawn subagents).

**TDD test first**
- `MedicalSubagentRoutingIT`: assert orchestrator delegates an "explore evidence" prompt to `evidence-retriever` only, and that each subagent receives an isolated context (no cross-leak of patient data).

---

### Feature 5 — `feat/a2a-interop-servers` (Part 5)

**Goal:** Expose MedExpertMatch capabilities as **A2A servers** so external hospital systems / partner agents can discover and call the matching and evidence agents over an open standard (HTTP + JSON-RPC).

**Changes**
- Add `spring-ai-a2a-server-autoconfigure` (test-scope first per pom dependency-version rule; promote with human approval).
- Publish an `AgentCard` (`/.well-known/agent-card.json`) describing skills: `doctor_match`, `evidence_search`.
- Implement `DefaultAgentExecutor` bridging the A2A request to the existing matching `ChatClient` + `MedicalAgentTools`.
- (Future) Host/router agent using `RemoteAgentConnections.sendMessage` for cross-institution delegation.

**TDD test first**
- `A2AAgentCardIT` + `A2ASendMessageIT`: assert the agent card is served at the well-known path with correct skills, and a JSON-RPC `sendMessage` returns a structured match result. **PHI must never appear in A2A payloads** — assert anonymization.

---

### Feature 6 — `feat/auto-memory-consolidation` (Part 6)

**Goal:** Fully wire the existing AutoMemory layer for durable, cross-session clinician/context memory with automatic consolidation.

**Changes**
- Add `AutoMemoryToolsAdvisor.builder().memoriesRootDirectory(${agent.memory.dir})` to the medical agent `ChatClient` (sandboxed Option A).
- Use typed Markdown memory files (`user`, `feedback`, `project`, `reference`) + `MEMORY.md` index — but **store only non-PHI** (clinician preferences, routing policies, NOT patient data).
- Add a `memoryConsolidationTrigger` (e.g., time-gap or "bye" predicate; or probabilistic ~5%).

**TDD test first**
- `AutoMemoryPersistenceIT`: write a clinician preference in session 1, assert recall in a fresh `SessionService` context; assert PHI-shaped strings are rejected/never persisted.

---

### Feature 7 — `feat/session-turn-safe-compaction` (Part 7)

**Goal:** Replace flat `ChatMemory` usage with the event-sourced **Session API** for turn-safe short-term memory, intelligent compaction, and multi-agent **branch isolation** (pairs with Feature 4).

**Changes**
- Wire `SessionMemoryAdvisor.builder(sessionService)` with `TurnCountTrigger(20)` + `TokenCountTrigger(4000)` via `CompositeCompactionTrigger.anyOf(...)`.
- Use `RecursiveSummarizationCompactionStrategy` for long clinical sessions; `TurnWindowCompactionStrategy` for short ones.
- Branch isolation: orchestrator `branch="orch"`, subagents `orch.evidence`, `orch.match` (via `EventFilter.forBranch`) so each subagent sees only its lineage.
- Add `SessionEventTools` (`conversation_search`) for keyword recall after compaction. Persist via JDBC tables `AI_SESSION` / `AI_SESSION_EVENT`.

**TDD test first**
- `SessionCompactionIT`: drive >20 turns; assert no orphaned tool results (kept window starts on USER), summary turns are searchable, and sibling-branch events are hidden.

---

## 4. TDD Project Rule (to be added to repo rules)

> **Always use TDD.** For every piece of functionality:
> 1. **Write the test first** — before any implementation code.
> 2. **Double-check the test against the requirements** using an internal review tool/agent (e.g., the `code-reviewer`/`testing` skill or a dedicated review subagent) to confirm it actually encodes the requirement.
> 3. **Only then implement** the functionality.
> 4. **Re-run the test** after implementation; fix problems and iterate until it passes (`mvn verify` green).

This will be added to `AGENTS.md` ("Key Rules at a Glance") and `docs/CODING_RULES.md`, and reinforced in the `testing` skill.

---

## 5. Sequencing & Branch Strategy

Each feature is implemented on its **own branch off `main`**, TDD-developed, and merged back after `mvn verify` passes.

| Order | Branch | Rationale |
|-------|--------|-----------|
| 0 | `feat/tdd-project-rule` | Establish the rule that governs all later work |
| 1 | `feat/session-turn-safe-compaction` | Memory backbone other patterns build on |
| 2 | `feat/auto-memory-consolidation` | Complements short-term session memory |
| 3 | `feat/agent-skills-registry` | Formalize existing skills |
| 4 | `feat/todowrite-plan-tracking` | Visible multi-step planning |
| 5 | `feat/ask-user-question-intake` | Interactive intake |
| 6 | `feat/task-subagent-orchestration` | Hierarchical agents (uses session branches) |
| 7 | `feat/a2a-interop-servers` | External interoperability (capstone) |

---

## 6. Compliance Guardrails (project-specific)

Per `AGENTS.md`, every feature above MUST:
- Keep **all patient data anonymized**; no PHI in logs, errors, tests, memory files, or A2A payloads.
- Use **OpenAI-compatible providers only** (no Ollama swap).
- Keep `ShellTools` disabled (no unsandboxed script execution).
- Not modify `pom.xml` dependency **versions** without explicit human approval (adding new **test** deps is allowed).
- Never auto-merge PRs or deploy without human approval.

---

## 7. Sources

- Part 1 — Agent Skills: https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills
- Part 2 — AskUserQuestionTool: https://spring.io/blog/2026/01/16/spring-ai-ask-user-question-tool
- Part 3 — TodoWriteTool: https://spring.io/blog/2026/01/20/spring-ai-agentic-patterns-3-todowrite/
- Part 4 — Subagent Orchestration: https://spring.io/blog/2026/01/27/spring-ai-agentic-patterns-4-task-subagents
- Part 5 — A2A Integration: https://spring.io/blog/2026/01/29/spring-ai-agentic-patterns-a2a-integration
- Part 6 — AutoMemoryTools: https://spring.io/blog/2026/04/07/spring-ai-agentic-patterns-6-memory-tools
- Part 7 — Session API: https://spring.io/blog/2026/04/15/spring-ai-session-management
- spring-ai-session: https://github.com/spring-ai-community/spring-ai-session
- Spring AI Reference: https://docs.spring.io/spring-ai/reference/
- ChatMemory API: https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/chat-memory.html
