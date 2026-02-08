# MedExpertMatch Coding Rules

**Last Updated:** 2026-01-23

## Overview

This document describes coding rules and conventions for MedExpertMatch, with a focus on medical-specific adaptations.

## General Principles

- **Test-Driven Development**: Always follow TDD approach
- **Interface-Based Design**: All services and repositories use interfaces
- **Domain-Driven Design**: Clear module boundaries, domain language
- **SOLID Principles**: Single Responsibility, Open/Closed, etc.

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
        String description = llmCallLimiter.execute(LlmClientType.CHAT, () -> {
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
    return llmCallLimiter.execute(LlmClientType.CHAT, () -> {
        return chatClient.prompt(prompt).call().content();
    });
}

// Caller - wraps again (DOUBLE WRAPPING!)
public void processBatch(List<MedicalCase> cases) {
    for (MedicalCase
    case :
    cases){
        String description = llmCallLimiter.execute(LlmClientType.CHAT, () -> {
            return descriptionService.generateDescription(
            case); // Already wrapped!
        });
    }
}
```

**Rationale**: Rate limiting is infrastructure concern, not business logic. Service should focus on description
generation, callers manage concurrency.

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
- [Architecture](ARCHITECTURE.md)
- [Testing Guide](TESTING.md)

---

*Last updated: 2026-01-23*
