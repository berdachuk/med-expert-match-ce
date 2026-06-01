# M35: Chat Conversation Context & Turn Continuity

Fix the broken follow-up turn flow where `GoalClassifier` loses track of the prior intent,
causing conversational responses like "yes" to fall through to the general-purpose LLM instead
of re-invoking the correct harness engine or tool chain.

**Prerequisite:** M34 in progress (no direct dependency on its artifacts; compatible with all completed plans).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|-------------|--------|--------|--------|
| 1 | `ConversationGoalContext` — session-keyed map for turn continuity (last goal, caseId) | `feat/m35-chat-turn-continuity` | ⬜ Planned | 3h |
| 2 | `GoalClassifier` follow-up detection — keyword + regex on short yes/more replies with sessionId guard | `feat/m35-chat-turn-continuity` | ⬜ Planned | 4h |
| 3 | `ChatAssistantServiceImpl` turn continuity wiring — set context after harness/LLM turns, clear on cleanup | `feat/m35-chat-turn-continuity` | ⬜ Planned | 4h |
| 4 | Integration test — full "Find Specialist … → yes" round-trip | `feat/m35-chat-turn-continuity` | ⬜ Planned | 4h |

**Total effort: ~15h**

---

## Problem Statement

```
Turn 1: "Find specialists for case 6a1db20e…" → GoalClassifier keyword match → MATCH_DOCTORS ✅ → Harness engine → Good response
Turn 2: "yes"                              → GoalClassifier no keyword → LLM fallback → GENERAL_QUESTION ❌ → Bad ad-lib response
```

Three sub-problems:

### P1: GoalClassifier treats each message independently

`GoalClassifier.classify("yes")` (`GoalClassifier.java:47`) matches zero keywords and falls through to
LLM classification, which correctly classifies it as `GENERAL_QUESTION` because a bare "yes" has no
domain intent. The classifier has no access to the prior turn's goal.

**Impact:** Turn 2 is always `GENERAL_QUESTION`, so `processViaHarnessEngine()` is never re-entered.

### P2: No case ID in the follow-up message

Even if P1 were fixed and the goal were correctly inherited as `MATCH_DOCTORS`, the follow-up "yes"
contains no case ID. The guard `goal.isRoutableToEngine() && goal.hasCaseId()`
(`ChatAssistantServiceImpl.java:104`) would still evaluate to `false`, skipping the harness engine.

**Impact:** Harness engine path is unreachable for any follow-up that doesn't contain a case ID.

### P3: Session memory gap between harness engine and LLM chat

`processViaHarnessEngine()` calls `chatService.appendAssistantMessage()` directly — it does not update
`SessionMemoryAdvisor` state. The `SessionMemoryAdvisor` is only used in the LLM-driven chat path
(`prepareTurn()` → `chatClient.prompt().advisors(...)`). When Turn 2 reaches the LLM, the session
memory may not include the Turn 1 doctor match results.

**Impact:** Even if the LLM were invoked with the correct goal hint and case ID, it may lack the
conversation context needed to produce a coherent follow-up.

---

## Design

### D1: ConversationGoalContext — session-keyed store

New class `llm/chat/ConversationGoalContext.java`.

**Why not ThreadLocal:** Each turn is a separate HTTP POST to
`/api/v1/chats/{chatId}/messages/stream`. The `CompletableFuture.runAsync()` in `streamMessage()`
(`ChatAssistantServiceImpl.java:131`) uses `ForkJoinPool.commonPool()`, which reuses threads across
unrelated requests. Turn 1 and Turn 2 will land on **different threads** in the overwhelming majority
of cases — a ThreadLocal would return `null` and follow-up detection would silently fail. Worse, if
thread reuse happens to give Turn 2 the same physical thread, User B could read User A's stale context.

**Solution:** Use a `ConcurrentHashMap<String, Entry>` keyed by `sessionId` (the same
`userId + "-" + chatId` key used throughout the chat flow), with a short TTL for automatic cleanup.

