# M83: Add Depth to HARNESS_AND_AGENT_USAGE §2.2 (Medical LLM Orchestration)

**Status:** Active (planned 2026-06-09)
**Created:** 2026-06-09
**Depends on:** none — pure docs work; no code, test, or behavior changes.

## Problem Statement

`docs/HARNESS_AND_AGENT_USAGE.md` is a "patterns and alignment" doc that introduces the model-vs-harness vocabulary (§1), names two kinds of agents in this project (§2), and then maps the harness to the repo (§3) plus a list of practices (§5). §2.1 (Coding agents) is a thin pointer to `AGENTS.md`, but §2.2 (Medical LLM orchestration) is a single paragraph that says "Spring AI orchestration: `MedicalAgentService` and workflow services call a chat client, use `.st` files, and expose `MedicalAgentTools`." That is the only narrative a developer has for the runtime medical LLM pipeline.

The actual runtime is a **ten-layer pipeline** with two goals, a hybrid 5-stage goal classifier, three workflow engines (doctor match / routing / case analysis), a state machine (`TASK_CREATED → … → POLICY_GATE → NEEDS_HUMAN / DONE / FAILED`), a verifier, a confidence-policy router (`ANSWER/CLARIFY/ESCALATE/REFUSE`), a no-PHI policy gate, a language normalizer, three distinct LLM endpoints (clinical / utility / tool-calling), an SSE event pipeline with `token / agent / pipeline_stage / activity / done` events, and Prometheus metrics. None of that is in the doc. A new engineer onboarding to the chat subsystem has to read `HARNESS.md` + `ChatAssistantServiceImpl` + `GoalClassifier` + `DoctorMatchWorkflowEngine` to assemble a mental model.

§2.1 ("Coding agents") is also stale: it describes Cursor as the canonical coding agent, but the project has since added autonomous iteration capabilities that are a different kind of coding harness, and `.agents/skills/` is the canonical source of truth for coding-agent conventions. §2.1 is no longer the right pointer.

## Goal

1. **Remove §2.1** ("Coding agents (development time)"). Replace it with a one-line stub that points to the canonical sources (`AGENTS.md`, `.agents/skills/`, `HARNESS.md`) and notes the M83 archival.
2. **Expand §2.2** ("Medical LLM orchestration (runtime)") with:
   - **§2.2.1 — High-level flow**: a Mermaid `flowchart TD` diagram showing the user request → controller → rate limiter → sanitizer → language normalizer → goal classifier → routing decision → harness engines (plan / context / analyze / match / verify / confidence policy / human checkpoint / interpret / policy gate) OR non-harness chat path → localizer → SSE emitter → persistence + metrics. Boxes are labeled with the class name and the key detail (`LlmClientType`, prompt template, etc.).
   - **§2.2.2 — Layer-by-layer deep description**: a ten-layer narrative mirroring the diagram (HTTP ingress + rate limiting → orchestration entry + session binding → PHI sanitization → language normalization → goal classification (5-stage hybrid) → routing decision → harness state machine (doctor match / routing) → case analysis workflow → non-harness chat path → output / observability / persistence). Each layer names the service, the file, the contract, and any cross-cutting concerns (caching, rate limiting, instrumentation).
   - **§2.2.3 — LLM endpoints by `LlmClientType`**: a table of `CLINICAL / UTILITY / TOOL_CALLING / EMBEDDING / RERANKING` → chat-model bean → default model → where it is used in the harness.
   - **§2.2.4 — Skills (runtime prompts)**: a table of each `src/main/resources/skills/*/SKILL.md`, the `@Tool` methods it documents, and any special scoring weights or tier logic.
   - **§2.2.5 — Configuration**: a paragraph naming `MedicalAgentConfiguration`, `PromptTemplateConfig`, and the `LlmClientType` bean wiring + `AiConfigStartupValidator` fail-fast behavior.

3. **Leave §3, §4, §5, §6 untouched.** They are still correct summaries; the new §2.2 is the deep reference, and the old sections are the high-level "where in the repo" index. (Future M84+ may consolidate further if the doc grows unwieldy.)

## Non-Goals

