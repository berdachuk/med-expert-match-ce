# MedExpertMatch

Medical expert recommendation system — Spring Boot 4.0 + Spring Modulith + GraphRAG (PostgreSQL/PgVector/Apache AGE) + Spring AI 2.0.

**Stack**: Java 21, Maven 3.9+, Spring Modulith 2.0, Thymeleaf, Flyway, Testcontainers, Docker.

## Repo Map

```
src/main/java/.../medexpertmatch/
├── core/           # Shared infrastructure (exception, config, util, health)
├── doctor/         # Doctor/MedicalSpecialty entities + repos + REST
├── medicalcase/    # MedicalCase/CaseType/UrgencyLevel entities + repos + REST
├── medicalcoding/  # ICD10Code/Procedure entities + repos + REST
├── facility/       # Facility entity + repos
├── clinicalexperience/  # ClinicalExperience entity + repos (doctor-case history)
├── caseanalysis/   # LLM-based case analysis (→core, medicalcase)
├── evidence/       # PubMed clinical evidence retrieval (→core)
├── embedding/      # PgVector embeddings + multi-endpoint pool (→core, medicalcase)
├── graph/          # Apache AGE Cypher graph ops (→core + 5 domain modules)
├── retrieval/      # Hybrid GraphRAG matching/scoring/routing (→core + 8 modules)
├── ingestion/      # FHIR adapters + synthetic data generation (→core + 7 modules)
├── llm/            # LLM orchestration + Agent Skills (→core + 11 modules)
├── chunking/       # Document chunking strategies (→core)
├── documents/      # Document management + PDF/JSONL parsing (→core, embedding)
├── web/            # Thymeleaf SSR web UI (→core, llm + 6 modules)
└── system/         # System health indicators
.agents/
├── skills/         # Domain skills (canonical; see Skills Index below)
└── plans/          # Implementation plans: M{NN}-{goal-slug}.md (+ archive/)
src/main/resources/
├── prompts/        # Spring AI StringTemplate (.st) prompt files
├── skills/         # Spring AI Agent Skills (runtime; distinct from .agents/skills/)
├── sql/            # External SQL query files
├── templates/      # Thymeleaf templates
└── static/         # CSS/JS/images
```

## Commands

```bash
mvn spring-boot:run -Plocal          # Run app (local profile)
mvn clean install                    # Full build
mvn clean install -DskipTests        # Build without tests
mvn test                             # Unit tests only (*Test.java, *Tests.java)
mvn verify                           # Integration tests (*IT.java) + package
mvn test -Dtest=DoctorRepositoryIT   # Single IT class
mvn test jacoco:report               # Coverage report
mvn clean verify sonar:sonar         # SonarQube/Cloud analysis
./scripts/build-test-container.sh    # Build custom Postgres+AGE+PgVector test image
./scripts/restart-service-local.sh   # Restart local service
```

## Module Guidance (nested AGENTS.md)

| Module | File | Focus |
|--------|------|-------|
| core | `core/AGENTS.md` | Shared infrastructure rules, exception patterns, config boundaries |
| retrieval | `retrieval/AGENTS.md` | GraphRAG matching/scoring/routing flows, module orchestration |
| llm | `llm/AGENTS.md` | LLM orchestration, agent workflows, prompt management, medical compliance |
| ingestion | `ingestion/AGENTS.md` | FHIR adapters, synthetic data generation, multi-module bootstrapping |
| web | `web/AGENTS.md` | Thymeleaf controllers, SSR patterns, web UI conventions |

> All other domain modules (doctor, medicalcase, medicalcoding, facility, clinicalexperience, evidence, embedding, chunking, documents, caseanalysis, graph) follow the same patterns documented in skills. Their conventions are covered by `core-architecture`, `domain-modeling`, and `code-style` skills — no individual AGENTS.md needed.

## Skills Index (`.agents/skills/`)

