# MedExpertMatch Coding Rules

**Last Updated:** 2026-05-19

## Overview

This document describes coding rules and conventions for MedExpertMatch, with a focus on medical-specific adaptations.

## TDD Workflow (mandatory)

Always use TDD. Before implementing any functionality:

1. **Write the test first** — before any implementation code.
2. **Verify the test against the requirements** — use an internal review tool/skill (e.g. a code-review or testing skill, or a review subagent) to confirm the test truly encodes the requirement.
3. **Implement** the functionality — only after the test is written and verified.
4. **Re-run the test** (`mvn verify`) — fix problems and iterate until it passes.

## General Principles

- **Test-Driven Development**: Always follow TDD approach
- **Interface-Based Design**: All services and repositories use interfaces
- **Domain-Driven Design**: Clear module boundaries, domain language
- **SOLID Principles**: Single Responsibility, Open/Closed, etc.
- **Lombok First**: Use Lombok to simplify code and reduce boilerplate where appropriate

### Lombok Usage

- Prefer Lombok annotations (`@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@Builder`, `@Slf4j`) to collapse repetitive getters/setters, constructors, and logging setup.
- Use Lombok-generated constructors and builders for simple DTOs or configuration classes, avoiding hand-written boilerplate unless framework constraints prevent annotation usage.

## Error Handling & Null Safety

### Fail-Fast Null Checks

**Always validate parameters at method start**:

```java

@Override
public String generateDescription(MedicalCase medicalCase) {
    if (medicalCase == null) {
        throw new IllegalArgumentException("MedicalCase cannot be null");
    }
    // ... rest of method
}
```

**Benefits**:

- Fail fast with clear error messages
- Prevents NPE in catch blocks
- Makes debugging easier

### Error Logging in Catch Blocks

**Use safely computed variables in catch blocks**:

```java
String caseId = medicalCase.id() != null ? medicalCase.id() : "unknown";
long startTime = System.currentTimeMillis();

try{
        // ... operation
        }catch(
Exception e){
long duration = System.currentTimeMillis() - startTime;
    log.

error("Error processing case: {} | Duration: {} ms",caseId, duration, e);
// Use caseId, not medicalCase.id() - avoids NPE if medicalCase is null
}
```

**Anti-pattern** (avoid):

```java
try{
        // ...
        }catch(Exception e){
        log.

error("Error: {}",medicalCase.id(),e); // NPE risk if medicalCase is null
        }
```

### Duration Calculation

**Always calculate elapsed time correctly**:

```java
long startTime = System.currentTimeMillis(); // Declare outside try block

try{
// ... operation
long endTime = System.currentTimeMillis();
long duration = endTime - startTime;
}catch(
Exception e){
long duration = System.currentTimeMillis() - startTime; // Correct elapsed time
    log.

error("Error after {} ms",duration, e);
}
```

**Anti-pattern** (avoid):

```java
try{
        // ...
        }catch(Exception e){
long duration = System.currentTimeMillis(); // Wrong - this is current time, not elapsed!
    log.

error("Error after {} ms",duration, e);
}
```

### LLM Call Rate Limiting

**Single wrapper pattern** - Rate limiting should be handled by callers, not service implementations:

```java
// Service implementation - NO rate limiting wrapper
public String generateDescription(MedicalCase medicalCase) {
    // Direct LLM call - no llmCallLimiter wrapper
    return chatClient.prompt(prompt).call().content();
}

// Caller - handles rate limiting
public void processBatch(List<MedicalCase> cases) {
    for (MedicalCase
    case :
    cases){
        String description = llmCallLimiter.execute(LlmClientType.CLINICAL, () -> {
            return descriptionService.generateDescription(
            case);
        });
    }
}
```

**Anti-pattern** (avoid double wrapping):

```java
// Service - wraps with limiter
public String generateDescription(MedicalCase medicalCase) {
    return llmCallLimiter.execute(LlmClientType.CLINICAL, () -> {
        return chatClient.prompt(prompt).call().content();
    });
}

// Caller - wraps again (DOUBLE WRAPPING!)
public void processBatch(List<MedicalCase> cases) {
    for (MedicalCase
    case :
    cases){
        String description = llmCallLimiter.execute(LlmClientType.CLINICAL, () -> {
            return descriptionService.generateDescription(
            case); // Already wrapped!
        });
    }
}
```

**Rationale**: Rate limiting is infrastructure concern, not business logic. Service should focus on description
generation, callers manage concurrency.

### Session ID Propagation

**OrchestrationContextHolder pattern** - Session IDs propagate to `@Tool` methods and advisors via ThreadLocal, decoupled from `LogStreamService`:

```java
OrchestrationContextHolder.setSessionId(sessionId);
try {
    // workflow logic or ChatClient calls
} finally {
    OrchestrationContextHolder.clear();
}
```

Workflow services set the context at method entry and clear in a `finally` block. Fallback paths that clear/re-enter the parent `try` block must re-set the context before executing:

```java
try {
    OrchestrationContextHolder.setSessionId(sessionId);
    // ... primary path ...
} finally {
    OrchestrationContextHolder.clear();
}
// fallback: re-set before ChatClient usage
OrchestrationContextHolder.setSessionId(sessionId);
try {
    // ... fallback path ...
} finally {
    OrchestrationContextHolder.clear();
}
```

`@Tool` methods access the session ID via `OrchestrationContextHolder.sessionIdOrNull()`. This enables `AutoMemoryTools` and `SessionMemoryAdvisor` to associate actions with sessions.

### AutoMemory Conventions

- Memory types: `user`, `feedback`, `project`, `reference` (case-sensitive, validated by `AutoMemoryService`)
- Storage: `${user.home}/.medexpertmatch/automemory/` (configurable via `medexpertmatch.automemory.root`)
- Filesystem-backed Markdown, human-readable, survives DB resets
- `AutoMemoryTools` methods are registered on `medicalAgentChatClient` in `MedicalAgentConfiguration`
- Tests disable skills (`medexpertmatch.skills.enabled=false`), so `AutoMemoryTools` is not wired in test profiles

## Medical Domain Adaptations

### Entity Naming

- `Employee` → `Doctor`
- `Project` → `MedicalCase`
- `WorkExperience` → `ClinicalExperience`
- `Technology` → `MedicalSpecialty` / `ICD10Code`

### Medical-Specific Rules

1. **Patient Data Anonymization**: All patient identifiers must be anonymized
2. **HIPAA Compliance**: No PHI in logs or error messages
3. **Medical Disclaimers**: Add disclaimers about research/educational use
4. **ICD-10 Code Validation**: Validate ICD-10 codes against standard database
5. **Clinical Evidence**: Cite sources for clinical recommendations

## Related Documentation

- [Development Guide](DEVELOPMENT_GUIDE.md)
- [Architecture](pipeline/02-architecture.md)
- [Testing Guide](pipeline/04-testing.md)

---

*Last updated: 2026-01-23*
