# Agentic Improvements Implementation Plan

Based on [analysis of ai-architect-6-agents](../docs/improvements-plan-agentic-patterns.md).

## Scope

5 improvements in dependency order. Item 6 (stub/live profiles) is deferred.

| # | Improvement | Files Changed | Est. Lines |
|---|---|---|---|
| 1 | OrchestrationContextHolder | 1 new, 3 modified | +30 / ~10 |
| 2 | Fix inline prompts -> .st | 3 new, 2 modified | +60 / ~20 |
| 3 | Session Memory (pom.xml + DB) | 2 modified | ~15 / ~40 |
| 4 | SessionMemoryAdvisor config | 1 modified | +25 |
| 5 | AutoMemory | 5 new, 2 modified | +210 / ~10 |
| 6 | Evaluation module | 6 new, 0 modified | +320 |

---

## Step 1: OrchestrationContextHolder

**Prerequisites:** None.

**Goal:** Decouple session ID propagation from `LogStreamService`. Provides ThreadLocal-based
context that `@Tool` methods and advisors can access independently.

### Files

| Action | File | Purpose |
|--------|------|---------|
| **NEW** | `src/main/java/com/berdachuk/medexpertmatch/llm/agent/OrchestrationContextHolder.java` | ThreadLocal<String> with `setSessionId`, `sessionIdOrNull`, `clear` |

This is a direct port from ai-architect (`agent/OrchestrationContextHolder.java`, 24 lines),
with package changed to `com.berdachuk.medexpertmatch.llm.agent`.

### File: OrchestrationContextHolder.java

```java
package com.berdachuk.medexpertmatch.llm.agent;

public final class OrchestrationContextHolder {

    private static final ThreadLocal<String> SESSION_ID = new ThreadLocal<>();

    private OrchestrationContextHolder() {}

    public static void setSessionId(String sessionId) {
        SESSION_ID.set(sessionId);
    }

    public static String sessionIdOrNull() {
        return SESSION_ID.get();
    }

    public static void clear() {
        SESSION_ID.remove();
    }
}
```

### Integration points

In each workflow service that currently calls:
```java
logStreamService.setCurrentSessionId(sessionId);
// ... workflow logic ...
logStreamService.clearCurrentSessionId();
```

Add wrapping:
```java
logStreamService.setCurrentSessionId(sessionId);
OrchestrationContextHolder.setSessionId(sessionId);
try {
    // ... workflow logic ...
} finally {
    OrchestrationContextHolder.clear();
    logStreamService.clearCurrentSessionId();
}
```

This preserves the existing `LogStreamService` propagation (required for log streaming to
clients) while adding a standalone context holder.

**Affected workflow services:**
- `MedicalAgentDoctorMatchingWorkflowServiceImpl.matchDoctors()` — already has try/catch
- `MedicalAgentQueuePrioritizationWorkflowServiceImpl.prioritizeConsults()` — already sets/clears
- `MedicalAgentCaseAnalysisWorkflowServiceImpl`
- `MedicalAgentRoutingWorkflowServiceImpl`
- `MedicalAgentRecommendationWorkflowServiceImpl`

In `MedicalAgentTools.getSessionId()` (line 76), swap delegation:
```java
// Before:
return logStreamService.getCurrentSessionId();

// After:
return OrchestrationContextHolder.sessionIdOrNull();
```

**Verification:** Run `mvn test -Dtest="*IT"` (existing integration tests pass unchanged).

---

## Step 2: Fix Inline Prompts -> .st Template Files

**Prerequisites:** None.

**Goal:** Eliminate the two inline `"""..."""` prompts in
`MedicalAgentLlmSupportServiceImpl` (lines 191-220), which violate AGENTS.md rule
"Invalid: Hardcoded prompt strings". Extract into `.st` template files following
the existing 15-template pattern in `PromptTemplateConfig`.

### Files

