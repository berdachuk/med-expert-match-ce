# M91: M77 Fixes and Session Compaction Wiring

**Status:** Active (planned 2026-06-10)
**Created:** 2026-06-10
**Depends on:** M90 (archived — M77 runtime-measured estimates implemented)

## Problem Statement

M90 implemented the M77 runtime-measured synthetic data estimates feature, but two gaps remain:

1. **clinicalExperienceMs not wired**: The `clinicalExperienceMs` field exists in the record/DB and is initialized to 0 in `startRunTracking()`, but no code ever records the actual elapsed time for clinical experience generation. The method runs in `ClinicalExperienceGeneratorServiceImpl`, outside `SyntheticDataPostProcessingServiceImpl`. All run records persist `clinical_experience_ms = 0`.

2. **Duplicate @EnableScheduling**: `@EnableScheduling` appears on both `MedExpertMatchApplication` and `HarnessConfiguration`. The main class annotation already enables scheduling globally; the one on `HarnessConfiguration` is redundant and creates ambiguity.

## Goal

1. Wire `recordClinicalExperienceDuration()` into `SyntheticDataGenerator` around the clinical experience generation call.
2. Remove redundant `@EnableScheduling` from `HarnessConfiguration`.
3. Add a unit test asserting `clinicalExperienceMs > 0` after recording.
4. `mvn verify` green.

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Add `recordClinicalExperienceDuration` to interface + impl | Done |
| 2 | Wire timing in `SyntheticDataGenerator.generateClinicalExperiences` | Done |
| 3 | Add unit test for `recordClinicalExperienceDuration` | Done |
| 4 | Remove duplicate `@EnableScheduling` from `HarnessConfiguration` | Done |
| 5 | `mvn verify` green | Done |
| 6 | Archive this plan | Pending |

## Acceptance Criteria

- [x] `clinicalExperienceMs` is recorded for every synthetic data generation run
- [x] `HarnessConfiguration` no longer has `@EnableScheduling`
- [x] All unit + IT tests pass