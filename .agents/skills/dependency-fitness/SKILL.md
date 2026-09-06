---
name: dependency-fitness
description: >
  Audit third-party libraries already in the project (npm/Maven/etc.) for fit:
  how much of each package is actually used, cost of keeping it, and whether a
  tiny in-repo helper should replace the dependency. Prefer fewer dependencies.
  Use when reviewing package.json/pom.xml, dependency bloat, supply-chain risk,
  or when asked to thin the dependency tree. Complements oss-first (search before
  adding) and write-less-code (minimise owned LoC) — this skill decides when to
  *remove* a dependency that was already added.
---

# Dependency Fitness

**Core principle:** fewer third-party dependencies is better. A library that
contributes only a handful of call-sites is often more expensive (updates,
CVEs, transitive graph, review surface) than a small in-project helper.

This skill is the **mirror of `oss-first`**:

| Skill | Question |
|-------|----------|
| `oss-first` | Should we *add* an existing library instead of writing one? |
| `dependency-fitness` | Should we *remove* a library we barely use and own a thin slice instead? |

## When to use

- Explicit audit request (“прогони зависимости”, dependency fitness, thin deps)
- Before major version bumps or lockfile renovations
- After a feature lands that pulled in a large package for one helper
- Supply-chain / security review of the dependency tree
- New project bootstrap (after first dependency set is chosen)

**Skip for:** one-line config edits, unrelated bugfixes, or when the user already
decided to keep a package for compliance/licensing reasons.

## Verdict rubric

For each direct dependency, classify:

| Verdict | Meaning |
|---------|---------|
| **KEEP** | Broad use, framework-coupled, or replacing would be larger/riskier than the dep |
| **TRIM** | Keep the package but drop unused entrypoints / peer / duplicate alternatives |
| **REPLACE** | Usage is a thin wrapper (≲ ~50–150 LoC equivalent); implement in-repo |
| **REMOVE** | Unused or only referenced from dead code / deprecated app |

### Evidence required (do not guess)

1. **Manifest** — `package.json` / `pom.xml` / etc. (direct deps only for primary pass; note heavy transitives separately).
2. **Import/call graph** — ripgrep for package name / main exports across `apps/`, `packages/`, `scripts/`. Count files and distinct APIs used.
3. **Surface estimate** — list the symbols/APIs actually called (e.g. only `format` from `date-fns`).
4. **Replacement cost** — rough LoC + tests + edge cases if inlined; license and security notes.
5. **Coupling** — framework plugins (Spring starters, Radix/shadcn primitives, Vite plugins) usually **KEEP** even if import count looks “small”.

### Heuristics

- **One function from a large kit** (e.g. one `date-fns` helper, one `lodash` method) → strong **REPLACE** candidate.
- **Duplicate libraries for the same job** (`react-icons` + `lucide-react`, two dagre packages) → **TRIM** to one.
- **Deprecated app path only** (`apps/server-deprecated`) → prefer **REMOVE** from production path; may stay until cutover.
- **Types-only `@types/*`** → keep while the JS package stays; drop with the package.
- **Never remove** without a migration plan: crypto, DB drivers, auth frameworks, OpenAPI generators, test runners.

## Workflow

### 1. Inventory

List direct dependencies by ecosystem (Node root workspace, Java `apps/backend`, etc.).

### 2. Measure usage

For each suspicious or non-framework package:

```bash
# example — adapt paths
rg -n "from ['\"]date-fns|require\\(['\"]date-fns" apps packages --glob '!**/node_modules/**'
```

Record: hit count, files, APIs used.

### 3. Score and recommend

Produce a table:

```markdown
| Package | Ecosystem | Hits / files | APIs used | Verdict | Rationale / next step |
```

Order by actionability: **REMOVE** / **REPLACE** first, then **TRIM**, then **KEEP**.

### 4. Boundaries

- **Propose first; do not mass-delete deps** unless the user asks to implement removals.
- Do not break Risk Boundaries (auth, schema, canvas serialization) to chase dep count.
- Do not replace well-tested security/crypto libraries with hand-rolled crypto.
- Prefer sequential PRs: one dependency family per change + tests.
- Record durable findings in memory-bank / a short artifact under `.agents/artifacts/` if the audit is large.

## Relationship to other skills

- After **REPLACE**, run `write-less-code` / `simplify` on the inlined helper.
- Before **adding** a new dep, still run `oss-first`, then apply this rubric to the candidate (“will we use > thin surface?”).
- `security-check` still owns CVE / secrets review; this skill owns *fitness*, not vulnerability triage.

## Anti-patterns

- Counting transitive packages as if they were direct without noting the parent.
- Replacing React/Spring/Vite ecosystem pieces with “simpler” custom frameworks.
- Inlining PDF/Office parsers, browser engines, or TLS stacks “to save a dep”.
- Declaring **REMOVE** from import count alone without checking dynamic imports / config references.
