# M130 — Token-Efficient Format Skill Hardening

## Goal

Fix defects in the `token-efficient-format` skill identified during a code review against the
actual prompt inventory and parsers. Make the skill a reliable, safe-to-follow guide instead
of a trap that can route an agent into producing unparsable output.

## Requirement

REQ-130: The `token-efficient-format` skill must accurately reflect the implemented state of
the codebase (TOON has no adapter; ultra-compact JSON is the implemented non-schema path) and
must not recommend a format that has no working parser.

## Background

A code check of `src/main/resources/prompts/*.st` and their Java parsers found:

- `goal-classification.st` already uses ultra-compact JSON (`g`/`s`/`u`) — compliant.
- `icd10-extraction`, `specialty-determination`, `urgency-classification`,
  `reranking-doctors` use line-based / list output — compliant (skill §5).
- `case-analysis-system.st` and `medgemma-case-analysis-system.st` use verbose JSON
  parsed by `Map.class` (no BeanOutputConverter). These are candidates for ultra-compact
  JSON, **not TOON** — but that work is deferred to M131 because it is coupled to
  `LlmResponseSanitizer` (`FIELD_LABELS` + `JSON_BLOCK_PATTERN` at
  `LlmResponseSanitizer.java:34,62`), which hardcodes the long key names.

### Skill defects to fix (this milestone, docs-only)

1. **TOON is unimplemented but recommended as the default non-schema format.**
   The skill §2 and decision table direct agents to "prefer TOON" with no adapter existing.
   An agent following the skill would emit indentation-based output no Java parser can read.
2. **Decision rule ordering is risky.** §2 lists "no schema → prefer TOON" before
   ultra-compact JSON. Given TOON's status, ultra-compact JSON (implemented, Jackson-safe)
   must be the first non-schema choice.
3. **Decision table conflates two cases.** "Strict schema + Java DTO mapping" mixes
   BeanOutputConverter (provider structured output) with hand `Map.class` parsing.
   These have different correct answers.
4. **No input-side reduction guidance.** The skill only covers output format. The codebase
   pays repeated token cost for a duplicated medical disclaimer (~10 prompt files) and a
   duplicated `CRITICAL OUTPUT LIMITS` block (4 prose prompts).
5. **`AGENTS.md` skill description omits TOON**, inconsistent with the skill body.
6. **No `extractJson` coupling note.** Any non-standard format must round-trip through
   `LlmResponseSanitizer.extractJson` so sanitization / PHI stripping / JSON-block-to-prose
   rendering stays consistent.

## Scope (this milestone)

**Docs-only. No production code, no prompts, no tests change.** Safe to merge without a build.

### Tasks

- [x] TASK-130-1 — Add a `STATUS` banner to skill §2 marking TOON as unimplemented
  (no TOON→JSON adapter exists; M127 plan explicitly deferred it). Redirect non-schema
  nested-structure cases to ultra-compact JSON.
- [x] TASK-130-2 — Reorder decision rules so ultra-compact JSON is the first non-schema
  choice; TOON is a "future option" gated on adapter availability.
- [x] TASK-130-3 — Split the decision-table row into (a) DTO via BeanOutputConverter /
  provider structured output and (b) Map-parsed, no schema. Map-parsed → ultra-compact JSON.
- [x] TASK-130-4 — Add §7 "Input-side token reduction" covering shared disclaimers and
  repeated `CRITICAL OUTPUT LIMITS` blocks, with a pointer to Spring AI resource composition.
- [x] TASK-130-5 — Add a note that any non-JSON format must round-trip through
  `LlmResponseSanitizer.extractJson` and that short-key changes to JSON prompts must update
  `LlmResponseSanitizer.FIELD_LABELS` + `JSON_BLOCK_PATTERN` in lockstep (deferred to M131).
- [x] TASK-130-6 — Update the `AGENTS.md` Skills Index description to mention TOON
  (status) for consistency, OR align both. Decision: keep TOON in the skill body (now
  status-gated) and add a brief status note to the `AGENTS.md` row.

## Out of scope (deferred to M131)

- Converting `case-analysis-system.st` / `medgemma-case-analysis-system.st` to short keys.
- Updating `LlmResponseSanitizer.FIELD_LABELS` + `JSON_BLOCK_PATTERN` for short keys.
- Extracting the duplicated medical disclaimer into a shared `.st` fragment.

These require parser + sanitizer changes and must follow TDD with integration tests. They
are captured in the next-phase plan M131.

## Verification

- `mvn test -q` unaffected (no code changed) — skip; docs-only change.
- Manual review: skill is internally consistent; no rule recommends an unimplemented format.
- Memory bank updated; plan archived.

## Traceability

- Requirement: REQ-130 (skill reliability / safe-to-follow)
- Owning module: `.agents/skills/` (no Java module)
- Scenario: SCN-130 — an agent following the skill for a Map-parsed nested-structure call
  is directed to ultra-compact JSON, not TOON.
- Test artifact: none (docs-only); verified by manual review.
- Implementation: `.agents/skills/token-efficient-format/SKILL.md`, `AGENTS.md`
- Status: verified