# M57: Goal Classifier Hybrid Session Routing (Variant C)

**Status:** Archived (2026-05-31) — implemented; documented in `docs/HARNESS.md`  
**Created:** 2026-05-31  
**Depends on:** M41 (archived), M56 (archived) — partial fixes already landed; this milestone completes production-grade routing.

## Problem Statement

After a successful Find Specialist harness turn, follow-up messages in Russian (e.g. «детализируй клинический случай») are misclassified as `GENERAL_QUESTION` and routed to FunctionGemma tool-calling chat. The orchestrator then asks for case text that is already in session context.

Observed failure chain (chat `6a24230f…`, case `6a23f05200155d711484cf69`):

| Step | What happened |
|------|---------------|
| 1 | Keywords/follow-up missed Russian «детализируй клинический случай» (30 chars, no EN patterns) |
| 2 | LLM classifier (`medgemma1.5:4b`, `LlmClientType.CHAT`) returned `GENERAL_QUESTION` without caseId |
| 3 | `inheritSessionCaseId()` skipped — only applies to harness-routable goals (`MATCH_DOCTORS`, `ROUTE_CASE`) |
| 4 | `ChatCasePromptSupport` injected case ID into hints, but `Goal identified: GENERAL_QUESTION` in routing hint |
| 5 | FunctionGemma (`functiongemma:270m`) answered with text instead of calling `analyze_case(caseId)` |

## Goal

Implement **Variant C — Hybrid** classifier and chat routing:

1. **Session-first rules** — deterministic continuation and goal-shift detection (~80% of follow-ups)
2. **English keyword fast path** — keep existing patterns
3. **Session-aware LLM fallback** — only for ambiguous messages
4. **Post-classification safety overrides** — belt-and-suspenders before routing
5. **Chat prompt continuity** — history + inherited case ID for non-harness turns
6. **Optional harness route for `ANALYZE_CASE`** — stable medgemma workflow instead of fragile tool choice

## Non-Goals

- FunctionGemma fine-tuning (see **M58**)
- New LLM provider or model version changes in `pom.xml`
- UI changes beyond existing agent activity panel

## Architecture (Target)

```
User message
  │
  ▼
┌─────────────────────────────────────┐
│ 1. Session continuation rules (NEW) │  ← ConversationGoalContext + goal-shift
└─────────────────────────────────────┘
  │ miss
  ▼
┌─────────────────────────────────────┐
│ 2. Keyword fast path (existing + RU)│
└─────────────────────────────────────┘
  │ miss
  ▼
┌─────────────────────────────────────┐
│ 3. LLM classify w/ session context  │  ← medgemma 4b, enriched prompt
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ 4. Post-override rules (NEW)          │  ← session case + intent heuristics
└─────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────┐
│ 5. inheritSessionCaseId (expanded)   │
└─────────────────────────────────────┘
  │
  ├── MATCH_DOCTORS / ROUTE_CASE + caseId → Harness (existing)
  ├── ANALYZE_CASE + caseId → Harness analyze workflow (NEW, phase 2)
  └── other → FunctionGemma chat w/ historyBlock + case hints
```

## Root Causes Addressed

| # | Root cause | Fix in M57 |
|---|-----------|------------|
| R1 | No Russian analyze/detail patterns | D1 |
| R2 | No goal-shift (MATCH → ANALYZE) detection | D1 |
| R3 | `inheritSessionCaseId` too narrow | D2 |
| R4 | LLM classifier lacks session + history | D3 |
| R5 | `historyBlock` only for `follow-up:` summary | D4 |
| R6 | `ANALYZE_CASE` not harness-routed | D5 (phase 2) |
| R7 | `parseClassification` drops `ANALYZE_CASE` without message caseId | D2, D6 |

## Deliverables

### Phase 1 — Session rules + inheritance (P0, ~1 day)

#### D1: Session-first continuation and goal-shift rules

**Files:**
- `src/main/java/.../llm/chat/GoalClassifier.java`
- `src/test/java/.../llm/chat/GoalClassifierFollowUpTest.java`
- `src/test/java/.../llm/chat/GoalClassifierRussianTest.java` (new)

**New patterns (package-private for testing):**

| Intent | English examples | Russian examples |
|--------|------------------|------------------|
| Analyze/detail | `elaborate`, `detail the case`, `case summary` | `детализируй`, `опиши`, `разверни`, `подробнее`, `клинический случай`, `анализ случая` |
| More doctors | existing `MATCH_MORE_DOCTORS` | existing `RUSSIAN_MORE_DOCTORS` |
| Follow-up | existing `FOLLOW_UP_PHRASING` | `ещё`, `еще`, `расскажи подробнее`, `что ещё` |

**Goal-shift logic** (before generic follow-up):

