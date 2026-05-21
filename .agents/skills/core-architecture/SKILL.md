# Core Architecture

## Description
System architecture, Spring Modulith module boundaries, dependency rules, and cross-module integration patterns. Covers all 17 modules and their tiered dependency structure.

## When to use
- Adding a new module or modifying `package-info.java` dependencies
- Debugging module boundary violations or circular dependencies
- Understanding how modules communicate and where to place new code
- Designing cross-module service orchestration
- Answering: "Which module should own this logic?"

## Instructions

### Module Dependency Tiers

```
Tier 1 — Foundation:   core (shared by all, no domain entities)
Tier 2 — Domain:       doctor, medicalcase, medicalcoding, facility, clinicalexperience, evidence, chunking
Tier 3 — Processing:   caseanalysis, embedding, documents, graph
Tier 4 — Orchestration: retrieval, ingestion, llm
Tier 5 — Presentation:  web
Tier 6 — System:        system
```

### Adding a New Module

1. Create directory under `src/main/java/.../medexpertmatch/{modulename}/`
2. Create `domain/`, `repository/`, `service/`, `rest/` subdirectories as needed
3. Add `package-info.java` with `@ApplicationModule(allowedDependencies = {...})`
4. Declare ONLY the modules you actually reference in imports — never over-declare
5. Add tests following `testing` skill conventions
6. If the module needs DB tables, update Flyway V1 (follow `db-migrations` skill)

### Cross-Module Patterns

- Domain modules (Tier 2) depend ONLY on `core`
- Processing modules (Tier 3) depend on `core` + specific domain modules
- Orchestration modules (Tier 4) depend on `core` + all modules they orchestrate
- Web module (Tier 5) depends on orchestration + core
- Never create dependencies from a domain module to an orchestration module (Tier 2 → Tier 4 is forbidden)

### Service Orchestration

- `MedicalAgentServiceImpl` in `llm` coordinates workflows across domain modules — this is intentional
- `SyntheticDataBootstrapService` in `ingestion` writes to multiple domain modules — also intentional
- Orchestrators use interface-based injection; never new-up dependencies directly

## Boundaries
- Do NOT reorganize module dependency tiers without human approval
- Do NOT add `allowedDependencies` entries speculatively — only declare actual imports
- Do NOT merge domain modules or delete them from the dependency graph