| Don't | Why |
|---|---|
| Refactor §3, §4, §5, §6 | M83 is scoped to §2; the existing sections are still correct summaries. Future consolidation is a separate milestone. |
| Add new Mermaid diagrams to §3 or §4 | Same reason; each section owns its scope. |
| Change the runtime, prompts, or skills | M83 is docs-only. |
| Re-introduce a coding-agents section | The doc's audience (§1) is shared; the "two kinds" framing was misleading. The stub in §2.1 points to the right sources. |

## Changes

| Area | File | Change |
|------|------|--------|
| Doc body | `docs/HARNESS_AND_AGENT_USAGE.md` | Remove §2.1; expand §2.2 to ~190 lines with the Mermaid diagram and the five sub-sections above. |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Survey runtime to enumerate the ten layers and their service boundaries | Done (explored via the `explore` subagent) |
| 2 | Draft the Mermaid `flowchart TD` for §2.2.1 — every box must be a class/file path that exists in `src/main/java` or `src/main/resources/` today | Done |
| 3 | Write §2.2.2–§2.2.5 as narrative + tables, all cross-referenced to the diagram and the file paths | Done |
| 4 | Replace §2.1 body with a one-paragraph stub that points to `AGENTS.md` / `.agents/skills/` / `HARNESS.md` and notes the M83 archival | Done |
| 5 | Verify the file renders (no broken Mermaid, all internal links resolve) — MkDocs build is optional; visual eyeball is enough | Pending |
| 6 | Commit on feature branch; push; merge to develop; delete feature branch | Pending |
| 7 | Archive this M83 plan; update `00-index.md` to drop M83 from Active | Pending |

## Acceptance criteria

- [ ] `docs/HARNESS_AND_AGENT_USAGE.md` contains a Mermaid `flowchart TD` diagram that renders in MkDocs and names every layer of the runtime (user → controller → sanitizer → language → goal classifier → routing → harness engines OR chat path → localizer → SSE → metrics)
- [ ] §2.2.2 narrates all ten layers with service names + file paths + key contracts
- [ ] §2.2.3 lists all five `LlmClientType` values with the default model and at least one use-site
- [ ] §2.2.4 lists all nine `src/main/resources/skills/*/SKILL.md` runtime skills with their documented `@Tool` methods
- [ ] §2.2.5 names `MedicalAgentConfiguration`, `PromptTemplateConfig`, and the `AiConfigStartupValidator` fail-fast behavior
- [ ] §2.1 is a one-paragraph stub that points to canonical sources, with a link back to this M83 plan
- [ ] No code, test, prompt, or skill files are touched
- [ ] `mvn test` is not run (M83 is docs-only; no Java changes)

## Out of scope

- Refactoring §3, §4, §5, §6 into the new §2.2 — those sections are correct summaries
- Adding diagrams to other sections of the doc
- Documenting the iteration loop here (it lives in `AGENTS.md` and `docs/ai-context-strategy.md`)
- Documenting the M67/M68/M70/M71 sub-harnesses individually — they are referenced inline in §2.2.2

## References

- `docs/HARNESS_AND_AGENT_USAGE.md` — the file being changed
- `docs/HARNESS.md` — the high-level flow + workflow state machine (M66 introduced "Quick question vs Expert match" packaging here)
- `src/main/java/com/berdachuk/medexpertmatch/llm/service/impl/ChatAssistantServiceImpl.java` — the orchestrator
- `src/main/java/com/berdachuk/medexpertmatch/llm/chat/GoalClassifier.java` — the 5-stage hybrid classifier
- `src/main/java/com/berdachuk/medexpertmatch/llm/harness/DoctorMatchWorkflowEngine.java` — the state machine
- `src/main/java/com/berdachuk/medexpertmatch/llm/harness/MedicalAgentPolicyGateServiceImpl.java` — the PHI + disclaimer gate
- `src/main/java/com/berdachuk/medexpertmatch/llm/harness/MedicalConfidencePolicyServiceImpl.java` — the ANSWER/CLARIFY/ESCALATE/REFUSE router
- `src/main/resources/skills/*/SKILL.md` — the runtime skill prompts
- `src/main/resources/prompts/*.st` — the external prompt templates
- `src/main/resources/policy/medical-confidence-policy.yml` — the confidence policy rules
- `AGENTS.md` — the coding-agent contract that §2.1 used to point to
- `.agents/skills/` — the canonical development-only skills directory
- `docs/ai-context-strategy.md` — the iteration loop architecture section