When `ConversationGoalContext` has `lastCaseId` and message matches analyze/detail patterns:
- Return `ANALYZE_CASE` with inherited caseId
- Do **not** inherit prior `MATCH_DOCTORS` goal (explicit shift)

When message matches `requestsMoreDoctors()`:
- Return `MATCH_DOCTORS` with inherited caseId (existing behavior, extend RU)

**TDD:** Write `GoalClassifierRussianTest` first with cases:
- «детализируй клинический случай» after `MATCH_DOCTORS` → `ANALYZE_CASE` + caseId
- «найди еще докторов» after harness → `MATCH_DOCTORS` + caseId
- «детализируй» with no session → keyword miss → LLM path (mock)

#### D2: Expand `inheritSessionCaseId`

**File:** `GoalClassifier.java`

Current guard:
```java
if (!goal.isRoutableToEngine() || goal.hasCaseId()) return goal;
```

New behavior:
- If session has `lastCaseId` and goal has no caseId:
  - **Always inherit** for `ANALYZE_CASE`, `SEARCH_EVIDENCE`, `GENERATE_RECOMMENDATIONS`
  - **Inherit for `MATCH_DOCTORS` / `ROUTE_CASE`** when `lastGoal` matches OR summary indicates follow-up
  - **Inherit for `GENERAL_QUESTION`** only when `looksLikeCaseContinuation(message)` (shared with D6)
- Remove strict `ctx.lastGoal() != goal.goalType()` block for analyze/evidence goals

**Also fix `parseClassification`:**
```java
case "ANALYZE_CASE":
    return caseId.map(id -> GoalClassification.analyzeCase(id, summary))
            .orElse(GoalClassification.analyzeCase("", summary)); // allow session inherit
```

#### D6: Post-classification override

**File:** `GoalClassifier.java` — new method `applySessionOverrides(GoalClassification, String message)`

After `inheritSessionCaseId`, if still `GENERAL_QUESTION` and session has caseId:
- `looksLikeCaseDetailRequest(message)` → `ANALYZE_CASE` + inherited caseId
- `requestsMoreDoctors(message)` → `MATCH_DOCTORS` + inherited caseId

Log override: `Goal override: GENERAL_QUESTION → ANALYZE_CASE (session case present)`

---

### Phase 2 — LLM classifier enrichment (P1, ~1 day)

#### D3: Session-aware LLM classification prompt

**Files:**
- `src/main/resources/prompts/goal-classification.st`
- `GoalClassifier.java` — `classifyByLlm()`
- `src/test/java/.../llm/chat/GoalClassifierLlmContextTest.java` (new, mock ChatModel)

**Prompt additions:**
- Input variables: `lastGoal`, `lastCaseId`, `recentHistory` (last 4 messages, anonymized)
- Rules for continuation: «detail/elaborate case» with active case → `ANALYZE_CASE`
- Russian examples in prompt (not PHI)
- Rule: if session has caseId and user asks for more info about **the same case**, prefer `ANALYZE_CASE` over `GENERAL_QUESTION`
- Optional JSON field: `"useSessionCase": true` (parsed in `parseClassification`)

**`classifyByLlm` changes:**
```java
String classificationPrompt = goalClassificationUserTemplate.render(Map.of(
    "userMessage", message,
    "lastGoal", sessionGoal.orElse("none"),
    "lastCaseId", sessionCaseId.orElse("none"),
    "recentHistory", formatHistory(history, 4)
));
```

Keep `LlmClientType.CHAT` + `primaryChatModel` (medgemma 4b). Do **not** use FunctionGemma for goal routing.

---

### Phase 3 — Chat prompt continuity (P0, ~4h)

#### D4: Expand `historyBlock` injection

**Files:**
- `ChatAssistantServiceImpl.java` — `buildUserPrompt()`
- `src/test/java/.../llm/service/impl/ChatAssistantServiceImplHistoryTest.java` (new or extend existing)

Inject recent history when **any** of:
- `goal.summary().startsWith("follow-up:")` (existing)
- `goal.hasCaseId()` from inheritance
- `ConversationGoalContext.get(sessionId)` has `lastCaseId`

Cap: 6 messages, exclude current user message, truncate long assistant replies to 500 chars.

#### D4b: Routing hint for inherited analyze

When `goal.goalType() == ANALYZE_CASE` and caseId present:
```
Goal identified: ANALYZE_CASE | case ID: {id}
Call analyze_case with this exact case ID. Do NOT ask the user for case text.
```

**File:** `src/main/resources/prompts/chat-agent-orchestrator-instructions.st` — reinforce analyze_case when case ID in hints.

---

### Phase 4 — Harness route for case analysis (P1, ~1–2 days)

#### D5: Route `ANALYZE_CASE` + caseId to harness workflow

