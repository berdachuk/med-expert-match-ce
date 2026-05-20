# MedExpertMatch Agentic Improvements Plan

**Last Updated:** 2026-05-19

Analysis of [ai-architect-6-agents](https://github.com/berdachuk/ai-architect) for agentic patterns applicable to med-expert-match.

## Current State

med-expert-match implements a hardcoded multi-step pipeline: each workflow (doctor matching, case analysis, routing, etc.) follows a fixed sequence of LLM call -> tool execution -> LLM interpretation. The agent never dynamically selects tools or routes — it always follows the same script.

Key gaps:
- No automated quality evaluation for LLM outputs
- Fully stateless between sessions (no durable memory)
- Inline prompts in Java code violating project guidelines (`MedicalAgentLlmSupportServiceImpl` lines 191-220)
- No session memory window management (conversations grow unbounded during long workflows)
- Session propagation coupled to `LogStreamService`

**Note on Spring AI Chat Memory:** Spring AI 2.0.x deprecated the legacy `ChatMemory` interface and `MessageChatMemoryAdvisor` from `spring-ai-spring-boot-autoconfigure`. The replacement is the community `spring-ai-starter-session-jdbc` module (`org.springaicommunity:spring-ai-session-bom:0.2.0`) with `SessionService` + `SessionMemoryAdvisor`. med-expert-match uses neither the old nor the new approach — it is fully stateless. The improvements below use the new (non-deprecated) Session API where applicable.

---

## Improvements

### 1. Evaluation Module (highest impact)

**Status:** Not implemented.

**Problem:** No automated measurement of LLM output quality. Prompt changes, model upgrades, or tool modifications have zero quality regression detection.

**From ai-architect:**
- `evaluation/` module: YAML dataset loader (`EvalDatasetLoader`), heuristics scorer (`EvalScorer`), batch runner (`EvaluationService`)
- `EvalCase` record: `id, type, question, expectedCity, requiredFields, minHeadlines, requireSourceOrTime`
- `EvalDataset` record: `datasetId, version, cases`
- CLI entry point: `EvalCliConfiguration` for `--eval` flag on startup
- Output: JSON report with pass/fail counts and per-case reason codes

**Proposed for med-expert-match:**

New module `evaluation` under `llm/evaluation/`:

```
llm/evaluation/
├── EvalCase.java               # record(id, type, question, expectedSpecialty, requiredFields, minMatches, ...)
├── EvalDataset.java            # record(datasetId, version, cases)
├── EvalDatasetLoader.java      # YAML -> EvalDataset
├── EvalScorer.java             # Heuristics: specialty match, match count, required fields present
├── EvaluationService.java      # Runs dataset through MedicalAgentServiceImpl, scores each case, produces JSON report
├── MedicalEvalController.java  # REST endpoint: POST /api/v1/eval/run
└── EvalCliConfiguration.java   # CLI for offline batch runs
```

**YAML dataset format** (`src/main/resources/eval/medical-eval-v1.yaml`):

```yaml
dataset_id: medical-eval-v1
version: 1
cases:
  - id: cardio-match-01
    type: doctor-match
    question: "Match doctors for case <caseId>"
    expected_specialty: "Cardiology"
    min_matches: 3
    required_fields:
      - doctor_name
      - match_score
      - specialty
  - id: routing-01
    type: facility-routing
    question: "Route case <caseId> to facilities"
    min_facilities: 2
    required_fields:
      - facility_name
      - route_score
```

**Effort:** ~300 lines. Directly portable from ai-architect with domain-specific scorer extensions.

---


### 2. Cross-Session AutoMemory

**Status:** Not implemented.

**Problem:** Every session starts from zero. The agent cannot learn user preferences, remember feedback, or accumulate project context.

**From ai-architect:**
- `AutoMemoryService` (`memory/AutoMemoryService.java`, 114 lines): Filesystem-backed at root directory (`~/.meteoris-insight/automemory/`), typed entry files (`user.md`, `feedback.md`, `project.md`, `reference.md`), global `MEMORY.md` index
- `AutoMemoryTools` (`agent/AutoMemoryTools.java`, 82 lines): Four `@Tool` methods exposed to orchestrator LLM:
  - `automemory_append(type, markdownLine)` — persist a typed fact
  - `automemory_read(type)` — read entries by type
  - `automemory_index()` — view MEMORY.md index
  - `appendPreference(markdownLine)` / `readPreferences()` — convenience for `user` type
- Entries survive across sessions (durable beyond process restarts)
- LLM decides when to write (self-curating)

**Proposed for med-expert-match:**

New module `automemory`:

```
automemory/
├── AutoMemoryService.java      # Filesystem persistence, typed entry files, index
└── AutoMemoryTools.java        # @Tool methods: append, read, index
```

Configuration:

```yaml
medexpertmatch:
  automemory:
    root: ${user.home}/.medexpertmatch/automemory
```

**Usage scenarios:**
- Agent remembers "Dr. Smith prefers conservative treatment for geriatric patients"
- Agent stores feedback: "Patient responded better to physical therapy than surgical referral"
- Agent accumulates preference: "Prefer doctors with telehealth availability for follow-ups"

**Registration:** Add `AutoMemoryTools` to the `MedicalAgentConfiguration.medicalAgentChatClient` bean via `defaultTools(autoMemoryTools)`, similar to how `MedicalAgentTools` is already registered.

**Effort:** ~200 lines. Portable from ai-architect with `~/.meteoris-insight` path changed to `~/.medexpertmatch`.

---


### 3. Fix Inline Prompts -> .st Template Files

**Status:** Two methods in `MedicalAgentLlmSupportServiceImpl` use inline string prompts, violating the project's AGENTS.md rule:

```
src/main/java/com/berdachuk/medexpertmatch/llm/service/impl/MedicalAgentLlmSupportServiceImpl.java:

Line 191-199  summarizeRoutingResults()      — inline """...""" prompt
Line 213-220  summarizeNetworkAnalyticsResults()  — inline """...""" prompt
```

**Proposed:**

Extract both inline prompts into `.st` template files under `src/main/resources/prompts/`:

```
prompts/
├── routing-summarization.st              # System+user prompt for routing results
└── network-analytics-summarization.st    # System+user prompt for analytics results
```

Register in `PromptTemplateConfig` as `@Bean` with `@Qualifier`:

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

Then refactor `summarizeRoutingResults()` and `summarizeNetworkAnalyticsResults()` to use `PromptTemplate.render()` instead of `"".formatted()`.

**Effort:** ~30 minutes. No behavior change, zero risk.

---


### 4. OrchestrationContextHolder for Session Propagation

**Status:** Session ID propagation currently relies on `logStreamService.getCurrentSessionId()` / `logStreamService.setCurrentSessionId()`, coupling session context to logging infrastructure.

**From ai-architect:**
- `OrchestrationContextHolder` (`agent/OrchestrationContextHolder.java`, 24 lines): Clean `ThreadLocal<String>` with `setSessionId()`, `sessionIdOrNull()`, `clear()`
- Used in `OrchestrationService.exchange()` to set before ChatClient call and clear in `finally` block
- `@Tool` methods access it via `OrchestrationContextHolder.sessionIdOrNull()` independently of logging

**Proposed:** Create `OrchestrationContextHolder` in `llm/agent/` (24 lines, directly portable). Use it in workflow services instead of `logStreamService.getCurrentSessionId()`. `MedicalAgentTools` already has a `getSessionId()` method that delegates to `logStreamService` — swap to `OrchestrationContextHolder.sessionIdOrNull()`.

**Effort:** ~15 minutes. Backward-compatible.

---


### 5. Session Memory with SessionMemoryAdvisor

**Status:** No conversation memory window management. Long workflows (queue prioritization with many cases, multi-step case analysis with evidence retrieval + interpretation) grow unbounded context, leading to token overflow.

**Spring AI 2.0.x Deprecation Note:** The legacy `ChatMemory` interface (`InMemoryChatMemory`, `JdbcChatMemory`) and `MessageChatMemoryAdvisor` from `spring-ai-spring-boot-autoconfigure` are **deprecated** in Spring AI 2.0.x. The replacement is the community-provided `SessionService` + `SessionMemoryAdvisor` pattern:

- Dependency: `org.springaicommunity:spring-ai-starter-session-jdbc` (BOM: `spring-ai-session-bom:0.2.0`)
- Schema: `AI_SESSION` + `AI_SESSION_EVENT` tables (see `V2__spring_ai_session_api.sql` in ai-architect)
- `SessionMemoryAdvisor` replaces `MessageChatMemoryAdvisor`
- Configurable compaction: `TurnCountTrigger(n)` + `SlidingWindowCompactionStrategy`

**From ai-architect:**
- `LiveAgentConfiguration.sessionMemoryAdvisor()`: `SessionMemoryAdvisor` with `TurnCountTrigger(20)` and `SlidingWindowCompactionStrategy(maxEvents=40)`
- `orchestratorChatClient` registered with `.defaultAdvisors(sessionMemoryAdvisor)`
- Used in `OrchestrationService.exchange()` via `.advisors(a -> a.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId))`
- SQL migration: `V2__spring_ai_session_api.sql` (38 lines, drops legacy tables, creates `AI_SESSION` + `AI_SESSION_EVENT`)
- BOM dependency: `org.springaicommunity:spring-ai-session-bom:0.2.0`

**Proposed for med-expert-match:**

1. Add dependency to `pom.xml`:
   ```xml
   <dependencyManagement>
       <dependency>
           <groupId>org.springaicommunity</groupId>
           <artifactId>spring-ai-session-bom</artifactId>
           <version>0.2.0</version>
           <type>pom</type>
           <scope>import</scope>
       </dependency>
   </dependencyManagement>
   ```
   ```xml
   <dependency>
       <groupId>org.springaicommunity</groupId>
       <artifactId>spring-ai-starter-session-jdbc</artifactId>
   </dependency>
   ```

2. Add V1 migration update (`V1__initial_schema.sql`) with `AI_SESSION` + `AI_SESSION_EVENT` tables (consolidated per project rules — no incremental V2)

3. Configure `SessionMemoryAdvisor` bean in `MedicalAgentConfiguration`:
   ```java
   @Bean
   SessionMemoryAdvisor sessionMemoryAdvisor(SessionService sessionService) {
       return SessionMemoryAdvisor.builder(sessionService)
               .compactionTrigger(new TurnCountTrigger(15))
               .compactionStrategy(SlidingWindowCompactionStrategy.builder().maxEvents(30).build())
               .build();
   }
   ```

4. Register advisor on `medicalAgentChatClient`:
   ```java
   .defaultAdvisors(sessionMemoryAdvisor, new SimpleLoggerAdvisor())
   ```

5. Propagate `sessionId` in workflow services when using `medicalAgentChatClient`:
   ```java
   chatClient.prompt()
       .advisors(a -> a.param(SessionMemoryAdvisor.SESSION_ID_CONTEXT_KEY, sessionId))
       ...
   ```

**Note:** The `medicalAgentChatClient` is currently only used by Queue Prioritization and Recommendation workflows (and their fallbacks). Other workflows call Java tool methods directly (not through the LLM tool-calling ChatClient), so they do not need session memory. If those workflows later adopt tool-calling, they would automatically benefit.

**Effort:** ~200 lines. Requires: dependency addition, SQL migration, configuration bean, advisor registration.

---


### 6. Stub/Live AI Profile Switching

**Status:** Integration tests use Mockito mocks for `MedicalAgentTools`, `MedicalAgentLlmSupportService`, etc. This prevents catching integration-level LLM issues and makes tests tightly coupled to implementation details.

**From ai-architect:**
- `@Profile("stub-ai")` activates: `StubAiConfiguration`, `StubOrchestrationService`, `StubWeatherIntegration`, `StubNewsIntegration`
- `@Profile("!stub-ai")` activates: `LiveAgentConfiguration`, `OrchestrationService`, `OpenMeteoWeatherIntegration`, `KeylessNewsIntegration`
- `StubOrchestrationService` returns deterministic responses via keyword matching (no LLM dependency)
- All integration tests run with `stub-ai` profile by default in `application-test.yml`
- Deterministic pseudo-embeddings for pgvector testing (`NewsHeadlineEmbeddingEncoder.deterministicEmbedding()`)

**Proposed for med-expert-match:**

1. Add `application-test-stub.yml` with `stub-ai` profile activation
2. Create `StubMedicalAgentLlmSupportService` implementing `MedicalAgentLlmSupportService` that returns canned JSON for case analysis, result interpretation, routing summary, and analytics summary
3. Create `StubMedicalAgentConfiguration` (`@Profile("stub-ai")`) with the stub beans
4. Update existing integration tests to use `stub-ai` profile via `@ActiveProfiles` on `BaseIntegrationTest`

**Note:** This is the largest effort item and may be deferred — the current mock-based approach in `@SpringBootTest` with `@MockBean` already provides test isolation.

**Effort:** ~400 lines. Optional — current mock approach works but lacks integration-level coverage of the LLM boundary.

---


## Prioritization

| # | Improvement | Effort | Impact | Ready to Start |
|---|---|---|---|---|
| 1 | Evaluation module | Medium (~300 lines) | Critical — makes quality measurable | Yes |
| 2 | AutoMemory | Medium (~200 lines) | Significant — enables learning | Yes |
| 3 | Fix inline prompts -> .st | Low (~30 min) | Medium — fixes guideline violation | Yes |
| 4 | OrchestrationContextHolder | Low (~15 min) | Low — cleaner code, no behavior change | Yes |
| 5 | SessionMemoryAdvisor | Medium (~200 lines) | Medium — prevents token overflow | Yes, requires Session API dependency |
| 6 | Stub/live profiles | Large (~400 lines) | Medium — better test isolation | Deferred |

## Recommended Execution Order

1. **OrchestrationContextHolder** (15 min warm-up, establishes session propagation pattern)
2. **Fix inline prompts -> .st** (30 min, guideline compliance)
3. **Evaluation module** (core quality infrastructure — makes all future changes measurable)
4. **AutoMemory** (enables agent learning across sessions)
5. **SessionMemoryAdvisor** (prevents token overflow, requires Session API dependency)

Item 6 (stub profiles) is deferred — current mock-based test approach is adequate.
