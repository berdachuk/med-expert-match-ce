# M111: Core Framework Upgrades (Spring AI 2.0.0 GA + Boot 4.1.0)

**Status:** Active (planned 2026-06-14)
**Created:** 2026-06-14
**Depends on:** M110 (archived)

## Problem Statement

Codebase needs a coordinated upgrade of the core framework stack:

| Dependency | Current | Target | Reason |
|-----------|---------|--------|--------|
| Spring Boot | 4.0.6 | 4.1.0 | Required by Spring AI 2.0.0 GA; includes bugfixes, performance, and security improvements |
| Spring AI | 2.0.0-M8 | 2.0.0 | GA release on 2026-06-12; several breaking changes from M8 |
| Spring Modulith | 2.0.7 | 2.1.0 | Required by Spring Boot 4.1.0 |
| spring-ai-agent-utils | 0.8.0 | 0.9.0 | Community library compatibility with Spring AI 2.0.0 GA |

**Dependency chain:** Spring AI 2.0.0 GA requires Spring Boot 4.1.0 (per its release notes: "Upgrade to Spring Boot 4.1.0"). Spring Boot 4.1.0 requires a matching Spring Modulith version.

## Key API Changes

### Spring Boot 4.0.6 → 4.1.0
- Standard platform upgrade; verify deprecated API removals and configuration property changes
- Spring Framework 7.0 → 7.1 underpinnings
- Check for removed/renamed auto-configuration classes

### Spring AI M8 → GA

#### 1. `ToolCallAdvisor` → `ToolCallingAdvisor`
Renamed in RC1 with backward-compat shims that are removed in GA. Impact: `MedicalAgentConfiguration.java`.

#### 2. `internalToolExecutionEnabled` removed
Removed from `ToolCallingChatOptions` and all provider-specific options. Impact: `ToolSelectionLiveEvalService.java` sets `.internalToolExecutionEnabled(false)`.

#### 3. `streamToolCallResponses` removed
Removed from `ToolCallingAdvisor.Builder`, `ToolCallAdvisor.Builder`. Not used in our codebase.

#### 4. `ToolSpec` consumer API removed
`.tools(t -> t.callbacks(...).context(...))` API removed; replaced by `.tools(...)` + `.toolContext()`. Not used in our codebase.

#### 5. Advisor order changes
`Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER` changed. Memory advisors now sit outside `ToolCallingAdvisor` by default.

#### 6. Options immutability
Options are now strictly immutable. `copy()` and `fromOptions()` removed in favor of `mutate()`.

#### 7. `MethodToolCallbackProvider` throws `IllegalArgumentException`
Instead of `IllegalStateException`. Impact: test code that catches `IllegalStateException`.

### Community Libraries
- `spring-ai-agent-utils` 0.9.0: verify SkillsTool/TaskTool/AskUserQuestionTool API compatibility
- `spring-ai-session-bom` 0.3.0: verify compatibility with Spring AI 2.0.0 GA

## Goal

1. Upgrade `spring-boot-starter-parent` 4.0.6 → 4.1.0
2. Upgrade `spring-ai.version` 2.0.0-M8 → 2.0.0
3. Upgrade `spring-modulith.version` 2.0.7 → 2.1.0
4. Upgrade `spring-ai-agent-utils` 0.8.0 → 0.9.0
5. Fix `ToolCallAdvisor` → `ToolCallingAdvisor` rename
6. Fix `internalToolExecutionEnabled` removal
7. Verify session BOM compatibility
8. `mvn compile` green
9. `mvn test` green
10. `mvn verify` green

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Update `pom.xml`: Boot 4.1.0, Spring AI 2.0.0, Modulith 2.1.0, agent-utils 0.9.0 | Pending |
| 2 | Fix `ToolCallAdvisor` → `ToolCallingAdvisor` rename | Pending |
| 3 | Fix `internalToolExecutionEnabled` removal in `ToolSelectionLiveEvalService` | Pending |
| 4 | Fix `MethodToolCallbackProvider` exception type in tests | Pending |
| 5 | Verify agent-utils 0.9.0 API compatibility (SkillsTool, TaskTool) | Pending |
| 6 | Verify advisor order / session compatibility | Pending |
| 7 | `mvn compile` green | Pending |
| 8 | `mvn test` green | Pending |
| 9 | `mvn verify` green | Pending |
| 10 | Archive plan | Pending |

## Files to Modify

| File | Change |
|------|--------|
| `pom.xml` | 4 version bumps (Boot, Spring AI, Modulith, agent-utils) |
| `MedicalAgentConfiguration.java` | `ToolCallAdvisor` → `ToolCallingAdvisor` |
| `ToolSelectionLiveEvalService.java` | Remove `.internalToolExecutionEnabled(false)` |
| Test files | `IllegalStateException` → `IllegalArgumentException` if catching from `MethodToolCallbackProvider` |

## References

- Spring AI 2.0.0 GA: https://github.com/spring-projects/spring-ai/releases/tag/v2.0.0
- Upgrade notes: https://docs.spring.io/spring-ai/reference/upgrade-notes.html
- RC1 changelog: https://github.com/spring-projects/spring-ai/releases/tag/v2.0.0-RC1
- Spring Modulith 2.1.0: https://mvnrepository.com/artifact/org.springframework.modulith/spring-modulith-bom/2.1.0