```java
package com.berdachuk.medexpertmatch.llm.chat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ConversationGoalContext {

    private static final ConcurrentMap<String, Entry> STORE = new ConcurrentHashMap<>();

    private static final long TTL_SECONDS = 60;

    private static final ScheduledExecutorService CLEANER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "goal-context-cleaner");
                t.setDaemon(true);
                return t;
            });

    static {
        CLEANER.scheduleAtFixedRate(
                STORE::clear, TTL_SECONDS, TTL_SECONDS, TimeUnit.SECONDS);
    }

    public record Entry(GoalType lastGoal, String lastCaseId, String sessionId) {}

    public static void set(GoalType goal, String caseId, String sessionId) {
        STORE.put(sessionId, new Entry(goal, caseId, sessionId));
    }

    public static Entry get(String sessionId) {
        return STORE.get(sessionId);
    }

    public static void clear(String sessionId) {
        STORE.remove(sessionId);
    }
}
```

Key properties:
- Keyed by `sessionId` — no cross-user contamination
- 60s TTL via daemon thread — stale entries auto-evicted; no unbounded growth
- `clear(sessionId)` called in `clearTurnContext()` for explicit cleanup after each turn
- Same pattern as the `sessionId` key used by `OrchestrationContextHolder` and `LogStreamService`

### D2: GoalClassifier follow-up detection

Modify `GoalClassifier.classifyByKeywords()` (`GoalClassifier.java:73`) to add a new check **before**
the existing keyword checks:

```java
GoalClassification classifyByKeywords(String message, Optional<String> caseId) {
    GoalClassification followUp = detectFollowUp(message, caseId);
    if (followUp != null) {
        return followUp;
    }

    String lower = message.toLowerCase();
    // … existing keyword checks unchanged …
}
```

New method:

```java
GoalClassification detectFollowUp(String message, Optional<String> caseId) {
    if (!isFollowUpSignal(message)) {
        return null;
    }
    String sessionId = OrchestrationContextHolder.sessionIdOrNull();
    if (sessionId == null) {
        return null;
    }
    ConversationGoalContext.Entry ctx = ConversationGoalContext.get(sessionId);
    if (ctx == null || ctx.lastGoal() == GoalType.GENERAL_QUESTION) {
        return null;
    }
    if (!sessionId.equals(ctx.sessionId())) {
        return null;
    }
    String inheritedCaseId = caseId.orElse(ctx.lastCaseId() != null ? ctx.lastCaseId() : "");
    return new GoalClassification(
        ctx.lastGoal(),
        inheritedCaseId.isEmpty() ? Optional.empty() : Optional.of(inheritedCaseId),
        Optional.empty(),
        "follow-up: " + ctx.lastGoal().name()
    );
}
```

**Single source-of-truth for follow-up signals** (`isFollowUpSignal`):

```java
private static final Set<String> FOLLOW_UP_AFFIRMATIVES = Set.of(
        "yes", "yeah", "yep", "ok", "okay", "sure", "go ahead", "please", "proceed", "continue");

private static final Pattern FOLLOW_UP_PREFIX = Pattern.compile(
        "^\\s*(more|other|another|next|show me more|show more|any other|additional)\\b.*",
        Pattern.CASE_INSENSITIVE);

private boolean isFollowUpSignal(String message) {
    if (message == null) return false;
    String trimmed = message.trim().toLowerCase();
    if (FOLLOW_UP_AFFIRMATIVES.contains(trimmed)) {
        return true;
    }
    if (FOLLOW_UP_PREFIX.matcher(trimmed).matches()) {
        return true;
    }
    if (trimmed.length() <= 20
            && !trimmed.matches(".*\\b(no|cancel|stop|quit|help|hello|hi|what|why|how|who|when|where)\\b.*")
            && !containsDomainKeyword(trimmed)) {
        return true;
    }
    return false;
}

private boolean containsDomainKeyword(String lower) {
    return lower.contains("find") || lower.contains("match") || lower.contains("doctor")
            || lower.contains("case") || lower.contains("analyze") || lower.contains("specialist")
            || lower.contains("facility") || lower.contains("route") || lower.contains("evidence")
            || lower.contains("pubmed") || lower.contains("icd") || lower.contains("triage")
            || lower.contains("urgency");
}
```

Design decisions:
- `FOLLOW_UP_AFFIRMATIVES` and `FOLLOW_UP_PREFIX` cover the explicit follow-up words. No overlap — each message
  matches at most one mechanism.
- The length-based fallback (≤20 chars) is gated by: no negation words (no/cancel/stop), no question words
  (what/why/how/who/when/where), and no domain keywords. This prevents false positives for short standalone
  messages like "help", "what about case B?", "find doctors".
