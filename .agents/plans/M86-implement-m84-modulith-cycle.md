# M86: Implement M84 (Resolve the Pre-Existing Modulith Cycle)

**Status:** Active (planned 2026-06-09)
**Created:** 2026-06-09
**Depends on:** M84 (Active plan — the spec this milestone implements), M75/M72 (archived — established the test infrastructure; M57 — the original intent of the `caseanalysis -> llm` boundary), the live failure mode of `ModulithVerificationIT` confirmed on develop (2026-06-09: four cycles rooted in `MedicalCaseDescriptionService`'s `ChatClient` import inside the `medicalcase` module).

## Problem Statement

`mvn verify` fails on `ModulithVerificationIT.verifyApplicationModuleStructure` with the four cycles the M84 plan enumerates (verified live 2026-06-09, after the M85 docs fix landed):

1. `caseanalysis -> medicalcase -> llm -> caseanalysis`
2. `embedding -> medicalcase -> llm -> embedding`
3. `graph -> medicalcase -> llm -> graph`
4. `llm -> medicalcase -> llm` (and the transitive `llm -> retrieval -> medicalcase -> llm`)

M76 (data-sizes) and M77 explicitly carve out this cycle from their acceptance criteria ("pre-existing Modulith cycle"). M84 lays out the fix (move `MedicalCaseDescriptionService` from `medicalcase` to `llm`, update 10 import sites). M86 is the execution: do the M84 work on a feature branch, run the test, commit, merge. The expected outcome is `mvn verify` green — for the first time since M57.

M86 is intentionally a thin execution plan. The design rationale, the non-goals, the parts/phases, and the acceptance criteria are all in M84. M86 only adds the execution-layer detail: the feature branch name, the test surface, the commit shape, and the merge flow.

## Goal

1. Create `feature/m86-implement-m84-modulith-cycle` from develop.
2. Execute the M84 plan verbatim:
   - **Part 1 — Move the service**: create `llm/service/MedicalCaseDescriptionService.java` and `llm/service/impl/MedicalCaseDescriptionServiceImpl.java` (identical content, new package); delete the `medicalcase/service/MedicalCaseDescriptionService.java` and `medicalcase/service/impl/MedicalCaseDescriptionServiceImpl.java`.
   - **Part 2 — Update callers** (import only, no logic change): 5 main files (`EmbeddingServiceImpl`, `MultiEndpointEmbeddingServiceImpl`, `MedicalCaseEmbeddingSupport`, `SyntheticDataPostProcessingServiceImpl`, `CaseIntakeWorkflowEngine`) and 5 test files (`MultiEndpointEmbeddingServiceImplTest`, `SyntheticDataPostProcessingReconcileTest`, `SyntheticDataPostProcessingReconcileCaseTest`, `SyntheticDataGeneratorIT`, `MedicalCaseDescriptionServiceIT`).
   - **Part 3 — Verify the cycle is broken**: `mvn verify` → `ModulithVerificationIT` passes; `mvn test` → all 872+ existing unit tests still pass; manual review → no `ChatClient` import in `medicalcase`.
3. Commit on the feature branch (one commit per Part 1/2/3, or a single commit if the diff is small enough to review in one pass). No `Co-authored-by:` trailer.
4. Merge to develop, delete the feature branch (local + remote), update `00-index.md` to archive M86 (M84 stays Active until the Modulith test is green in CI; M86 is the implementation milestone).

## Non-Goals (inherited from M84)

| Don't | Why |
|---|---|
| Restructure the `caseanalysis` module's `allowedDependencies` | M57 already documented the intent that `caseanalysis` should depend on `core` and `medicalcase` only, and the LLM tool binding is a one-way runtime edge. M86 keeps that intact. |
| Move other LLM-calling services from `medicalcase` | Verified by grep: there are none besides `MedicalCaseDescriptionService`. |
| Rename the service or its methods | Callers depend on the existing signatures. |
| Widen `llm`'s `allowedDependencies` | `llm` already allows `medicalcase` and `embedding`; the moved service lands in a home that already permits the consumers to import it. |
| Touch HIPAA / auth / V2+ migrations / `pom.xml` | Global boundaries. |

## Changes

| Area | File | Change |
|------|------|--------|
| Feature branch | `feature/m86-implement-m84-modulith-cycle` (new from develop) | All M84 work lands here. |
| Service interface (move) | `src/main/java/com/berdachuk/medexpertmatch/llm/service/MedicalCaseDescriptionService.java` (new, identical content) | Package `medicalcase.service.*` → `llm.service.*`. |
| Service impl (move) | `src/main/java/com/berdachuk/medexpertmatch/llm/service/impl/MedicalCaseDescriptionServiceImpl.java` (new, identical content) | Package `medicalcase.service.impl.*` → `llm.service.impl.*`. |
| Service interface (delete) | `src/main/java/com/berdachuk/medexpertmatch/medicalcase/service/MedicalCaseDescriptionService.java` (delete) | — |
| Service impl (delete) | `src/main/java/com/berdachuk/medexpertmatch/medicalcase/service/impl/MedicalCaseDescriptionServiceImpl.java` (delete) | — |
| Caller imports (5 main + 5 test) | See M84 Part 2 table | `import com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService;` → `import com.berdachuk.medexpertmatch.llm.service.MedicalCaseDescriptionService;`. No logic change. |
| Docs (optional) | `docs/HARNESS_AND_AGENT_USAGE.md` §3 "How MedExpertMatch maps the harness" | If the service is named there, update the path reference. (Verify by grep before committing.) |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Create `feature/m86-implement-m84-modulith-cycle` from develop. Run `mvn verify` once on the branch to confirm the pre-existing failure is reproducible. | Pending |
| 2 | Create `llm/service/MedicalCaseDescriptionService.java` (new package, identical content) | Pending |
| 3 | Create `llm/service/impl/MedicalCaseDescriptionServiceImpl.java` (new package, identical content) | Pending |
| 4 | Delete the two old `medicalcase/service/...` files. | Pending |
| 5 | Update the 5 main + 5 test import sites (M84 Part 2 table). One commit per logical group is fine; a single commit is also fine. | Pending |
| 6 | Run `mvn test` → expect 872+ green. | Pending |
| 7 | Run `mvn verify` → expect `ModulithVerificationIT` green for the first time since M57. | Pending |
| 8 | Manual review: `grep -rn "import com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService" src/` returns no hits. | Pending |
| 9 | Commit on the feature branch (no `Co-authored-by:` trailer), push, merge `--no-ff` to develop, delete the feature branch (local + remote). | Pending |
| 10 | Archive this M86 plan; update `00-index.md` (drop M86 from Active). M84 stays Active until the Modulith test is green in CI on a clean release. | Pending |

## Acceptance criteria

- [ ] `mvn verify` passes; `ModulithVerificationIT.verifyApplicationModuleStructure` is green
- [ ] No `ChatClient` import or any other Spring AI LLM type in the `medicalcase` module
- [ ] `medicalcase` `package-info.java` allowed dependencies unchanged (`core :: *` only)
- [ ] `llm` `package-info.java` allowed dependencies unchanged
- [ ] All 872+ existing unit tests + integration tests pass
- [ ] `MedicalCaseDescriptionService` interface signature unchanged so no caller logic changes
- [ ] No `Co-authored-by:` trailer in any commit
- [ ] The "pre-existing Modulith cycle" exception in M77 acceptance criteria can be dropped (future cleanup, not part of M86)

## Out of scope

- Restructuring the `caseanalysis` module's `allowedDependencies`
- Moving other LLM-calling services from `medicalcase`
- Renaming the service or its methods
- Refactoring the M84 archived plan

## References

- `.agents/plans/M84-resolve-modulith-cycle.md` — the spec this milestone implements (Parts 1/2/3, non-goals, acceptance criteria, references)
- `.agents/plans/archive/M76-resolve-modulith-cycle.md` — the orphaned original spec, equivalent in scope
- `src/main/java/com/berdachuk/medexpertmatch/medicalcase/service/MedicalCaseDescriptionService.java` — current location
- `src/main/java/com/berdachuk/medexpertmatch/medicalcase/service/impl/MedicalCaseDescriptionServiceImpl.java` — current impl
- `src/main/java/com/berdachuk/medexpertmatch/llm/package-info.java` — `llm` allows `medicalcase`, so the moved service can be consumed by `llm` tools without widening the LLM module's deps
- `src/test/java/com/berdachuk/medexpertmatch/ModulithVerificationIT.java` — the test that currently fails (the 4 cycles M84 enumerates)
- `src/main/java/com/berdachuk/medexpertmatch/medicalcase/package-info.java` — confirms `medicalcase` should only know about `core`
