# M50: Fix Remaining Integration Test Failures

**Goal**: Fix the 19 remaining integration test failures that persist after M49 (down from 456), achieving zero failures in `mvn verify`.

**Context**:
- M49 fixed 437 test errors by adding `@ConditionalOnProperty(medexpertmatch.skills.enabled=true)` to `AgentToolCallingConfiguration`.
- Remaining failures fall into 3 categories:
  - **event-driven profile tests** (12 errors): `EventDrivenPipelineIT`, `EventPipelineMetricsIT`, `MultiAgentPipelineIT` — profile `event-driven` doesn't set `skills.enabled=true` so the pipeline beans fail to wire.
  - **Web test context failures** (7 errors): `ChatAgenticUxIT`, `ChatDataLifecycleIT`, `ChatE2ESmokeIT` — `@SpringBootTest(webEnvironment=MOCK)` tests with `test` profile have `skills.enabled=false` preventing agent ChatClient beans.
  - **SQL/baseline issues** (3 errors): `EvaluationServicePassRateIT` (BadSqlGrammar for `evaluation_dataset`), `HarnessRetentionRepositoryIT` (BadSqlGrammar for `llm_harness_chain_event`, `llm_harness_workflow_run` tables).
  - **ModulithVerificationIT** (1 error): Spring Modulith module verification fails (likely due to new dependency configuration).

## Steps

### 1. Fix event-driven profile tests

**Root cause**: `EventDrivenPipelineIT`, `EventPipelineMetricsIT`, `MultiAgentPipelineIT` use `@ActiveProfiles("event-driven")` which doesn't include `skills.enabled=true`. The profile likely needs `@ConditionalOnProperty` alignment.

**Fix**: Add `medexpertmatch.skills.enabled=true` to the `application-event-driven.properties` (or `application-event-driven.yml`) if it exists. Alternatively, annotate the tests with `@SpringBootTest(properties = {"medexpertmatch.skills.enabled=true"})` or add an `@ActiveProfiles` value that includes it.

### 2. Fix web test context failures

**Root cause**: Chat-related web tests (ChatAgenticUxIT, ChatDataLifecycleIT, ChatE2ESmokeIT) run with the `test` profile (`skills.enabled=false`) but need agent ChatClient beans (MedicalAgentConfiguration, AgentToolCallingConfiguration) which are gated by `skills.enabled=true`.

**Fix**: Check if these tests import `TestAIConfig` which provides `@ConditionalOnProperty(name = "medexpertmatch.skills.enabled", havingValue = "false", matchIfMissing = true)` fallback beans. If so, the test ChatClient beans should be sufficient. If not, update `TestAIConfig` or individual test configurations to provide the required beans.

### 3. Fix SQL/baseline issues

**Root cause**: `EvaluationServicePassRateIT` hits `evaluation_dataset` table (BadSqlGrammar), and `HarnessRetentionRepositoryIT` hits `llm_harness_chain_event` and `llm_harness_workflow_run` tables. These tables may not be created by Flyway V1 migration.

**Fix**: 
- Verify that `evaluation_dataset`, `llm_harness_chain_event`, and `llm_harness_workflow_run` exist in `V1__initial_schema.sql`.
- If not, add the missing table DDL to the V1 migration (following `db-migrations` skill — V1 consolidation only).
- Run `mvn clean verify` to confirm Flyway applies correctly.

### 4. Fix ModulithVerificationIT

**Root cause**: Spring Modulith module verification fails, potentially due to the M49 change adding `@ConditionalOnProperty` on `AgentToolCallingConfiguration` or related dependency graph changes.

**Fix**:
- Run `ModulithVerificationIT` in isolation and examine the failure report.
- If it's a missing dependency declaration, update `AgentToolCallingConfiguration`'s module `package-info.java` to declare `@ApplicationModule(allowedDependencies = {...})`.
- If it's a cyclic dependency, restructure accordingly.

### 5. `mvn verify` — zero failures

Run `mvn clean verify` and confirm all tests pass with zero failures.

### 6. Update CHANGELOG.md

Add M50 entry.

## Success Criteria

- [ ] Event-driven profile tests pass (12 tests)
- [ ] Web chat tests pass (7 tests)
- [ ] SQL/baseline tests pass (3 tests)
- [ ] ModulithVerificationIT passes
- [ ] `mvn verify` passes with zero test failures
- [ ] CHANGELOG updated
