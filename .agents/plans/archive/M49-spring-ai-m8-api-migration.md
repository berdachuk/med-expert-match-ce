# M49: Spring AI M8 API Migration and AutoMemory Test Compatibility

**Goal**: Migrate deprecated Spring AI M8 APIs, fix `AutoMemoryTools` `NoSuchBeanDefinitionException` in test context, and restore full integration test suite.

**Context**:
- M48 complete: GeoDistance utility extracted, reranking prompt externalized, 11 dependency versions bumped to Spring AI 2.0.0-M8.
- M8 introduces `defaultToolCallbacks` deprecation — `MedicalAgentConfiguration:374` still uses the legacy API.
- `AutoMemoryTools` and `AutoMemoryService` use `@ConditionalOnProperty(name = "medexpertmatch.skills.enabled")`, but are required as non-nullable constructor params in `medicalAgentChatClient()` and `toolCallbackResolver()`. In minimal test contexts where the property is not set (or explicitly set to `false`), this causes `NoSuchBeanDefinitionException` → cascading ApplicationContext failure → 16 integration tests fail.
- No `@Disabled` tests, no `// TODO/FIXME`, no remaining hardcoded LLM prompts.

## Steps

### 1. Replace deprecated `defaultToolCallbacks` with `defaultTools`

**File**: `llm/config/MedicalAgentConfiguration.java`

- Line 374: Replace `builder.defaultToolCallbacks(skillsTool, taskTool)` with `builder.defaultTools(skillsTool, taskTool)`
- Remove the `try/catch` wrapper (lines 372-378) — `defaultTools` accepts `ToolCallback` instances directly without risk of throwing when `SkillsTool` isn't configured; the framework handles missing tools gracefully.
- The `@Qualifier("skillsTool")` and `@Qualifier("taskTool")` params remain as-is since they're already resolved via `ObjectProvider` in the bean provider.

### 2. Fix `AutoMemoryTools` NoSuchBeanDefinitionException in test context

**Root cause**: `medicalAgentChatClient()` and `toolCallbackResolver()` both require `AutoMemoryTools` as a non-nullable constructor parameter. When `@ConditionalOnProperty(medexpertmatch.skills.enabled=false)` excludes the bean, Spring can't wire these methods.

**Fix**:
- Mark `AutoMemoryTools` parameter as `@Nullable` in both:
  - `MedicalAgentConfiguration.medicalAgentChatClient()` (line 346)
  - `AgentToolCallingConfiguration.toolCallbackResolver()` (line 49)
- In `medicalAgentChatClient()`: if `autoMemoryTools` is null, skip adding it to `defaultTools` and skip `autoMemorySystemPromptTemplate` for the default system prompt (use `Collections.emptyMap()` with a bare template instead).
- In `toolCallbackResolver()`: if `autoMemoryTools` is null, skip adding it to the `toolObjects` list passed to `MethodToolCallbackProvider`.
- No changes needed to `@ConditionalOnProperty` annotations — the conditional logic remains correct; we just handle the null case gracefully.

### 3. Add `@ConditionalOnProperty` fallback bean for test profiles

Create a no-op `AutoMemoryTools` bean in `TestAIConfig` (or a separate `@TestConfiguration`) that activates when `medexpertmatch.skills.enabled=false`:

- Alternative: Add `medexpertmatch.skills.enabled=true` to `BaseIntegrationTest`'s `@SpringBootTest(properties = {...})` to ensure the property is always set. Simpler but less flexible.
- **Better**: Create a `@TestConfiguration` `AutoMemoryTestConfig` with `@ConditionalOnProperty(name = "medexpertmatch.skills.enabled", havingValue = "false")` that provides a stub `AutoMemoryTools` returning empty responses. This preserves the ability to test with skills disabled while avoiding the wiring failure.

### 4. Verify `AgentToolCallingConfiguration` M8 compatibility

Check that the manual `MethodToolCallbackProvider.builder().toolObjects(...).build().getToolCallbacks()` pattern still works correctly with M8's built-in `ToolCallbackProvider` auto-discovery. Remove this manual registration if M8 auto-discovers `@Tool`-annotated beans:

- Read M8 release notes for `@Tool` auto-discovery behavior
- Verify no duplicate tool registrations occur (each `ToolCallback` name should appear exactly once)
- If auto-discovery works, simplify `toolCallbackResolver()` to use the auto-discovered beans directly

### 5. `mvn verify` — full test suite pass

Run `mvn verify` and confirm all 210+ tests pass (including the 16 previously-failing ITs):
- `MatchingServiceIT` (12 tests)
- `SemanticGraphRetrievalServiceIT` (8 tests)
- `ComprehensiveHealthIndicatorIT`
- `AdminDashboardWebControllerIT`
- `CaseAnalysisControllerIT`

### 6. Update CHANGELOG.md

Add M49 entry documenting the M8 API migration and test compatibility fix.

## Success Criteria

- [ ] `defaultToolCallbacks` replaced with `defaultTools` on line 374; `try/catch` removed
- [ ] `AutoMemoryTools` nullable in `medicalAgentChatClient()` and `toolCallbackResolver()`; null-safe wiring implemented
- [ ] All 16 previously-failing ITs pass
- [ ] `mvn verify` passes with zero test failures
- [ ] No new deprecation warnings from M8 API usage
- [ ] CHANGELOG updated