| Action | File | Purpose |
|--------|------|---------|
| **NEW** | `src/main/resources/prompts/routing-summarization.st` | Prompt for `summarizeRoutingResults()` |
| **NEW** | `src/main/resources/prompts/network-analytics-summarization.st` | Prompt for `summarizeNetworkAnalyticsResults()` |
| MODIFY | `src/main/java/com/berdachuk/medexpertmatch/core/config/PromptTemplateConfig.java` | Add 2 `@Value` fields + 2 `@Bean` definitions |
| MODIFY | `src/main/java/com/berdachuk/medexpertmatch/llm/service/impl/MedicalAgentLlmSupportServiceImpl.java` | Inject new templates, use `.render()` instead of `"".formatted()` |

### File: routing-summarization.st

Uses StringTemplate4 syntax with `<` / `>` delimiters (matching existing templates):

```
Summarize the following facility routing results for the user in 1-2 short paragraphs.
Use the case analysis for context. Focus on: recommended facilities, why they match, and any next steps.
Do not include tool names or procedural text. Write in plain language for a medical or operations reader.
Case analysis (context):
<caseAnalysis>

Routing tool results:
<rawToolResults>
```

### File: network-analytics-summarization.st

```
Summarize the following network analytics results for the user in 1-2 short paragraphs.
Focus on key findings: which conditions were analyzed, how many experts, and main metrics.
Do not include tool names, step numbers, code snippets, or procedural text.
Write in plain language for a medical or analytics reader.
Raw results:
<rawResults>
```

### PromptTemplateConfig changes

Add two `@Value` fields:
```java
@Value("classpath:/prompts/routing-summarization.st")
private Resource routingSummarizationResource;

@Value("classpath:/prompts/network-analytics-summarization.st")
private Resource networkAnalyticsSummarizationResource;
```

Add two beans:
```java
@Bean
@Qualifier("routingSummarizationPromptTemplate")
public PromptTemplate routingSummarizationPromptTemplate(StTemplateRenderer renderer) {
    return PromptTemplate.builder()
            .renderer(renderer)
            .resource(routingSummarizationResource)
            .build();
}

@Bean
@Qualifier("networkAnalyticsSummarizationPromptTemplate")
public PromptTemplate networkAnalyticsSummarizationPromptTemplate(StTemplateRenderer renderer) {
    return PromptTemplate.builder()
            .renderer(renderer)
            .resource(networkAnalyticsSummarizationResource)
            .build();
}
```

### MedicalAgentLlmSupportServiceImpl changes

Inject new templates:
```java
private final PromptTemplate routingSummarizationPromptTemplate;
private final PromptTemplate networkAnalyticsSummarizationPromptTemplate;
```

Refactor `summarizeRoutingResults()`:
```java
// Before:
String prompt = """...""".formatted(caseAnalysis, rawToolResults);
medGemmaChatClient.prompt().user(prompt).call().content();

// After:
String prompt = routingSummarizationPromptTemplate.render(Map.of(
    "caseAnalysis", caseAnalysis != null ? caseAnalysis : "",
    "rawToolResults", rawToolResults != null ? rawToolResults : ""));
medGemmaChatClient.prompt().user(prompt).call().content();
```

Same pattern for `summarizeNetworkAnalyticsResults()` with `networkAnalyticsSummarizationPromptTemplate`.

**Verification:** Run `mvn test -Dtest="*IT"`. No behavioral change — outputs must be
identical since the prompt text is unchanged, only the template engine renders it.

---

## Step 3: Add Session Memory Dependencies + DB Schema

**Prerequisites:** None.

**Goal:** Add `spring-ai-starter-session-jdbc` dependency and create `AI_SESSION` +
`AI_SESSION_EVENT` tables, enabling the `SessionMemoryAdvisor` in Step 4.

**Spring AI Deprecation Note:** Spring AI 2.0.x deprecated the legacy `ChatMemory`
interface and `MessageChatMemoryAdvisor`. The replacement is the community module
`org.springaicommunity:spring-ai-session-bom:0.2.0` with `SessionService` +
`SessionMemoryAdvisor`. This project currently uses neither — it is fully stateless.

### Files

| Action | File | Purpose |
|--------|------|---------|
| MODIFY | `pom.xml` | Add `spring-ai-session-bom:0.2.0` to `<dependencyManagement>`, add `spring-ai-starter-session-jdbc` dependency |
| MODIFY | `src/main/resources/db/migration/V1__initial_schema.sql` | Append `AI_SESSION` + `AI_SESSION_EVENT` tables |

