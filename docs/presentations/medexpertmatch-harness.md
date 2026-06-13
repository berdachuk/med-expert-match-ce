---
revealjs:
  presentation: true
  height: 800
---

## Harness — the leash on the LLM

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**The model proposes; the harness constrains and executes.**

In MedExpertMatch, the **harness** is everything around the LLM that turns a chat message into a **reliable, observable**
medical workflow — not the model weights themselves.

**Docs:** [Harness Architecture](../HARNESS.md)

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-harness-metaphor.png" alt="Harness metaphor: guided control — dog on leash ready to move with direction" />

</div>

</div>

Note: Like a leash and harness, our layer gives direction and safety while the model (the dog) does the work. ~30 s.

---

## What the harness does

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **Goal identification** — what does the user want? (match, analyze, route, evidence)
- **Context assembly** — case bundles, session memory, case ID hints
- **Routing** — send work to the right workflow engine, not random tool calls
- **Execution & verify** — GraphRAG, tools, policy gate, retries
- **Safety** — no PHI in logs, medical disclaimers, sanitized replies

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-harness-metaphor.png" alt="Harness metaphor: controlled execution" />

</div>

</div>

Note: Contrast with “prompt only” agents that hope the model picks the right tool every time.

---

## High-level steps — one chat turn

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

1. **Receive** user message (any language)
2. **Translate** to English if needed (`ChatLanguageService` + MedGemma)
3. **Classify goal** — session rules → keywords → LLM fallback (`GoalClassifier`)
4. **Inject context** — case ID, recent history, PHI-safe bundle
5. **Route**
   - Match / route / analyze **with case ID** → **workflow engine**
   - Other goals → **FunctionGemma** Auto chat + tools
6. **Execute** — GraphRAG, domain tools, interpretation
7. **Verify** — optional policy gate + retry loop
8. **Reply** — translate back to user language; stream SSE progress

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-request-path.png" alt="Request path through harness and engines" />

</div>

</div>

Note: Walk through steps 3–5 slowly; that is where the hybrid classifier fixed «детализируй случай» routing.

---

## Step 1–2 — Intake and language

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**Receive** — `ChatAssistantServiceImpl` is the AI Chat entry point.

**Translate** — Russian and other non-English text is converted to English **before** classification and processing.
Original user text stays in chat history and the UI.

**Why?** One consistent pipeline for goal rules, keywords, and MedGemma prompts.

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-harness-metaphor.png" alt="Harness metaphor: prepare before the walk" />

</div>

</div>

Note: FunctionGemma on the Auto path also receives English prompts for tool consistency.

---

## Step 3 — Goal classification (hybrid)

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

`GoalClassifier` runs **before** any tool-calling or workflow engine.

| Layer | Example |
|-------|---------|
| Session continuation | «найди еще докторов» → `MATCH_DOCTORS` + same caseId |
| Keywords (EN + RU) | `detail the clinical case` → `ANALYZE_CASE` |
| LLM fallback | Ambiguous text + session context via MedGemma |
| Post-override | `GENERAL_QUESTION` + active case → analyze or match |

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-flow-steps.png" alt="Goal classification and flow steps" />

</div>

</div>

Note: Session memory (`ConversationGoalContext`, 30 min TTL) is the fix for follow-ups without re-pasting case text.

---

## Step 4–5 — Context and routing

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**Context** — `ChatCasePromptSupport` injects 24-char case ID; `CaseContextBundleService` builds MATCH / ANALYZE / ROUTE bundles.

**Route by goal + case ID:**

| Goal | Engine |
|------|--------|
| `MATCH_DOCTORS` | `DoctorMatchWorkflowEngine` |
| `ROUTE_CASE` | `RoutingWorkflowEngine` |
| `ANALYZE_CASE` | Case analysis workflow (harness) |
| Other | FunctionGemma Auto + `@Tool` methods |

**Key idea:** deterministic Java routing beats hoping the 270M tool model guesses right.

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-llm-tools.png" alt="LLM orchestration and tools behind the harness" />

</div>

</div>

Note: Link to [FunctionGemma](../FUNCTIONGEMMA.md) for the Auto path when harness does not take the turn.

---

## Step 6–7 — Execute and verify

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**Doctor match state machine** (SSE `HARNESS_STATE`):

`TASK_CREATED` → `PLANNING` → `CONTEXT_BUILT` → `TOOLS_EXECUTED` → `VERIFYING` → `POLICY_GATE` → `DONE`

Typical match steps:

1. MedGemma case analysis
2. `match_doctors_to_case` (GraphRAG hybrid scoring)
3. MedGemma interpretation of results
4. `AgentResponseVerifier` + optional policy gate

Follow-up «more doctors» excludes prior matches and broadens the pool.

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-demo-match.png" alt="Doctor match workflow demo visual" />

</div>

</div>

Note: Same pattern for routing and case analysis engines.

---

## Step 8 — Reply and observability

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**Reply** — localize to user language; stream tokens and harness events to the UI.

**SSE events:** `HARNESS_GOAL`, `HARNESS_PROGRESS`, `HARNESS_STATE`, `agent` (e.g. `doctor-match-harness`)

**Admin:** `/admin/harness-runs`, metrics `harness.verify.failure`

**Logs:**

`Goal classified: ANALYZE_CASE (caseId=…)` → `Routing to case analysis harness`

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-demo-dashboard.png" alt="Dashboard and observability" />

</div>

</div>

Note: Operators can trace a bad turn from goal log → engine → verify outcome.

---

## Model vs harness (quick reference)

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

| Harness (Java + config) | Model (LLM) |
|-------------------------|-------------|
| GoalClassifier, routing | MedGemma reasoning, translation |
| Workflow engines, GraphRAG | FunctionGemma tool choice (Auto path) |
| Verify, policy gate, retries | Case analysis narrative, interpretation |
| Session memory, case bundles | Prompt text from `.st` templates |

**6 role-separated LLM endpoints:** clinical-high, clinical-low, utility, tool-calling, embedding, reranking

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-harness-metaphor.png" alt="Harness metaphor: model under control" />

</div>

</div>

Note: See [Harness & Agent Patterns](../HARNESS_AND_AGENT_USAGE.md) for shared terminology with coding agents.

---

## Further reading

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- [Harness Architecture](../HARNESS.md) — full developer reference
- [FunctionGemma Tool Calling](../FUNCTIONGEMMA.md) — Auto path and fine-tuning
- [Find Specialist Flow](../FIND_SPECIALIST_FLOW.md) — end-to-end UX
- [chat-ops-runbook](../chat-ops-runbook.md) — operations and metrics

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-thank-you.png" alt="Thank you / next steps" />

</div>

</div>

Note: ~20 s. Offer to open HARNESS.md or run a live «detail case» follow-up demo.