- Session ID is validated **before** case ID inheritance (`sessionId.equals(ctx.sessionId())`) — not deferred.
  If the stored session doesn't match the current session, the method returns `null` (no follow-up).
- Fallback LLM classification is NOT modified — `classifyByLlm()` never sees follow-up messages because
  `classifyByKeywords()` returns non-null.

### D3: ChatAssistantServiceImpl wiring

Four modification points in `ChatAssistantServiceImpl.java`:

#### D3a: Set context after harness engine turn

In `processViaHarnessEngine()` (`ChatAssistantServiceImpl.java:326`), after appending the assistant message:

```java
ChatMessage assistantMessage = chatService.appendAssistantMessage(chatId, userId, engineResponse.response());
ConversationGoalContext.set(goal.goalType(), caseId, sessionId);
return Map.of("userMessage", userMessage, "assistantMessage", assistantMessage);
```

#### D3b: Set context after LLM chat turn

In `streamMessage()` (`ChatAssistantServiceImpl.java:124`), after `chatService.appendAssistantMessage()`:

```java
ChatMessage assistant = chatService.appendAssistantMessage(chatId, userId, reply);
ConversationGoalContext.set(ctx.goal().goalType(),
    ctx.goal().caseId().orElse(null), ctx.sessionId());
```

Note: requires modifying `prepareTurn` (`ChatAssistantServiceImpl.java:360`) to store `GoalClassification`
on the `TurnContext` record. The `TurnContext` record already exists at line 395.

#### D3c: Clear context in cleanup

In `clearTurnContext()` (`ChatAssistantServiceImpl.java:270`):

```java
private void clearTurnContext(String sessionId) {
    ChatToolContextHolder.clear();
    OrchestrationContextHolder.clear();
    ConversationGoalContext.clear(sessionId);
    logStreamService.clearCurrentSessionId();
}
```

#### D3d: Extend TurnContext to carry GoalClassification

At `ChatAssistantServiceImpl.java:395`, add `goal` field:

```java
private record TurnContext(
        ChatMessage userMessage,
        String sessionId,
        ChatAgentProfile profile,
        GoalClassification goal,
        String systemPrompt,
        String userPrompt) {}
```

