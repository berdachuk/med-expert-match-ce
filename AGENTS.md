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
├── chat/           # Chat agent orchestration + retention (→core, llm, ...)
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
./scripts/start-local-stack.sh       # Local stack: Postgres + mvn -Plocal + MkDocs
./scripts/restart-service-local.sh   # Restart local stack (stop + start)
./scripts/ralph.sh M{NN} [--max N] [--agent stub|openai|<path>] [--max-time S] [--max-consecutive-failures N]   # Ralph-style autonomous loop over M{NN}-stories.json
```

## Ralph Workflow

`scripts/ralph.sh` is the iteration driver described in M78/M79/M80/M81. It
reads a machine-parseable milestone plan (`.agents/plans/M{NN}-stories.json`),
picks the highest-priority unpassed story, runs its `test_target`, and on
green commits + marks the story `passes: true` + writes a `commit_sha` +
appends a block to `.agents/plans/progress.txt`. On red, it logs to
`progress.txt` and exits non-zero.

### Agent modes (`--agent`)

The `--agent` flag picks how the loop implements each story. Three modes:

- `--agent stub` (default, M79 behavior) — the loop assumes a human has
  already implemented the story in the working tree; it just runs the test
  and commits. Use for hand-driven milestones or for verifying the
  harness itself.
- `--agent openai` (M80) — the loop calls an OpenAI-compatible chat
  endpoint to generate a unified-diff patch for the story, then applies
  the patch. Requires env vars `OPENAI_API_KEY`, `OPENAI_BASE_URL`,
  `OPENAI_MODEL`. `OPENAI_TIMEOUT` (default 600) and
  `OPENAI_TEMPERATURE` (default 0.0) are optional. The endpoint is
  whichever OpenAI-compatible service the project is already configured
  for (CLINICAL_* or UTILITY_* per M67); the env vars are independent
  from Spring AI's config so the loop can target a different model than
  the application itself.
- `--agent <path>` — run an external script that reads a story id and
  prints a patch on stdout. Use to swap in a non-OpenAI agent while
  reusing the rest of the loop.

Pipeline (openai mode): `render_prompt.sh` → `call_openai.sh` →
`extract_patch.sh` → `apply_patch.sh`. The renderer expands
`.agents/templates/M{NN}-prompt.md.template` with the story's `accept[]`
and `skills_to_load[]`; the LLM is told to return a single ```diff
fenced block; the extractor pulls the first such block; `git apply
--check` verifies it before applying.

### Story contract and iteration log

- **Story contract:** `.agents/plans/M{NN}-stories.json` — see `M77-stories.json`
  for the canonical 10-story shape. Each story has `id`, `title`,
  `test_target`, `files_touched[]`, `skills_to_load[]`, `accept[]`, `priority`,
  `passes`, `commit_sha`, `started_at`, `finished_at`, `duration_min`, `notes`.
- **Iteration log:** `.agents/plans/progress.txt` — append-only. Read it
  before starting any new milestone; it captures the "rediscovered-the-same-
  gotcha" tax that motivated the loop.
- **Smoke test:** `./scripts/ralph.sh M77 --dry-run` prints the next story
  and exits 0 without invoking the agent. Full loop: `./scripts/ralph.sh M77
  --max 10 --max-time 21600 --max-consecutive-failures 3 --agent openai`
  (with `OPENAI_*` set). Sanity: `bash scripts/ralph/test_ralph.sh` runs
  8 negative tests and exits 0; `bash scripts/ralph/test_render_prompt.sh`
  runs 6 prompt tests; `bash scripts/ralph/test_extract_patch.sh` runs
  3 patch-extraction tests.

**M81 stop conditions** (when the pilot is running unattended): the
loop appends `[RED-TIMEOUT]` to `progress.txt` and exits 8 when
`--max-time S` elapses, or `[RED-LOOP-GIVEUP]` and exits 7 when
`--max-consecutive-failures N` is exceeded. These blocks let a human
find the loop in a known state.

