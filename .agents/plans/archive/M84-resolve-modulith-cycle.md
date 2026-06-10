# M84: Resolve Pre-Existing Spring Modulith Cycle

**Status:** Active (planned 2026-06-09)
**Created:** 2026-06-09
**Depends on:** M75 (archived — established the test infrastructure; M77 plan explicitly excludes the Modulith cycle from its acceptance criteria, treating it as pre-existing), M57 (archived — the original intent was `caseanalysis -> llm` as a one-way runtime edge)

## Problem Statement

`mvn verify` currently fails on `ModulithVerificationIT.verifyApplicationModuleStructure` with two cycles (verified live on develop, 2026-06-09):

1. **Cycle A** — `caseanalysis -> medicalcase -> llm -> caseanalysis`
2. **Cycle B** — `embedding -> medicalcase -> llm -> embedding`

There is also a transitive cluster: `graph -> medicalcase -> llm -> graph` and `llm -> retrieval -> medicalcase -> llm`. All four are driven by the same root cause:

**`MedicalCaseDescriptionService` lives in the `medicalcase` module but calls an LLM.** The `medicalcase` module is meant to be a pure data module (`package-info.java` only allows `core` dependencies) but it has LLM-calling code inside it, which the Modulith slice detector correctly flags as an architectural violation.

A pre-existing plan to fix this exists at `archive/M76-resolve-modulith-cycle.md` (created 2026-06-09) but was never implemented — it was superseded by the M76 *data-sizes* milestone which took the same M-number. The cycle has been the documented "out of scope" for every milestone since M72 and is mentioned by name in the M76 (data-sizes) acceptance criteria and the M77 acceptance criteria. M84 picks up the orphaned M76-resolve-modulith-cycle plan and drives it to completion.

## Goal

Move `MedicalCaseDescriptionService` (interface) and `MedicalCaseDescriptionServiceImpl` (impl) from the `medicalcase` module to the `llm` module, mirroring how `CaseAnalysisService` already lives in the `caseanalysis` module because it calls an LLM. The service interface and method signatures stay the same so every existing caller compiles against the same API — only the import statements move.

After the move:

- `medicalcase` no longer has any LLM-calling code, no `ChatClient` import, no Spring AI dependency → the cycle is broken.
- `caseanalysis`, `embedding`, `llm` still depend on the description service (now in the `llm` module) — that is a one-way dependency and is allowed by their existing `package-info.java` rules.
- `ModulithVerificationIT` passes for the first time since M57; `mvn verify` is green.

## Non-Goals

| Don't | Why |
|---|---|
| Restructure the `caseanalysis` module's `allowedDependencies` | M57 already documented that `caseanalysis` should depend on `core` and `medicalcase` only, and the LLM tool binding is a one-way runtime edge. M84 keeps that intact. |
| Move other LLM-calling services out of `medicalcase` | There are none besides `MedicalCaseDescriptionService` (verified by grep). |
| Rename the service or its methods | All callers depend on the existing signatures. |
| Widen `llm`'s `allowedDependencies` | `llm` already allows `medicalcase` and `embedding`; the moved service lands in a home that already permits the consumers to import it. |
| Touch HIPAA / auth / V2+ migrations / `pom.xml` | global boundaries |

## Changes

### Part 1 — Move the service

| Area | File | Change |
|------|------|--------|
| Service interface | `src/main/java/com/berdachuk/medexpertmatch/llm/service/MedicalCaseDescriptionService.java` (new) | Move the existing interface from `medicalcase.service.*` to `llm.service.*`. Keep the package, the methods, and the Javadoc exactly the same so every existing caller compiles against the same API. |
| Service impl | `src/main/java/com/berdachuk/medexpertmatch/llm/service/impl/MedicalCaseDescriptionServiceImpl.java` (new) | Move the existing impl from `medicalcase.service.impl.*` to `llm.service.impl.*`. Same code, same dependencies — only the package changes. |
| Old service interface | `src/main/java/com/berdachuk/medexpertmatch/medicalcase/service/MedicalCaseDescriptionService.java` (delete) | Delete the old location once all callers are updated. |
| Old service impl | `src/main/java/com/berdachuk/medexpertmatch/medicalcase/service/impl/MedicalCaseDescriptionServiceImpl.java` (delete) | Delete the old location. |

### Part 2 — Update callers (import only, no logic change)