Update `prepareTurn()` to pass `goal` through (the overload at line 360 already receives `GoalClassification` —
line 250's no-goal overload only exists in `processMessage()`). Update callsites in D3b to use `ctx.goal()`.

### D4: Session memory for harness engine

After `processViaHarnessEngine()` appends the assistant message to the DB, also add a lightweight
session-memory update so the `SessionMemoryAdvisor` sees the harness output if the next turn falls
back to LLM chat (non-routable goal):

```java
try {
    sessionService.addMessage(sessionId, Message.assistant(engineResponse.response()));
} catch (Exception ignored) { /* best effort */ }
```

This requires injecting `SessionService` into `ChatAssistantServiceImpl`. Since
`processViaHarnessEngine` is only called for `MATCH_DOCTORS`/`ROUTE_CASE` goals (which are the
majority of follow-up targets), this covers the critical path. For non-engine goals, the LLM chat
path already uses `SessionMemoryAdvisor` natively.

---

## Files

| Action | File | Purpose |
|--------|------|---------|
| **NEW** | `llm/chat/ConversationGoalContext.java` | Session-keyed `ConcurrentHashMap` holder for last goal + caseId, 60s TTL daemon cleanup |
| **MOD** | `llm/chat/GoalClassifier.java` | Add `detectFollowUp()`, `isFollowUpSignal()`, `FOLLOW_UP_AFFIRMATIVES` set, `FOLLOW_UP_PREFIX` pattern, `containsDomainKeyword()` |
| **MOD** | `llm/service/impl/ChatAssistantServiceImpl.java` | Set/clear `ConversationGoalContext`, wire goal into `TurnContext`, session memory update, inject `SessionService` |
| **NEW** | `src/test/java/.../llm/chat/GoalClassifierFollowUpTest.java` | Unit tests for follow-up detection: yes/more/other/next + context present/absent + sessionId mismatch |
| **NEW** | `src/test/java/.../llm/chat/ConversationGoalContextTest.java` | Unit tests for session-keyed set/get/clear + TTL eviction |
| **MOD** | `src/test/java/.../llm/chat/ChatCasePromptSupportTest.java` | Ensure existing case prompt tests still pass with new classifier logic |
| **NEW** | `src/test/java/.../llm/service/impl/ChatTurnContinuityIT.java` | Integration test: Turn 1 "Find specialist…" → Turn 2 "yes" → verify harness re-invoked |
| **NEW** | `src/test/java/.../llm/chat/ChatAssistantFollowUpTest.java` | Unit test: verify `ConversationGoalContext` is set + cleared per session in `processViaHarnessEngine` |

---

## Acceptance Criteria

1. **Follow-up "yes" after doctor match re-invokes harness engine.**
   - Turn 1: `"Find specialist case 6a1db20e…"` → `MATCH_DOCTORS` → harness engine → assistant response →
     `ConversationGoalContext.set(MATCH_DOCTORS, "6a1db20e…", sessionId)`
   - Turn 2: `"yes"` → `detectFollowUp()` → `ConversationGoalContext.get(sessionId)` returns entry →
     sessionId matches → inherits `MATCH_DOCTORS` + `caseId=6a1db20e…`
   - Turn 2 guard: `goal.isRoutableToEngine() && goal.hasCaseId()` → `true` → harness engine again
2. **Follow-up "more" after any goal inherits that goal.**
   - Same as above but for "more", "other doctors", "show more", "another"
3. **Non-follow-up short messages still classify correctly.**
   - `"no"`, `"cancel"`, `"help"` → NOT treated as follow-up (matched by negation/question-word guard)
4. **GoalClassifier keyword fast path still works for fresh requests.**
   - `"find specialist diabetes"` → `MATCH_DOCTORS` (not treated as follow-up because `ConversationGoalContext.get(sessionId)` returns null)
5. **ConversationGoalContext is cleared between turns.**
   - After each turn, `clearTurnContext()` calls `ConversationGoalContext.clear(sessionId)` — no stale context
6. **Session ID mismatch prevents cross-user contamination.**
   - If User A's entry has `sessionId = "userA-chat1"` and User B's request has `sessionId = "userB-chat2"`,
     the `sessionId.equals(ctx.sessionId())` check fails and `detectFollowUp()` returns `null`
7. **TTL eviction prevents unbounded memory growth.**
   - Entries older than 60s are cleaned by the daemon thread; `STORE.clear()` is a full clear each cycle
     since entries should be explicitly removed by `clearTurnContext()` before TTL expires
8. **Integration test passes.**
   - Full SSE round-trip: send "Find specialist…" → verify response contains doctor names → send "yes" → verify second response also contains doctor names (not generic ad-lib)
9. **No PHI in ConversationGoalContext.**
   - Only `GoalType`, case ID (24-char hex string), and `sessionId` (opaque string)

---

## Ship Order

**D1 → D2 → D3 → D4**

- D1: Holder exists but no producer calls `set()` → no behavioral change
- D2: Classifier calls `detectFollowUp()` which calls `ConversationGoalContext.get(sessionId)` → always
  returns null since nothing has been stored yet → safe no-op
- D3: Wires `set()` and `clear()` calls → full fix active
- D4: Session memory gap — additive, does not block D2+D3 from working

---

## Rollback

The `ConversationGoalContext` is isolated in a single new class with a static `ConcurrentHashMap`.
Misconfigurations are limited in scope:

- **Uncleared entries:** If `clearTurnContext()` is not called (exception skips finally), entries live up
  to 60s before the daemon thread cleans them. No permanent leak.
- **Wrong session ID:** The `sessionId.equals(ctx.sessionId())` check in `detectFollowUp()` rejects any
  entry from a different session. The worst case is a false negative (follow-up not detected) — not a
  false positive (wrong case inherited).
- **Daemon thread failure:** If the cleaner thread dies, `STORE` grows unboundedly. Mitigation: the
  `clear(sessionId)` call in `clearTurnContext()` removes entries immediately on the normal path.
  For the abnormal path, a `maxSize` guard can be added in a follow-up step (M36 candidate).

---

## Out of Scope

- Cross-device conversation continuity (requires DB-persisted conversation state — M36 candidate)
- Follow-up detection for non-English replies (non-critical for English-dominant medical UI)
- Re-classifying follow-ups with LLM (keyword-only is deterministic and faster)
- `ConversationGoalContext` max-size guard (TTL eviction is sufficient for normal operation;
  a Caffeine-based bounded cache can replace the `ConcurrentHashMap` + daemon thread in M36)
