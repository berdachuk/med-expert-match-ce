# MedExpertMatch: Harness Engineering Improvements Proposition

**Last updated:** 2026-05-31  
**Audience:** Engineering leads, LLM module owners, platform/ops  
**Status:** Strategic proposition — M29/M30 ✅ archived; next: [M31](../.agents/plans/M31-harness-human-checkpoint-and-events.md)

---

## Executive summary

A reliable medical AI product is not “a better model” alone. It is **LLM + harness**: the infrastructure, loops, validation, tooling, memory, and orchestration that turn probabilistic outputs into repeatable clinical-adjacent workflows.

MedExpertMatch already has substantial harness investment (workflow services, specialized chat agents, GraphRAG tools, A2A/chat governance milestones M08–M28). This document translates **harness engineering** patterns into a concrete improvement backlog for MedExpertMatch—distinct from **context engineering** (prompts, RAG, skills content).

**Core thesis:** Most production value and most lines of code should live in the harness. When the model fails, treat the trace as a **harness backlog item**, not as “the model was dumb.”

---

## 1. What harness engineering is (and why it beats model chasing)

### Definition

**Harness engineering** is the systematic design of wrappers around an LLM so it behaves as a dependable agent—not as smart autocomplete.

```
Coding / medical agent  ≈  LLM  +  Harness
```

In mature agent products, **90%+ of engineering effort** typically goes to:

| Harness responsibility | Examples in MedExpertMatch |
|------------------------|----------------------------|
| Task formulation | Workflow steps, `.st` prompts, skill routing (`ChatAgentProfile`) |
| Verification | `mvn verify`, eval datasets (planned), tool parameter sanitizers |
| Iteration loops | Tool call → retrieval → second LLM pass in matching workflows |
| State & memory | Chat sessions, `OrchestrationContextHolder`, job status enums |
| Environment integration | REST/A2A, Flyway, GraphService, CI scripts under `scripts/` |

The idea that “most of Claude Code is harness, not model” applies here: demo-quality medical matching comes from **architecture and process**, not from swapping the base model alone.

### Harness vs context engineering

| Question | Owner | MedExpertMatch examples |
|----------|--------|-------------------------|
| *What should the model know, and how do we pack it?* | **Context engineering** | GraphRAG retrieval, embeddings, `src/main/resources/skills/`, prompt `.st` files |
| *What process ensures a correct outcome step by step, even when the model errs?* | **Harness engineering** | Workflow services, tool guards, Ralph-style verify/fix loops, multi-agent routing |

Both are required. MedExpertMatch is relatively strong on context (RAG, skills, prompts) and should deliberately strengthen **process harness** next.

---

## 2. Three layers of AI architecture (mapped to MedExpertMatch)

Inspired by agentic architecture literature (e.g. Addy Osmani, Martin Fowler): three layers stack on top of the raw model.

### Layer 1 — Model layer

- OpenAI-compatible chat/completion APIs (project constraint: no Ollama).
- “Raw reasoning” with no business process attached.

### Layer 2 — AI layer (single session / single workflow run)

What happens around **one** user chat turn or **one** workflow invocation:

| Component | Purpose | Current / gap in MedExpertMatch |
|-----------|---------|----------------------------------|
| Rules & roles | System prompts, disclaimers, agent personas | `chat-agent-system.st`, `src/main/resources/agents/*.md` |
| Skills & tools | Callable capabilities | Split tool beans (`DoctorMatchingAgentTools`, etc.), `NormalizingToolCallbackResolver` |
| Hooks | Pre/post processing, guardrails | `LlmResponseSanitizer`, `MatchToolParameterSanitizer`, `AgentToolCaseIdValidator` |
| Sub-roles in one run | Planner / Coder / Critic modes | **Partial** — fixed pipelines in workflow impls; chat has profiles but not explicit critic pass |
| State | Plan, artefacts, step index | Chat DB; workflow session via context holder; **no persisted plan artefact per task** |

**Intuition:** Model = brain; AI layer = personality and habits; orchestration = software development process at scale.

### Layer 3 — Orchestration layer (multi-session / multi-agent)

- Multiple specialized agents, event-driven handoffs, sagas/state machines.
- MedExpertMatch: `MedicalAgentService` facade + per-domain workflow services; `ChatAgentProfile` (M14) for chat routing; A2A milestones for federation and governance.

**Gap:** Orchestration is largely **scripted pipelines** (see `docs/improvements-plan-agentic-patterns.md`) rather than dynamic state machines with explicit events (`PLAN_READY`, `TESTS_FAILED`, etc.).

