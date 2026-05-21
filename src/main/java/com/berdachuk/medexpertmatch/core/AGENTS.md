# Core Module

Shared infrastructure used by all modules. No domain entities owned here.

## Purpose

- Spring AI configuration (`SpringAIConfig`, `PromptTemplateConfig`)
- Exception hierarchy (`MedExpertMatchException`, `RetrievalException`, `ErrorCode`)
- Utilities (`IdGenerator`, `LlmCallLimiter`, `RetryWithBackoff`)
- Log streaming (`LogStreamService`)
- Health monitoring (`HealthCheck`, `DatabaseHealthCheck`)
- SQL injection utilities (`InjectSql`, `SqlInjectBeanPostProcessor`)

## Module Dependencies

`@ApplicationModule` (no allowedDependencies — accessible from all modules).

## Conventions

- Shared config beans go in `config/`, not scattered across modules
- Exceptions extend `MedExpertMatchException`; never throw generic `RuntimeException`
- `IdGenerator` is the single source of ID generation; never use raw UUIDs
- `RetryWithBackoff` for all external API calls (LLM, PubMed, embedding endpoints)
- `LlmCallLimiter` respects configured rate limits; check before calling LLM

## Constraints

- Do NOT add domain entities here — core is infrastructure, not domain
- Do NOT add business logic — orchestrate from the service module that owns the flow
- Do NOT reference specific domain modules in core utilities

## Related Skills

- `core-architecture` — module boundaries and cross-module rules
- `code-style` — shared coding conventions