**Files:**
- `GoalClassification.java` — add `isAnalyzableViaHarness()` or extend routing predicate
- `ChatAssistantServiceImpl.java` — `processViaCaseAnalysisEngine()` / `streamViaCaseAnalysisEngine()`
- `MedicalAgentService.analyzeCase()` — already exists
- `src/test/java/.../llm/service/impl/ChatTurnContinuityE2ETest.java`

Mirror harness pattern from `processViaHarnessEngine`:
```java
if (goal.goalType() == GoalType.ANALYZE_CASE && goal.hasCaseId()) {
    return processViaCaseAnalysisEngine(...);
}
```

Benefits:
- Uses `MedicalAgentCaseAnalysisWorkflowService` + medgemma interpretation
- Avoids FunctionGemma `analyze_case` vs `analyze_case_text` ambiguity
- Consistent progress events in agent panel

Feature flag (optional): `medexpertmatch.harness.analyze-case-enabled: true` default on.

---

### Phase 5 — Regression eval set (P2, ~4h)

#### D7: Classification regression dataset

**Files:**
- `src/test/resources/eval/goal-classifier-cases.jsonl`
- `src/test/java/.../llm/chat/GoalClassifierEvalTest.java`

Format per line:
```json
{"message":"детализируй клинический случай","session":{"lastGoal":"MATCH_DOCTORS","lastCaseId":"6a23..."},"expect":{"goalType":"ANALYZE_CASE","hasCaseId":true}}
```

Minimum 30 cases: EN follow-up, RU follow-up, goal-shift, case-switch, explicit new goals, ambiguous.

Run in `mvn test` (not IT) — pure unit tests with session setup.

---

## File Touch List

| File | Change |
|------|--------|
| `GoalClassifier.java` | D1, D2, D3, D6 |
| `GoalClassification.java` | Optional routing helper |
| `goal-classification.st` | D3 |
| New `goal-classification-user.st` | D3 user prompt template |
| `PromptTemplateConfig.java` | Wire new template |
| `ChatAssistantServiceImpl.java` | D4, D5 |
| `chat-agent-orchestrator-instructions.st` | D4b |
| Tests (6 classes) | TDD per deliverable |

## TDD Order (mandatory)

1. `GoalClassifierRussianTest` — D1 goal-shift + RU patterns
2. `GoalClassifierFollowUpTest` — extend inherit for ANALYZE
3. `GoalClassifierLlmContextTest` — mock LLM receives session vars
4. `ChatAssistantServiceImplHistoryTest` — historyBlock conditions
5. `ChatTurnContinuityE2ETest` — «детализируй» → analyze harness or analyze_case tool
6. `GoalClassifierEvalTest` — JSONL regression

## Verification Checklist

Manual smoke (local profile, case `6a23f05200155d711484cf69`):

- [ ] Paste Find Specialist block → harness match (unchanged)
- [ ] «найди еще докторов» → harness with exclude + broaden search
- [ ] «детализируй клинический случай» → `ANALYZE_CASE`, no «provide case description»
- [ ] «tell me more about this case» after match → analyze or follow-up with history
- [ ] «find specialist for a different case» → context cleared, no inherited caseId
- [ ] Logs show `Goal classified: ANALYZE_CASE (caseId=6a23…)` not `GENERAL_QUESTION`

Automated:
- [ ] `mvn test -Dtest=GoalClassifier*`
- [ ] `mvn test -Dtest=ChatTurnContinuityE2ETest`
- [ ] `mvn verify` green

## Effort Estimate

| Phase | Effort |
|-------|--------|
| P0 — D1, D2, D4, D6 | ~1.5 days |
| P1 — D3, D5 | ~2 days |
| P2 — D7 | ~0.5 day |
| **Total** | **~4 days** |

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Over-aggressive session inherit attaches wrong case | Case-switch patterns clear context; explicit new goal keywords clear context |
| LLM latency on every ambiguous message | Session rules handle ~80%; LLM only on miss |
| Harness analyze duplicates doctor-match analysis step | Reuse cached `CaseAnalysisResult` if fresh (< TTL) |
| Russian regex maintenance | Complement with LLM fallback + eval JSONL |

## Success Metrics

| Metric | Baseline | Target |
|--------|----------|--------|
| RU «детализируй случай» → correct goal | 0% | 100% |
| Follow-up caseId inheritance | partial | 100% for active session |
| «Ask for case text» when case in session | frequent | 0% |
| GoalClassifier eval pass rate | n/a | ≥ 95% on JSONL set |

## Relationship to M58

M57 fixes **routing and context** (upstream). M58 fine-tunes FunctionGemma for **tool selection** (downstream). Implement M57 first; measure residual tool-calling errors before investing in M58.