**Do NOT Ralph-ify** (per M78 non-goals + AGENTS.md global boundaries):

- HIPAA / PHI handling (loop has no session memory)
- Admin/auth boundaries (`AdminAccessGuard`, etc.)
- New Flyway migrations on V2+ (AGENTS.md line 66)
- AI provider swaps (AGENTS.md line 74)
- `pom.xml` dependency version changes (AGENTS.md line 73)
- Module boundary changes (`allowedDependencies`)
- Exploration work (M58/M60) — Ralph assumes design is decided
- Plans that delete or archive code (AGENTS.md line 75)
- Plans requiring 24h manual smoke (loop has no clock / no browser)

## Module Guidance (nested AGENTS.md)

| Module | File | Focus |
|--------|------|-------|
| core | `core/AGENTS.md` | Shared infrastructure rules, exception patterns, config boundaries |
| retrieval | `retrieval/AGENTS.md` | GraphRAG matching/scoring/routing flows, module orchestration |
| llm | `llm/AGENTS.md` | LLM orchestration, agent workflows, prompt management, medical compliance |
| ingestion | `ingestion/AGENTS.md` | FHIR adapters, synthetic data generation, multi-module bootstrapping |
| web | `web/AGENTS.md` | Thymeleaf controllers, SSR patterns, web UI conventions |

> All other domain modules (doctor, medicalcase, medicalcoding, facility, clinicalexperience, chat, caseanalysis, evidence, embedding, chunking, documents, graph) follow the same patterns documented in skills. Their conventions are covered by `core-architecture`, `domain-modeling`, and `code-style` skills — no individual AGENTS.md needed.

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
| api-design | `api-design/SKILL.md` | Designing or changing REST/RPC/A2A endpoints, versioning, error contracts |
| write-less-code | `write-less-code/SKILL.md` | Before non-trivial implementation and before commit — push back on bloat, prefer minimum diff and reuse |
| security-check | `security-check/SKILL.md` | Before/after any work touching auth, APIs, DB, secrets, external input, infra, or new dependencies; review final diff for vulnerabilities |

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
- Git commits: never add `Co-authored-by:` trailers (no agent/IDE co-author lines)
- Mock all external HTTP APIs in integration tests: use WireMock (wiremock-standalone) with recorded fixture responses stored in `src/test/resources/{module}/`. Never make live HTTP calls to external services (PubMed, NCBI, etc.) from tests. Record real API responses once, store as fixtures, and stub them in all ITs. External services that accept an injectable base URL (e.g. `baseUrl` constructor param) enable easy WireMock wiring.

## TDD Workflow (mandatory)

Always use TDD. Before implementing any functionality:

1. **Write the test first** — before any implementation code.
2. **Verify the test against the requirements** — use an internal review tool/skill (e.g. a code-review or testing skill, or a review subagent) to confirm it truly encodes the requirement.
3. **Run security check before implementation** — load the `security-check` skill for any task touching auth, APIs, DB, secrets, external input, infrastructure, or new dependencies. Report risks before coding.
4. **Implement** the functionality — only after the test is written, verified, and security pre-check is complete.
5. **Re-run the test** (`mvn verify`) — fix problems and iterate until it passes.
6. **Run security check again before commit** — review the final diff for secrets, missing auth, injection risks, vulnerable dependencies, or unsafe config.

## Layer Model

```
.agents/
├── skills/         ← Single source of truth for domain skills
└── plans/          ← Implementation plans (M{NN}-*.md; archive/ for completed)
AGENTS.md             ← Root index (this file)
{module}/AGENTS.md    ← Module-specific conventions (2-5 files only)
.cursor/              ← Optional IDE adapter (generated from skills)
.kilo/                ← Optional Kilo adapter (commands/agents generated from skills)
```

See `docs/ai-context-strategy.md` for adapter design and sync rules.