---

## 3. Mindset: every LLM failure is a harness signal

When traces show recurring failures, convert them into harness work:

| Symptom | Harness response (generic) | MedExpertMatch-oriented response |
|---------|---------------------------|----------------------------------|
| Wrong tool name / malformed JSON args | Structured tool schema + normalizer | Extend `AgentToolNameNormalizer`, sanitizer tests |
| `caseId` confusion or hallucinated IDs | Validation + extraction | `CaseIdExtractor`, `ChatCasePromptSupport`, `AgentToolCaseIdValidator` |
| Match calls with bad graph parameters | Pre-flight sanitization | `MatchToolParameterSanitizer` |
| Answers not grounded in retrieval | Verification loop | Mandatory tool-before-answer step in workflow; eval scorer |
| PHI in logs | Policy hook | `LlmResponseSanitizer`, audit patterns from M20–M27 |
| “Forgot” clinical disclaimer | Critic checklist | Post-generation compliance pass on medical prompts |

Maintain a **harness improvement backlog** alongside the product backlog—classified by failure type, not by “model version.”

---

## 4. Harness patterns and loops (detailed)

### 4.1 Ralph Loop — primary coding-style iteration

Automated **Formulate → Change → Run → Observe → Fix → Stop/Repeat**:

1. **Formulate** — Small step with acceptance criteria and minimal context bundle.
2. **Change** — Structured output (diff, file ops, tool calls)—not prose-only.
3. **Run** — Apply in workspace; execute checks (`mvn test`, module-scoped verify, lint).
4. **Observe** — Parse output to structured errors, e.g. `{ type, file, line, message }`.
5. **Fix** — Model or critic receives errors + prior patch; generates corrective change.
6. **Stop** — Green checks, iteration limit, or human checkpoint.

**MedExpertMatch application**

| Domain | Ralph Loop analogue today | Proposed enhancement |
|--------|---------------------------|----------------------|
| **Repo development** (Cursor / CI) | Manual: agent runs `mvn verify` | Document Ralph Loop in `.agents/skills/testing`; optional CI job that posts structured failures back to agent context |
| **Medical agent workflows** | LLM → tools → LLM interpret | Add **Verify** step: schema/contract check on tool JSON + heuristic eval before final user-facing answer |
| **Prompt/skill changes** | Ad hoc | Eval module run (from `docs/improvements-plan-agentic-patterns.md`) as the **Run** step after every prompt change |

Implement Ralph as a **state machine in code**, not as “engineer copies build log into chat.”

### 4.2 Planner → Coder → Critic (single task)

Three roles, optionally same model with different prompts:

| Role | Input | Output |
|------|-------|--------|
| **Planner** | Issue, case text, retrieved context | Step list, touched modules, acceptance criteria |
| **Coder** | One step + scoped context | Patches / tool invocations |
| **Critic** | Patch + test results + policy checklist | Approve, reject with reasons, or request fix |

**MedExpertMatch application**

- **Chat:** After `ChatAgentProfile` selects skills, run a lightweight **Planner** pass for complex intents (“match from this narrative + explain rationale”) before tool-heavy **Coder** phase; **Critic** enforces disclaimer, no PHI, and “tool evidence cited.”
- **Workflows:** `MedicalAgentDoctorMatchingWorkflowServiceImpl` already sequences steps; refactor toward explicit planner artefact (JSON plan stored in session) and critic gate before `AgentResponse` return.

### 4.3 Context Builder (separate from “RAG in general”)

Dedicated step/agent that only **finds, ranks, compresses** context—never generates final clinical recommendations.

Steps:

1. Candidate retrieval (semantic + symbolic: case ID, ICD, graph neighbors).
2. Relevance ranking (`core` / `maybe` / `noise`).
3. Compression (summaries, token budget).
4. Handoff bundle to Planner/Coder.

**MedExpertMatch application**

- Align with `retrieval/` + `embedding/` + graph tools; expose a **`buildCaseContextBundle(caseId, intent)`** tool used by all workflows.
- Chat: `ChatCasePromptSupport` is a start; generalize to intent-aware bundles per `ChatAgentProfile`.

### 4.4 Guarded Patch / guarded tool scope

- **Scope:** Only allowed modules/files/tools for this task (e.g. intake may call `matchFromText`, not arbitrary graph admin).
- **Format:** Structured diffs or tool args, not free-form refactors.
- **Validation:** Dry-run, diff policy (no Flyway without human), secret/config deny lists.
- **Rollback:** Branch/PR per agent step; correlate with chat export bundles (M27+).

