# Module Locks

Per-module ownership claims that prevent two agents from editing the same
coupled files concurrently. This is the mechanism that turns **silent semantic**
breakages (e.g. an `.st` prompt short-key change that must update
`core/util/LlmResponseSanitizer.java` in lockstep — the M130-class risk) into a
**textual** conflict the agent can detect and serialize on.

## File naming

`locks/<module>.md` — one file per Spring Modulith module. The module name is the
package suffix under `medexpertmatch/` (e.g. `llm`, `core`, `retrieval`, `web`).

## Lock file format

```markdown
# Lock: llm
- module: llm
- held-by: feat/m133-foo      # branch name (or agent session id)
- acquired: 2026-06-21T12:00Z
- expires: 2026-06-22T12:00Z  # auto-release after 24h
- scope:
  - src/main/java/**/medexpertmatch/llm/**
  - src/main/resources/prompts/*
  - src/main/resources/skills/**
  - src/main/java/**/medexpertmatch/core/util/LlmResponseSanitizer.java
  - src/test/java/**/medexpertmatch/llm/**
- notes: "converting reranking-doctors.st to short keys"
```

## Rules

1. **Acquire before editing coupled files.** Before touching any file listed in
   a module's `scope` (especially prompt `.st` files paired with
   `LlmResponseSanitizer` — see `token-efficient-format` skill §sanitizer-coupling),
   the agent MUST hold that module's lock.
2. **One holder at a time.** To acquire, create/overwrite `locks/<module>.md`.
   If `git pull` shows the file already exists with a non-expired `held-by`
   different from your branch, **do not edit** — pick a different module or wait.
3. **Auto-release.** A lock whose `expires` is in the past is stale; any agent may
   overwrite it.
4. **Release on merge.** Delete `locks/<module>.md` (or set `held-by: released`)
   once the branch merges to `develop`.
5. **Locks are advisory but skill-enforced.** The `code-style` and
   `security-check` skills load this README and verify lock ownership before
   approving edits to coupled files.

## Why per-module, not per-file

Per-file locks would be simpler but miss the coupling: two agents editing
different `.st` prompts that share `LlmResponseSanitizer` would each acquire
their own file lock and still produce a green merge that breaks the sanitizer
lockstep. The module lock makes the shared dependency explicit.

## Coupled-file pairs (must share one module lock)

These pairs are documented in the `token-efficient-format` skill and MUST be
edited under a single module lock:

| Prompt / shared file | Coupled sanitizer/config | Owning module lock |
|----------------------|--------------------------|--------------------|
| `prompts/*.st` (short keys) | `core/util/LlmResponseSanitizer.java` (`FIELD_LABELS`, `JSON_BLOCK_PATTERN`, `URGENCY_PATTERN`, `SPECIALTY_PATTERN`) | `llm` (or `core` if sanitizer is the primary change) |
| `case-analysis-system.st` / `medgemma-case-analysis-system.st` | `CaseAnalysisServiceImpl.extractList` (`key`+`legacyKey`) | `caseanalysis` |
| `goal-classification.st` | `GoalClassifier.parseClassification()` | `llm` |
| `reranking-doctors.st` | `RerankingServiceImpl` | `retrieval` |
| `icd10-extraction-system.st` / `specialty-determination-system.st` | `CaseAnalysisServiceImpl.parseJsonArray()` | `caseanalysis` |

When a prompt and its consumer live in different modules, acquire the lock of
the module whose files dominate the change; the lock `scope` must list both.