| Area | File | Change |
|------|------|--------|
| Embedding service | `embedding/service/impl/EmbeddingServiceImpl.java` | Change `import com.berdachuk.medexpertmatch.medicalcase.service.MedicalCaseDescriptionService;` to `import com.berdachuk.medexpertmatch.llm.service.MedicalCaseDescriptionService;`. |
| Multi-endpoint embedding | `embedding/service/impl/MultiEndpointEmbeddingServiceImpl.java` | Same import change. |
| Embedding support | `embedding/service/impl/MedicalCaseEmbeddingSupport.java` | Same import change. |
| Ingestion post-processor | `ingestion/service/impl/SyntheticDataPostProcessingServiceImpl.java` | Same import change. |
| LLM harness | `llm/harness/CaseIntakeWorkflowEngine.java` | Same import change. |
| Tests | `embedding/service/impl/MultiEndpointEmbeddingServiceImplTest.java`, `ingestion/service/impl/SyntheticDataPostProcessingReconcileTest.java`, `ingestion/service/impl/SyntheticDataPostProcessingReconcileCaseTest.java`, `ingestion/service/SyntheticDataGeneratorIT.java`, `medicalcase/service/MedicalCaseDescriptionServiceIT.java` | Same import change (test files only). |

### Part 3 — Verify the cycle is broken

| Area | File | Change |
|------|------|--------|
| Modulith test | `mvn verify` | `ModulithVerificationIT.verifyApplicationModuleStructure` passes (currently fails with the four cycles above). |
| Existing tests | `mvn test` | All 872+ unit tests still pass. The moved service has no behaviour change. |
| Manual review | inspect each `package-info.java` | `medicalcase` no longer references `ChatClient`; the LLM slice has one extra dependency. |
| Regression check | M77 plan acceptance | The pre-existing-Module-cycle exception in M77's acceptance criteria is now obsolete; subsequent milestones can drop the exception. |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Create `llm/service/MedicalCaseDescriptionService.java` (new package, identical content) | Pending |
| 2 | Create `llm/service/impl/MedicalCaseDescriptionServiceImpl.java` (new package, identical content) | Pending |
| 3 | Delete `medicalcase/service/MedicalCaseDescriptionService.java` | Pending |
| 4 | Delete `medicalcase/service/impl/MedicalCaseDescriptionServiceImpl.java` | Pending |
| 5 | Update imports in 5 main + 5 test files (Part 2 table) | Pending |
| 6 | Run `mvn test` → expect 872+ green | Pending |
| 7 | Run `mvn verify` → expect `ModulithVerificationIT` green for the first time | Pending |
| 8 | Commit, push, merge to develop, delete feature branch | Pending |
| 9 | Archive M84; update `00-index.md` (drop M84 from Active) | Pending |

## Acceptance criteria

- [ ] `mvn verify` passes; the Modulith test goes green for the first time since M57
- [ ] No `ChatClient` import or any other Spring AI LLM type in the `medicalcase` module
- [ ] `medicalcase` `package-info.java` allowed dependencies unchanged (`core :: *` only)
- [ ] `llm` `package-info.java` allowed dependencies unchanged
- [ ] All 872+ existing unit tests + integration tests pass
- [ ] `MedicalCaseDescriptionService` interface signature unchanged so no caller logic changes
- [ ] The `medicalcase.service` package no longer has any class — either deleted or moved (Lombok `@Service` Spring component will be auto-discovered under the new package)
- [ ] `mvn test jacoco:report` stays clean (no new warnings about missing services)

## Out of scope

- Restructuring the `caseanalysis` module's `allowedDependencies` (M57's intent is preserved as-is)
- Moving other LLM-calling services from `medicalcase` (verified by grep: there are none besides `MedicalCaseDescriptionService`)
- Renaming the service or its methods (callers depend on the existing signatures)
- Refactoring the `M76-resolve-modulith-cycle.md` archived plan — it is the spec; M84 implements it

## References

- `.agents/plans/archive/M76-resolve-modulith-cycle.md` — the pre-existing plan this milestone implements (orphaned by the M76 data-sizes name collision; now resurrected as M84)
- `src/main/java/com/berdachuk/medexpertmatch/medicalcase/service/MedicalCaseDescriptionService.java` — current location
- `src/main/java/com/berdachuk/medexpertmatch/medicalcase/service/impl/MedicalCaseDescriptionServiceImpl.java` — current impl (currently the only thing that pulls `medicalcase -> llm` via the `ChatClient` import, completing the cycle)
- `src/main/java/com/berdachuk/medexpertmatch/caseanalysis/package-info.java` — pattern to mirror (LLM-calling service in a non-core module)
- `src/main/java/com/berdachuk/medexpertmatch/llm/package-info.java` — `llm` allows `medicalcase`, so the moved service can be consumed by `llm` tools without widening the LLM module's deps
- `src/test/java/com/berdachuk/medexpertmatch/ModulithVerificationIT.java` — the test that currently fails
- `src/main/java/com/berdachuk/medexpertmatch/medicalcase/package-info.java` — confirms `medicalcase` should only know about `core`
- `.agents/plans/00-index.md` — add M84 to Active