**MedExpertMatch application**

- Extend tool-level validators to **workflow-level scopes** (which `@Tool` methods each `ChatAgentProfile` may invoke).
- A2A: agent card declares allowed skills/tools; server rejects out-of-scope calls (builds on M20/M28 trust work).

### 4.5 Error as training signal for harness

Log each failed Ralph iteration:

- Task type, plan, patch/tool calls, commands, structured errors.
- Classify: import/contract misunderstanding, scope violation, flaky test, retrieval miss, policy breach.

For frequent classes, add harness elements (new critic rule, planner prerequisite, new tool).

**MedExpertMatch application**

- Structured workflow telemetry (no PHI): `MatchJobStatus` / analyse job enums + reason codes.
- Wire chat admin observability (M21–M27) to **harness failure taxonomy**, not only HTTP 5xx.

### 4.6 Multi-agent workflow (orchestration layer)

Event-driven pipeline with orchestrator state:

```
TASK_CREATED → PLAN_READY → CONTEXT_BUILT → TOOLS_EXECUTED → 
VERIFY_FAILED | VERIFY_PASSED → NEEDS_HUMAN → DONE
```

Specialized sessions/agents:

| Agent role | MedExpertMatch mapping |
|------------|------------------------|
| Planner / Architect | Routing planner, case analyzer (plan steps) |
| Researcher / Context builder | Evidence scout + retrieval services |
| Implementation | Domain workflow impls (not code-gen today) |
| Tester | Eval module + `mvn verify` for dev harness |
| Refactor / cleanup | N/A for runtime; relevant for dev agents |
| User-facing | Chat assistant / A2A surface |

Orchestrator routes by state: no plan → planner; plan approved → tool execution; tools done → critic/eval.

### 4.7 Human-in-the-loop checkpoints

Mandatory human review when:

- Planner output changes clinical routing or match thresholds.
- Large tool chains or low confidence scores.
- Harness cannot classify error.
- Regulatory-sensitive outputs (disclaimers present but evidence weak).

Policies: auto-continue for low-risk read-only queries; require approval for match/routing commits affecting production case records.

---

## 5. Current MedExpertMatch harness baseline

### Strengths (already invested)

- **Workflow facade** — `MedicalAgentService` with dedicated workflow services per domain.
- **Specialized chat agents** — `ChatAgentProfile`, runtime skills under `src/main/resources/skills/`, agent descriptors under `src/main/resources/agents/`.
- **Tool hardening (in progress)** — Normalized tool names, parameter sanitization, case ID validation, split tool classes per agent domain.
- **Safety** — `LlmResponseSanitizer`, anonymized cases, OpenAPI/A2A governance milestones.
- **GraphRAG context** — Retrieval and graph tools wired into matching and analysis.
- **Development harness** — `.agents/skills/`, TDD rules in `AGENTS.md`, verification scripts in `scripts/`.

### Gaps (harness-oriented)

| Gap | Impact | Related docs / milestones |
|-----|--------|---------------------------|
| Fixed pipelines, limited dynamic tool routing | Cannot adapt steps to case complexity | `docs/improvements-plan-agentic-patterns.md` |
| No automated eval loop on prompt changes | Quality regressions undetected | Eval module proposal (same doc) |
| No explicit Planner/Critic artefacts in chat/workflows | Hard to debug “why this answer” | This proposition §4.2 |
| Ralph Loop not formalized for agents | Errors require manual retry | §4.1 |
| Context bundling not a first-class agent step | Prompt bloat, inconsistent grounding | §4.3 |
| Harness failure taxonomy not unified | Ops sees errors, not improvement signals | M21–M27 observability |

---

## 6. Prioritized improvement roadmap for MedExpertMatch

Ordered by leverage and fit with existing modules. Effort is indicative (engineering days, not calendar).

### Phase A — AI layer hardening (single session)

| # | Initiative | Description | Effort |
|---|------------|-------------|--------|
| A1 | **Structured verify step** | After tool execution, run JSON schema + business rules (min matches, required fields) before final LLM narrative | 3–5d |
| A2 | **Context Builder tool** | `buildCaseContextBundle` used by all workflows and chat profiles | 4–6d |
| A3 | **Explicit Critic pass** | Second LLM call or rule engine: disclaimer, PHI scan, evidence linkage | 2–4d |
| A4 | **Planner artefact** | Persist plan + acceptance criteria per chat/workflow session (DB or export bundle) | 3–5d |
| A5 | **Tool scope by profile** | Enforce allowed tools per `ChatAgentProfile` / A2A agent card | 2–3d |