### pom.xml changes

In `<dependencyManagement>` (before `</dependencyManagement>`):
```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-session-bom</artifactId>
    <version>0.2.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

In `<dependencies>` (after the `spring-ai-agent-utils` entry):
```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-starter-session-jdbc</artifactId>
</dependency>
```

### V1__initial_schema.sql changes

Append these tables at the end of the file (consolidated per project convention —
no incremental V2 for production/development):

```sql
CREATE TABLE IF NOT EXISTS AI_SESSION (
    id            VARCHAR(255)  NOT NULL PRIMARY KEY,
    user_id       VARCHAR(255)  NOT NULL,
    created_at    TIMESTAMP     NOT NULL,
    expires_at    TIMESTAMP,
    metadata      TEXT,
    event_version BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ai_session_user_id
    ON AI_SESSION (user_id);

CREATE INDEX IF NOT EXISTS idx_ai_session_expires_at
    ON AI_SESSION (expires_at);

CREATE TABLE IF NOT EXISTS AI_SESSION_EVENT (
    id              VARCHAR(255)  NOT NULL PRIMARY KEY,
    session_id      VARCHAR(255)  NOT NULL,
    "timestamp"     TIMESTAMP     NOT NULL,
    message_type    VARCHAR(20)   NOT NULL,
    message_content TEXT,
    message_data    TEXT,
    synthetic       BOOLEAN       NOT NULL DEFAULT FALSE,
    branch          VARCHAR(500),
    metadata        TEXT,
    CONSTRAINT fk_ai_session_event_session
        FOREIGN KEY (session_id) REFERENCES AI_SESSION (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_session_event_session_ts
    ON AI_SESSION_EVENT (session_id, "timestamp");
```

Note: `CREATE TABLE IF NOT EXISTS` ensures idempotency. For existing databases with
Flyway, a separate V2 migration may be needed instead of modifying V1. The test
container in `BaseIntegrationTest` drops and recreates the schema, so it picks up
V1 changes automatically.

**Verification:** Build: `mvn clean install -DskipTests`. The `spring-ai-starter-session-jdbc`
auto-configures `SessionService` via JDBC. Verify no startup errors with
`mvn spring-boot:run -Dspring-boot.run.profiles=local`.

---

## Step 4: Configure SessionMemoryAdvisor

**Prerequisites:** Step 3 (Session dependency + DB tables).

**Goal:** Register `SessionMemoryAdvisor` on the tool-calling `medicalAgentChatClient`
to compact conversation history for long workflows, preventing token overflow.

**Affected workflows:** Only workflows using `medicalAgentChatClient` (tool-calling
ChatClient) benefit:
- Queue Prioritization (primary path: deterministic, fallback: ChatClient)
- Recommendation (primary path: ChatClient with tools, fallback: ChatClient only)

Other workflows (doctor matching, case analysis, routing) call Java tool methods
directly, not through the tool-calling ChatClient, so session memory does not apply.
When those workflows later adopt tool-calling, they automatically inherit this.

### Files

| Action | File | Purpose |
|--------|------|---------|
| MODIFY | `src/main/java/com/berdachuk/medexpertmatch/llm/config/MedicalAgentConfiguration.java` | Add `sessionMemoryAdvisor` bean, register on ChatClient |

### MedicalAgentConfiguration changes

Add imports:
```java
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;
import org.springframework.ai.session.compaction.SlidingWindowCompactionStrategy;
import org.springframework.ai.session.compaction.TurnCountTrigger;
```

Add `sessionMemoryAdvisor` bean:
```java
@Bean
SessionMemoryAdvisor sessionMemoryAdvisor(SessionService sessionService) {
    return SessionMemoryAdvisor.builder(sessionService)
            .compactionTrigger(new TurnCountTrigger(15))
            .compactionStrategy(SlidingWindowCompactionStrategy.builder().maxEvents(30).build())
            .build();
}
```

Register on `medicalAgentChatClient` (line 133):
```java
// Before:
.defaultAdvisors(new SimpleLoggerAdvisor());

// After:
.defaultAdvisors(sessionMemoryAdvisor, new SimpleLoggerAdvisor());
```

Update `OrchestrationContextHolder` propagation in workflows that call
`medicalAgentChatClient`. Currently only `MedicalAgentQueuePrioritizationWorkflowServiceImpl`
(fallback path, line 124). Add `sessionId` propagation:
```java
String response = llmCallLimiter.execute(LlmClientType.TOOL_CALLING, () -> chatClient.prompt()
        .user(prompt)
        .advisors(a -> a.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY,
                OrchestrationContextHolder.sessionIdOrNull()))
        .call()
        .content());
```

**Verification:** Run queue prioritization with many cases and verify that session events
are written to `AI_SESSION_EVENT` table. Check that compaction triggers at turn 15.

---

## Step 5: AutoMemory Module

**Prerequisites:** Step 1 (OrchestrationContextHolder for session propagation to tools).

**Goal:** Add cross-session durable memory via `AutoMemoryTools` — filesystem-backed
typed entry files (`user.md`, `feedback.md`, `project.md`, `reference.md`) with global
`MEMORY.md` index. The LLM self-curates facts during workflows. Survives process restarts.

### Files

| Action | File | Purpose |
|--------|------|---------|
| **NEW** | `src/main/java/com/berdachuk/medexpertmatch/llm/automemory/AutoMemoryService.java` | Filesystem persistence for typed memories |
| **NEW** | `src/main/java/com/berdachuk/medexpertmatch/llm/automemory/AutoMemoryTools.java` | `@Tool` methods exposed to orchestrator |
| MODIFY | `src/main/java/com/berdachuk/medexpertmatch/llm/config/MedicalAgentConfiguration.java` | Wire `AutoMemoryTools` into ChatClient |
| MODIFY | `src/main/java/com/berdachuk/medexpertmatch/llm/package-info.java` | No change needed — automemory is within llm module |
| MODIFY | `src/main/resources/application.yml` / `application-local.yml` | Add `medexpertmatch.automemory.root` config |

### File: AutoMemoryService.java

Port of ai-architect `memory/AutoMemoryService.java` (114 lines), adapted:
- Package: `com.berdachuk.medexpertmatch.llm.automemory`
- Config path: `${medexpertmatch.automemory.root}` (default `~/.medexpertmatch/automemory`)
- Types: `user`, `feedback`, `project`, `reference`
- Methods: `appendEntry(type, markdownLine)`, `readEntries(type)`, `readAll()`, `readIndex()`
- Internal: `refreshIndex(type, preview)` appends timestamped entries to `MEMORY.md`
- All I/O via `java.nio.file.Files` (no external dependencies)

### File: AutoMemoryTools.java

Port of ai-architect `agent/AutoMemoryTools.java` (82 lines), adapted:
- Package: `com.berdachuk.medexpertmatch.llm.automemory`
- Uses `OrchestrationContextHolder.sessionIdOrNull()` for session ID
- Four `@Tool` methods:
  - `automemory_append(type, markdownLine)` — persist typed fact
  - `automemory_read(type)` — read by type (`"all"` for everything)
  - `automemory_index()` — view MEMORY.md index
  - `appendPreference(markdownLine)` — convenience for `user` type
  - `readPreferences()` — convenience for `user` type
- `@Tool(description = "...")` annotations guide the LLM on when to use each

### MedicalAgentConfiguration changes

Current ChatClient construction (line 123):
```java
ChatClient.Builder builder = ChatClient.builder(toolCallingChatModel)
        .defaultTools(fileSystemTools)
        .defaultTools(medicalAgentTools)
        .defaultAdvisors(sessionMemoryAdvisor, new SimpleLoggerAdvisor());
```

After change:
```java
ChatClient.Builder builder = ChatClient.builder(toolCallingChatModel)
        .defaultTools(fileSystemTools)
        .defaultTools(medicalAgentTools)
        .defaultTools(autoMemoryTools)  // NEW
        .defaultAdvisors(sessionMemoryAdvisor, new SimpleLoggerAdvisor());
```

Add `AutoMemoryTools` to constructor parameters:
```java
public ChatClient medicalAgentChatClient(
        @Qualifier("toolCallingChatModel") ChatModel toolCallingChatModel,
        @Qualifier("skillsTool") ToolCallback skillsTool,
        FileSystemTools fileSystemTools,
        MedicalAgentTools medicalAgentTools,
        AutoMemoryTools autoMemoryTools,           // NEW
        SessionMemoryAdvisor sessionMemoryAdvisor) // NEW
```

### application.yml changes

```yaml
medexpertmatch:
  automemory:
    root: ${MEDEXPERTMATCH_AUTOMEMORY_ROOT:${user.home}/.medexpertmatch/automemory}
```

### Design decisions

- **Within llm module, not separate module:** AutoMemoryTools is a tool used only by the
  LLM orchestrator, conceptually belonging with other tools (`MedicalAgentTools`,
  `SkillsTool`, `FileSystemTools`). Creates a sub-package `llm/automemory/` under the
  existing llm module. No new Modulith module or `package-info.java` needed.
- **Filesystem, not DB:** Follows ai-architect's deliberate choice. AutoMemory is
  human-readable Markdown files, survives DB resets, and requires zero schema changes.
- **No backward-incompatible changes:** Existing workflows continue unchanged. AutoMemory
  is an additive feature — the LLM may use it or ignore it.

**Verification:** Manual test with a workflow that triggers memory writes. Verify
`~/.medexpertmatch/automemory/MEMORY.md` grows across sessions. Integration tests should
not break (AutoMemory tools are passive — no behavioral changes unless the LLM chooses
to use them).

---

## Step 6: Evaluation Module

**Prerequisites:** Step 1 (OrchestrationContextHolder for clean session management
during eval runs).

**Goal:** Add automated LLM output quality measurement. YAML-based eval datasets,
heuristics-based scoring, JSON report output. Enables regression testing of prompt
changes and model upgrades.

### Files

| Action | File | Purpose |
|--------|------|---------|
| **NEW** | `src/main/java/com/berdachuk/medexpertmatch/llm/evaluation/EvalCase.java` | Record: id, type, caseId, expectedSpecialty, requiredFields, minMatches |
| **NEW** | `src/main/java/com/berdachuk/medexpertmatch/llm/evaluation/EvalDataset.java` | Record: datasetId, version, cases |
| **NEW** | `src/main/java/com/berdachuk/medexpertmatch/llm/evaluation/EvalDatasetLoader.java` | YAML -> EvalDataset |
| **NEW** | `src/main/java/com/berdachuk/medexpertmatch/llm/evaluation/EvalScorer.java` | Heuristics scoring per eval case |
| **NEW** | `src/main/java/com/berdachuk/medexpertmatch/llm/evaluation/EvaluationService.java` | Batch runner: iterates cases, calls MedicalAgentService, scores, builds JSON report |
| **NEW** | `src/main/resources/eval/medical-eval-v1.yaml` | Initial eval dataset (~10 cases) |

### File: EvalCase.java

```java
public record EvalCase(
    String id,
    String type,              // "doctor-match" | "case-analysis" | "facility-routing" | "queue-priority"
    String caseId,            // References a test case ID in the DB
    String expectedSpecialty, // e.g. "Cardiology"
    List<String> requiredFields, // e.g. ["doctor_name", "match_score", "specialty"]
    Integer minMatches        // Minimum expected matches
) {}
```

### File: EvalDataset.java

```java
public record EvalDataset(String datasetId, String version, List<EvalCase> cases) {}
```

### File: EvalDatasetLoader.java

Port of ai-architect `evaluation/EvalDatasetLoader.java` (37 lines), adapted for
med-expert-match eval case structure. Uses SnakeYAML to parse `classpath:` YAML.

### File: EvalScorer.java

Heuristics-based scoring:
- `doctor-match` type: checks response contains `expectedSpecialty`, contains
  `requiredFields` (doctor name, match score, specialty), has at least `minMatches`
  matches mentioned
- `case-analysis` type: checks response contains `requiredFields` (clinicalFindings,
  potentialDiagnoses, recommendedNextSteps)
- `facility-routing` type: checks response contains `requiredFields` (facility name,
  route score), has at least `minFacilities`
- `queue-priority` type: checks response contains urgency levels in correct order,
  has case IDs present

### File: EvaluationService.java

```java
@Service
public class EvaluationService {
    // run(datasetKey) -> JSON String
    // 1. Loads EvalDataset from classpath YAML
    // 2. For each EvalCase: creates fresh request params, calls MedicalAgentService,
    //    scores with EvalScorer, records pass/fail + reason codes
    // 3. Returns JSON report: dataset_id, pass_count, fail_count, per-case details
}
```

Uses `MedicalAgentService` interface directly (not REST API) to run within the same
JVM. Each case gets a generated `sessionId` for isolation. Clears `OrchestrationContextHolder`
between cases.

### File: medical-eval-v1.yaml

Initial dataset with ~10 cases covering all workflow types:

```yaml
dataset_id: medical-eval-v1
version: 1
cases:
  - id: cardio-match-01
    type: doctor-match
    case_id: "<from-test-data>"    # Populated by test data setup
    expected_specialty: "Cardiology"
    required_fields: ["doctor_name", "match_score", "specialty"]
    min_matches: 3
  - id: routing-01
    type: facility-routing
    case_id: "<from-test-data>"
    required_fields: ["facility_name", "route_score"]
    min_facilities: 2
  # ... etc
```

**Note on test data:** The YAML references `case_ids` that must exist in the database.
The eval can run either:
1. Against integration tests (Testcontainer has test data) — `EvalIT.java` integration
   test loads dataset and runs through `EvaluationService`
2. CLI mode against a running instance with test data — similar to ai-architect's
   `EvalCliConfiguration`

### Design decisions

- **Not a separate Modulith module:** Placed under `llm/evaluation/` within the existing
  `llm` module (similar to `llm/tools/`, `llm/config/`). Evaluation reads LLM outputs
  and scores them — it is tightly coupled to agent workflows. A separate module would
  create circular dependencies (eval imports workflow services, workflows return responses
  that eval scores).
- **Heuristics, not LLM-as-judge:** Follows ai-architect's practical approach. Regex/set
  matching is fast, deterministic, and repeatable. LLM-as-judge scoring can be added
  later as a second pass.
- **No REST endpoint initially:** The eval is a developer/maintainer tool. An
  `EvalCliConfiguration` (Spring profile `eval-cli`) can be added later following
  ai-architect's pattern.

**Verification:** Write an integration test `EvalIntegrationIT.java` that:
1. Populates test DB with known cases (using `MedicalCaseRepository`)
2. Loads `medical-eval-v1.yaml`
3. Runs `EvaluationService.run()`
4. Asserts pass_count > 0, fail_count == 0

Run: `mvn verify -Dtest=EvalIntegrationIT`

---

## Execution Order (Sequential Dependencies)

```
Step 1: OrchestrationContextHolder     [15 min, no deps]
    |
    v
Step 2: Fix inline prompts -> .st      [30 min, no deps, can parallel with Step 1]
    |
    v
Step 3: Session Memory deps + DB       [20 min, no deps, can parallel with Step 1-2]
    |
    v
Step 4: SessionMemoryAdvisor config    [20 min, depends on Step 3]
    |
    v
Step 5: AutoMemory module              [45 min, depends on Step 1]
    |
    v
Step 6: Evaluation module              [60 min, depends on Step 1]
```

Steps 1, 2, and 3 have no mutual dependencies and can execute in parallel.

---

## Rollback / Validation at Each Step

| Step | Validation Command | Success Criteria |
|------|-------------------|-----------------|
| 1 | `mvn test -Dtest="*IT"` | All existing integration tests pass |
| 2 | `mvn test -Dtest="*IT"` | All existing integration tests pass; prompt outputs identical |
| 3 | `mvn clean install -DskipTests && mvn spring-boot:run -Plocal` | No startup errors; Flyway applies cleanly |
| 4 | `mvn test -Dtest="MedicalAgentConfigurationTest"` | SessionMemoryAdvisor bean wires without error |
| 5 | `mvn test -Dtest="*IT"` | Existing tests pass; AutoMemory tools registered on ChatClient |
| 6 | `mvn verify -Dtest="EvalIntegrationIT"` | New eval test passes with score assertions |