| Skill | File | When to load |
|-------|------|-------------|
| core-architecture | `core-architecture/SKILL.md` | Adding/changing modules, understanding module boundaries, cross-module changes |
| domain-modeling | `domain-modeling/SKILL.md` | Creating entities, value objects, DTOs; understanding domain ownership |
| code-style | `code-style/SKILL.md` | Writing any Java/Template/SQL code; style checks before commits |
| testing | `testing/SKILL.md` | Writing tests (unit or IT), configuring Testcontainers, mocking AI providers |
| llm-prompts | `llm-prompts/SKILL.md` | Creating/modifying LLM prompt templates (.st files), Spring AI configuration |
| graph-db | `graph-db/SKILL.md` | Cypher queries via GraphService, Apache AGE MERGE patterns, graph schema changes |
| db-migrations | `db-migrations/SKILL.md` | Schema changes via Flyway V1 consolidation, SQL patterns, migration rules |

## Global Boundaries

**Always allowed, no approval needed:**
- Create/modify files within a single module following existing patterns
- Add tests (unit or IT) for any code
- Create new domain entities, DTOs, repositories following module conventions
- Modify prompt templates (.st files) within existing patterns

**Use caution, consider skill guidance:**
- Cross-module dependency changes (update `package-info.java` allowedDependencies)
- New module creation (follow `core-architecture` skill)
- Flyway migration changes (follow `db-migrations` skill)
- Cypher/AGE queries (follow `graph-db` skill)

**Forbidden without explicit human approval:**
- Remove or weaken HIPAA/PHI protections
- Log or expose patient data in any form
- Change AI provider from OpenAI-compatible to Ollama or other non-OpenAI providers
- Modify `pom.xml` dependency versions (except adding new test deps)
- Merge PRs or deploy to staging/prod
- Delete or archive modules or domain entities

## Plan Files

- Location: `.agents/plans/` directory
- Naming: `M{NN}-{goal-slug}.md` (e.g., `M01-upgrade-spring-ai-to-m6.md`)
- `{NN}` is a zero-padded milestone number; `{goal-slug}` is a kebab-case short goal
- Do NOT create auto-generated IDs (timestamps or random strings) for plan filenames
- When a plan is fully implemented and verified, move it to `.agents/plans/archive/`

## Key Rules at a Glance

- TDD (mandatory): (1) write the test FIRST; (2) verify the test against the requirements with an internal review tool/skill (e.g. a code-review or testing skill, or a review subagent) to confirm it truly encodes the requirement; (3) only then implement the functionality; (4) re-run the test (`mvn verify`) and fix problems until it passes. Prefer integration tests (`*IT.java` suffix).
- Interface-based design: every service/repo has interface + impl
- Java records for domain entities (immutable), Lombok for services/impls only
- External `.st` files for LLM prompts (never hardcode prompt strings); wire via `PromptTemplate.builder().resource(...)` in `PromptTemplateConfig` — see [Spring AI: using resources instead of raw strings](https://docs.spring.io/spring-ai/reference/2.0/api/prompt.html#_using_resources_instead_of_raw_strings)
- External `.sql` files for repository queries
- Service-layer transactions (`@Transactional` on service methods)
- Separate insert/update repository methods (never `createOrUpdate`)
- Single-entity repositories with batch-loading for related data
- Graph operations only through `GraphService` interface
- Flyway V1 consolidation (no V2/V3 until post-MVP)
- OpenAI-compatible providers only (no Ollama)
- All patient data must be anonymized; no PHI in logs/errors/tests

## TDD Workflow (mandatory)

Always use TDD. Before implementing any functionality:

1. **Write the test first** — before any implementation code.
2. **Verify the test against the requirements** — use an internal review tool/skill (e.g. a code-review or testing skill, or a review subagent) to confirm the test truly encodes the requirement.
3. **Implement** the functionality — only after the test is written and verified.
4. **Re-run the test** (`mvn verify`) — fix problems and iterate until it passes.

## Layer Model

```
.agents/
├── skills/         ← Single source of truth for domain skills
└── plans/          ← Implementation plans (M{NN}-*.md; archive/ for completed)
AGENTS.md             ← Root index (this file)
{module}/AGENTS.md    ← Module-specific conventions (2-5 files only)
.cursor/              ← Optional IDE adapter (generated from skills)
```

See `docs/ai-context-strategy.md` for adapter design and sync rules.