### Phase B — Ralph Loop for quality

| # | Initiative | Description | Effort |
|---|------------|-------------|--------|
| B1 | **Eval module** | YAML datasets + scorer + CI gate (from agentic improvements plan) | 5–8d |
| B2 | **Workflow iteration cap** | Max tool/LLM rounds with structured stop reasons | 1–2d |
| B3 | **Harness failure logging** | Reason codes on job status enums; admin metrics | 3–4d |

### Phase C — Orchestration layer

| # | Initiative | Description | Effort |
|---|------------|-------------|--------|
| C1 | **Workflow state machine** | Replace implicit scripts with explicit states/events for doctor match + intake | 8–12d |
| C2 | **Event-driven handoffs** | Optional: Modulith events between analysis → match → recommend | 6–10d |
| C3 | **Human checkpoints API** | Pause/resume workflow; attach clinician approval to case actions | 5–8d |

### Phase D — Development harness (engineering productivity)

| # | Initiative | Description | Effort |
|---|------------|-------------|--------|
| D1 | **Ralph Loop in `.agents/skills/testing`** | Standard: patch → `mvn test` → structured error feedback | 1d doc + templates |
| D2 | **Planner/Coder/Critic for Cursor** | Milestone plans as Planner output; PR checklist as Critic | 2d doc |
| D3 | **Harness backlog template** | Link trace IDs to harness tickets | 0.5d |

---

## 7. Architecture sketch (target state)

```mermaid
flowchart TB
  subgraph orchestration [Orchestration Layer]
    O[Workflow Orchestrator / State Machine]
  end

  subgraph ailayer [AI Layer - Single Run]
    P[Planner]
    CB[Context Builder]
    C[Coder - LLM + Tools]
    CR[Critic + Policy]
    V[Verify - Schema + Eval]
  end

  subgraph model [Model Layer]
    LLM[OpenAI-compatible LLM]
  end

  subgraph env [Environment]
    G[GraphService / Retrieval]
    DB[(Cases / Chat / Jobs)]
    CI[Eval + mvn verify]
  end

  User --> O
  O --> P
  P --> CB
  CB --> G
  CB --> C
  C --> LLM
  C --> G
  C --> V
  V --> CI
  V --> CR
  CR --> DB
  CR --> User
  V -->|fail| C
```

---

## 8. Success metrics

| Metric | Target |
|--------|--------|
| Eval pass rate on `medical-eval-v1` | ≥ baseline + 5% after each prompt change |
| Tool call failure rate (malformed args) | Down 50% after sanitization/normalization |
| Mean steps to successful match workflow | Stable or down with explicit planner |
| PHI/policy violations in stored chat | Zero (detected pre-persist) |
| Time to diagnose bad answer | ↓ via plan + tool trace artefacts |

---

## 9. References and related repository docs

| Resource | Relevance |
|----------|-----------|
| `docs/improvements-plan-agentic-patterns.md` | Eval module, session memory, orchestration context |
| `.agents/plans/M08-agentic-patterns-improvements.md` | Completed agentic milestone |
| `.agents/plans/M13-ai-chat-tab-and-specialized-agents.md` | Chat profiles and specialized agents |
| `.agents/plans/M14-ai-chat-agent-routing.md` | `ChatAgentProfile` routing |
| `src/main/java/.../llm/AGENTS.md` | Module constraints and conventions |
| `AGENTS.md` (root) | TDD, module map, global boundaries |
| Harness pattern catalogs (external) | e.g. `harn` repos, Addy Osmani / Martin Fowler agentic posts |

---

## 10. Recommended next steps

**Implementation plan:** M29–M30 complete. Next: [M31: Harness Human Checkpoint & Event Handoffs](../.agents/plans/M31-harness-human-checkpoint-and-events.md).

1. **Review Phase A** (M29 Steps 1–5) with LLM module owners.  
2. **Wire eval CI** (M29 Step 6) — builds on existing `llm/evaluation/`.  
3. **Doctor-match state machine pilot** (M29 Step 9) after verify/critic/bundle land.  
4. **Track harness backlog** using `.agents/templates/harness-backlog-item.md` (M29 Step 12).

---

*This proposition complements context/RAG work. Investment in harness process typically outperforms repeated model upgrades for reliability in regulated, tool-heavy domains like medical expert matching.*
