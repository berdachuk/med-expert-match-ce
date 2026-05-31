# Upgrade Spring AI 2.0.0-M2 to 2.0.0-M6

## Summary

Upgrade `spring-ai-bom` from `2.0.0-M2` to `2.0.0-M6` and `spring-ai-agent-utils` from `0.4.2` to `0.5.2` (`0.5.0` is the minimum, `0.6.0` latest). Adapt all code to M6 breaking changes: 4-arg `OpenAiEmbeddingModel` constructor, builder-only options classes, and configuration review.

---

## Compatibility Review

### M2 → M6 Breaking Changes (relevant to this project)

| Change | Impact | Files affected |
|---|---|---|
| `OpenAiEmbeddingModel` constructor: 3-arg → 4-arg (+ `RetryTemplate`) | **HIGH** - Compile error | `SpringAIConfig.java`, `EmbeddingEndpointPoolConfig.java` |
| Options classes: setter methods removed, builders only | **LOW** - Already using builders | None (verified) |
| `PromptChatMemoryAdvisor` removed | **NONE** - Not used | None |
| Configuration property names: `.options` suffix dropped | **LOW** - Using `spring.ai.custom.*` only | None (custom properties unaffected) |
| `ChatClient.builder().defaultFunctions()` → `defaultTools()` | **LOW** - Already using `.defaultTools()` | None (verified) |
| `OpenAiConnectionProperties` → `OpenAiCommonProperties` | **NONE** - Auto-config is disabled | None |
| `spring-ai-agent-utils` 0.4.2 → 0.5.2 | **MEDIUM** - API changes | `MedicalAgentConfiguration.java` (potential) |
| `PromptTemplate` deprecated constructors removed | **LOW** - Already using builder pattern | None (verified) |
| `StTemplateRenderer`: `supportStFunctions()` → `validateStFunctions()` | **NONE** - Not used | None |

### No-impact areas (verified by code audit)
- `ChatClient` fluent API (`.prompt()`, `.system()`, `.user()`, `.call()`, `.content()`) - unchanged
- `OpenAiApi.builder()` - unchanged
- `@Tool` / `@ToolParam` annotations - unchanged
- `ToolCallback` interface - unchanged
- `EmbeddingModel.embedForResponse()` - unchanged
- `ChatModel.call()` / `stream()` - unchanged
- `OpenAiChatModel.builder()` pattern - unchanged
- `PromptTemplate.builder().renderer().resource().build()` - unchanged
- Starter artifact IDs (`spring-ai-starter-model-openai`, `spring-ai-starter-vector-store-pgvector`) - unchanged
- Auto-config exclude class names (`org.springframework.ai.model.openai.autoconfigure.*`) - unchanged

---

## Implementation Steps

### Step 1: Update pom.xml

**File**: `pom.xml`

1. Change `spring-ai.version` property from `2.0.0-M2` to `2.0.0-M6` (line 29)
2. Change `spring-ai-agent-utils` version from `0.4.2` to `0.5.2` (line 117)

**Rationale**:
- `spring-ai-agent-utils` `0.5.0` is the minimum version compatible with Spring AI 2.0 (per project README: requires Spring AI 2.0.0-SNAPSHOT > M1). Version `0.5.2` is the latest stable. The `0.5.0` migration doc only changes `TaskToolCallbackProvider` → `TaskTool` (not used here); `SkillsTool` and `FileSystemTools` APIs remain identical.

### Step 2: Update OpenAiEmbeddingModel constructors (4-arg signature)

**Files**: `SpringAIConfig.java:179-182`, `EmbeddingEndpointPoolConfig.java:63-66`

The M5 → M6 transition to the official `openai-java` SDK requires a 4th `RetryTemplate` parameter:

**SpringAIConfig.java** (line 179-182):
```java
// BEFORE
OpenAiEmbeddingModel model = new OpenAiEmbeddingModel(
        embeddingApi,
        MetadataMode.EMBED,
        optionsBuilder.build());

// AFTER (add import org.springframework.ai.retry.RetryUtils)
OpenAiEmbeddingModel model = new OpenAiEmbeddingModel(
        embeddingApi,
        MetadataMode.EMBED,
        optionsBuilder.build(),
        RetryUtils.DEFAULT_RETRY_TEMPLATE);
```

**EmbeddingEndpointPoolConfig.java** (line 63-66):
```java
// BEFORE
OpenAiEmbeddingModel model = new OpenAiEmbeddingModel(
        api,
        MetadataMode.EMBED,
        optionsBuilder.build());

// AFTER (add import org.springframework.ai.retry.RetryUtils)
OpenAiEmbeddingModel model = new OpenAiEmbeddingModel(
        api,
        MetadataMode.EMBED,
        optionsBuilder.build(),
        RetryUtils.DEFAULT_RETRY_TEMPLATE);
```

### Step 3: Verify MedicalAgentConfiguration compatibility

**File**: `MedicalAgentConfiguration.java`

Check that `SkillsTool.builder()`, `FileSystemTools.builder()`, `ChatClient.builder().defaultTools()`, `.defaultAdvisors(new SimpleLoggerAdvisor())`, and `.defaultToolCallbacks()` compile correctly with v0.5.2. No changes expected based on the 0.5.0 migration docs (only `TaskTool` changed, not used here).

### Step 4: Verify compiles and tests pass

```bash
mvn clean compile
mvn test
mvn verify -DskipTests  # package phase
```

### Step 5: Run the full integration test suite

```bash
mvn verify
```

---

## Risk Assessment

| Risk | Likelihood | Mitigation |
|---|---|---|
| `OpenAiEmbeddingModel` constructor mismatch | Certain (API change) | Handled in Step 2 |
| `spring-ai-agent-utils` 0.5.2 incompatible with Spring AI 2.0.0-M6 | Low | 0.5.0+ requires Spring AI 2.0 (>M1) |
| Hidden API changes in `ChatClient` or `ChatModel` | Low | Code audit confirmed no usage overlap with removed APIs |
| Auto-config exclude class name changes | Low | Package structure unchanged through M6 |
| Test breakage from mock signature changes | Low | TestAIConfig.java uses Mockito mocks on interfaces (`ChatModel`, `EmbeddingModel`), unaffected |

---

## Rollback

If the upgrade breaks unexpectedly:
1. Revert `pom.xml` version properties back to `2.0.0-M2` / `0.4.2`
2. Revert `OpenAiEmbeddingModel` constructor changes (remove 4th arg)
3. Run `mvn clean test` to verify rollback

---

## Files Changed

1. `pom.xml` — version bumps (2 lines)
2. `src/main/java/.../core/config/SpringAIConfig.java` — `OpenAiEmbeddingModel` 4th arg + import (2 changes)
3. `src/main/java/.../embedding/config/EmbeddingEndpointPoolConfig.java` — `OpenAiEmbeddingModel` 4th arg + import (2 changes)
