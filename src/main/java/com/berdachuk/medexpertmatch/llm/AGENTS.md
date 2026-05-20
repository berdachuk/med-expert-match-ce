# LLM Module

Orchestrates all LLM-driven workflows and Agent Skills. Depends on 11 other modules to coordinate complex medical workflows.

## Purpose

- `MedicalAgentService` — primary orchestrator
- Workflow services for: case analysis, case intake, doctor matching, queue prioritization, routing, recommendations, network analytics
- `LlmResponseSanitizer` — strips PHI from LLM outputs before logging/storage
- Spring AI Agent Utils integration (runtime skills in `src/main/resources/skills/`)

## Owned Domain Models

- `AnalyzeJobStatus`, `MatchJobStatus`, `PrioritizeJobStatus`, `RouteJobStatus` — async job state enums/records

## Module Dependencies

`@ApplicationModule(allowedDependencies = {"core", "evidence", "doctor", "facility", "medicalcase", "clinicalexperience", "graph", "retrieval", "caseanalysis", "medicalcoding", "embedding", "web"})`

## Conventions

- All LLM prompts use external `.st` files in `src/main/resources/prompts/` — never hardcode prompt strings
- Prompt templates are configured as Spring beans in `PromptTemplateConfig` with `@Qualifier`
- Medical disclaimers must be included in ALL medical AI prompts
- Agent Skills (runtime) live in `src/main/resources/skills/` — distinct from `.agents/skills/` (development)
- Sanitize all LLM outputs through `LlmResponseSanitizer` before logging or storing

## Constraints

- OpenAI-compatible providers ONLY (no Ollama, no Anthropic direct)
- Never expose patient data in LLM prompts without anonymization
- Never hardcode API keys — use environment variables or Spring config
- Agent Skill .md files in `src/main/resources/skills/` are runtime prompts, NOT development guides

## Related Skills

- `llm-prompts` — prompt template creation and management
- `code-style` — service implementation patterns
- `testing` — mocking AI providers in integration tests
- `core-architecture` — cross-module orchestration boundaries
