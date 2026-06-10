# LLM Module

Orchestrates all LLM-driven workflows and Agent Skills. Depends on 11 other modules to coordinate complex medical workflows.

## Purpose

- `MedicalAgentService` — primary orchestrator
- Workflow services for: case analysis, case intake, doctor matching, queue prioritization, routing, recommendations, network analytics
- `LlmResponseSanitizer` — strips PHI from LLM outputs before logging/storage
- `llm/harness/` — verify/policy-gate loops, context bundles, tool scope, `DoctorMatchWorkflowEngine` (M29)
- Spring AI Agent Utils integration (runtime skills in `src/main/resources/skills/`)

## Owned Domain Models

- `AnalyzeJobStatus`, `MatchJobStatus`, `PrioritizeJobStatus`, `RouteJobStatus` — async job state enums/records

## Module Dependencies

`@ApplicationModule(allowedDependencies = {"core", "evidence", "doctor", "facility", "medicalcase", "clinicalexperience", "graph", "retrieval", "caseanalysis", "medicalcoding", "embedding", "web"})`

## Conventions

- All LLM prompts use external `.st` files in `src/main/resources/prompts/` — never hardcode prompt strings in Java
- Register prompts as `@Qualifier` `PromptTemplate` beans in `core/config/PromptTemplateConfig.java` using `.resource(classpath:...)` (Spring AI resource pattern), not inline `.template("...")` or text blocks
- Prompt templates are configured with `StTemplateRenderer` (`<variable>` delimiters) and injected via constructor
- Medical disclaimers must be included in ALL medical AI prompts
- Agent Skills (runtime) live in `src/main/resources/skills/` — distinct from `.agents/skills/` (development)
- Sanitize all LLM outputs through `LlmResponseSanitizer` before logging or storing

## Constraints

- OpenAI-compatible providers ONLY (no Ollama, no Anthropic direct)
- Never expose patient data in LLM prompts without anonymization
- Never hardcode API keys — use environment variables or Spring config
- Agent Skill .md files in `src/main/resources/skills/` are runtime prompts, NOT development guides

## Harness (M29 / M57)

- Docs: `docs/HARNESS.md` (workflow engines, goal routing), `docs/FUNCTIONGEMMA.md` (tool-calling model)
- Config: `medexpertmatch.llm.harness.*` (`HarnessProperties`)
- `GoalClassifier` hybrid routing (M57): session rules → keywords → LLM; analyze-case harness enabled by default
- Doctor match API uses `DoctorMatchWorkflowEngine` (states logged as `HARNESS_STATE`)
- Chat: `ChatToolContextHolder` + `ToolScopeEnforcingResolver`; policy gate via `MedicalAgentPolicyGateService`
- Multilingual: `ChatLanguageService` translates to/from English around harness and chat paths
- Harness backlog template: `.agents/templates/harness-backlog-item.md`

## Related Skills

- `llm-prompts` — prompt template creation and management
- `testing` — TDD for harness changes
- `code-style` — service implementation patterns
- `testing` — mocking AI providers in integration tests
- `core-architecture` — cross-module orchestration boundaries
