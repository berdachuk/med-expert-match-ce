# M76: Resolve Pre-Existing Spring Modulith Cycle

**Status:** Active (in progress 2026-06-09)
**Created:** 2026-06-09
**Depends on:** M75 (archived — establishes the test infrastructure we now need clean test output from); M57 (archived — `caseanalysis -> llm` boundary was the original intent).

## Problem Statement

`mvn verify` currently fails on the pre-existing `ModulithVerificationIT` test
(`org.springframework.modulith.core.Violations`). The Spring Modulith slice
detector reports two cycles (verified clean on main, 2026-06-09):

1. **Cycle A** — `caseanalysis -> medicalcase -> llm -> caseanalysis`
   - `caseanalysis.CaseAnalysisService.analyzeCase(MedicalCase)` takes a
     `medicalcase.MedicalCase` parameter → `caseanalysis -> medicalcase`.
   - `llm.CaseIntakeWorkflowEngine.enrichCase(MedicalCase)` and
     `llm.EmbeddingGeneratorServiceImpl` call
     `medicalcase.MedicalCaseDescriptionService` → `llm -> medicalcase`.
   - `llm.CaseAnalysisAgentTools` calls `caseanalysis.CaseAnalysisService`
     → `llm -> caseanalysis`.
2. **Cycle B** — `embedding -> medicalcase -> llm -> embedding`
   - `embedding.EmbeddingServiceImpl` and
     `embedding.MultiEndpointEmbeddingServiceImpl` both take
     `medicalcase.MedicalCaseDescriptionService` as a constructor
     dependency → `embedding -> medicalcase`.
   - `medicalcase.MedicalCaseDescriptionServiceImpl` (a
     `@Service` that wraps a Spring AI `ChatClient`) is detected as
     `medicalcase -> llm` because it uses `ChatClient` (Spring AI type
     that the slice detector traces into the LLM slice).
   - `embedding` indirectly depends on LLM utilities (e.g. via
     `LlmCallLimiter` from `core`, plus Spring AI types).

The two cycles are driven by the same root cause:
**`MedicalCaseDescriptionService` lives in the `medicalcase` module but
calls an LLM.** The medicalcase module is meant to be a pure data module
(its `package-info.java` only allows `core` dependencies) but it has
LLM-calling code inside it, which the Modulith slice detector correctly
flags as an architectural violation.

## Goal

Move `MedicalCaseDescriptionService` and its `Impl` from the `medicalcase`
module to a module where LLM-calling is allowed (`llm` is the natural
home, mirroring how `CaseAnalysisService` already lives in `caseanalysis`
because it calls an LLM). The service interface stays the same, so all
callers (embedding, ingestion, llm harness) continue to work without
code changes — only the import statements move.

After the move:

- `medicalcase` no longer has any LLM-calling code, no `ChatClient`
  import, no Spring AI dependency → the cycle is broken.
- `caseanalysis`, `embedding`, `llm` still depend on the description
  service (now in the `llm` module) — that is a one-way dependency and
  is allowed by their existing `package-info.java` rules.
- `ModulithVerificationIT` passes; `mvn verify` goes green.

## Changes

### Part 1 — Move the service

| Area | File | Change |
|------|------|--------|
| Service interface | `src/main/java/.../llm/service/MedicalCaseDescriptionService.java` (new) | Move the existing interface from `medicalcase.service.*` to `llm.service.*`. Keep the package, the methods, and the Javadoc exactly the same so every existing caller compiles against the same API. |
| Service impl | `src/main/java/.../llm/service/impl/MedicalCaseDescriptionServiceImpl.java` (new) | Move the existing impl from `medicalcase.service.impl.*` to `llm.service.impl.*`. Same code, same dependencies — only the package changes. |
| Old service files | `src/main/java/.../medicalcase/service/MedicalCaseDescriptionService.java` (delete) | Delete the old location once all callers are updated. |
| Old service impl | `src/main/java/.../medicalcase/service/impl/MedicalCaseDescriptionServiceImpl.java` (delete) | Delete the old location. |

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
| Modulith test | run `mvn verify` | Confirm `ModulithVerificationIT.verifyApplicationModuleStructure` passes (currently fails with the two cycles above). |
| Existing tests | run `mvn test` | All 1,400+ unit tests still pass. The moved service has no behaviour change. |
| Manual review | inspect each `package-info.java` | `medicalcase` no longer references `ChatClient`; the LLM slice has one extra dependency. |

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Create `llm/service/MedicalCaseDescriptionService.java` (new package, identical content) | Pending |
| 2 | Create `llm/service/impl/MedicalCaseDescriptionServiceImpl.java` (new package, identical content) | Pending |
| 3 | Delete `medicalcase/service/MedicalCaseDescriptionService.java` | Pending |
| 4 | Delete `medicalcase/service/impl/MedicalCaseDescriptionServiceImpl.java` | Pending |
| 5 | Update imports in 5 main + 5 test files (Part 2 table) | Pending |
| 6 | Run `mvn test` → expect 1,400+ green | Pending |
| 7 | Run `mvn verify` → expect `ModulithVerificationIT` green for the first time | Pending |
| 8 | Document the move in this plan's "Done" status + add to `00-index.md` archive | Pending |

## Acceptance criteria

- [ ] `mvn verify` passes (the Modulith test goes green for the first time since M57)
- [ ] No `ChatClient` import or any other Spring AI LLM type in `medicalcase` module
- [ ] `medicalcase` `package-info.java` allowed dependencies unchanged (`core :: *` only)
- [ ] `llm` `package-info.java` allowed dependencies unchanged
- [ ] All 1,400+ existing unit tests + integration tests pass
- [ ] `MedicalCaseDescriptionService` interface signature unchanged so no caller logic changes
- [ ] The `medicalcase.service` package no longer has any class — either deleted or moved (Lombok `@Service` Spring component will be auto-discovered under the new package)
- [ ] `mvn test jacoco:report` stays clean (no new warnings about missing services)

## Out of scope

- Restructuring the `caseanalysis` module's `allowedDependencies` — M57 already
  documented the intent that `caseanalysis` should depend on `core` and
  `medicalcase` only, and that the LLM tool binding is a one-way runtime
  edge. M76 keeps that intact.
- Moving other LLM-calling services from `medicalcase` (there are none
  besides `MedicalCaseDescriptionService`).
- Renaming the service or its methods (callers depend on the existing
  signatures).

## References

- `src/main/java/com/berdachuk/medexpertmatch/medicalcase/service/MedicalCaseDescriptionService.java` — current location
- `src/main/java/com/berdachuk/medexpertmatch/medicalcase/service/impl/MedicalCaseDescriptionServiceImpl.java` — current impl
- `src/main/java/com/berdachuk/medexpertmatch/caseanalysis/package-info.java` — pattern to mirror (LLM-calling service in a non-core module)
- `src/main/java/com/berdachuk/medexpertmatch/llm/package-info.java` — `llm` allows `medicalcase`, so the moved service can be consumed by `llm` tools without widening the LLM module's deps
- `src/test/java/com/berdachuk/medexpertmatch/ModulithVerificationIT.java` — the test that currently fails
- `src/main/java/com/berdachuk/medexpertmatch/medicalcase/package-info.java` — confirms `medicalcase` should only know about `core`
- `.agents/plans/00-index.md` — add M76 to Active table
