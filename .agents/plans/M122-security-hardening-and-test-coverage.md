# M122: Security Hardening and Test Coverage

**Status:** Active (planned 2026-06-16)
**Created:** 2026-06-16
**Priority:** High
**Depends on:** M121 (archived)

## Problem Statement

M121 completed application hardening (probes, readiness indicator, Docker health checks). Two critical gaps remain: (1) 8 REST controllers accept `@RequestBody` without `@Valid` validation, creating an injection/malformed-payload risk; (2) 5 modules have zero unit tests, and 3 Cucumber feature files lack step definitions.

## Goal

Close the highest-risk security and test-coverage gaps before the next feature phase.

## Tasks

### 1. Add `@Valid` input validation to all 8 controllers
- Add `@Valid` to `@RequestBody` parameters in: `MatchOutcomeRestController`, `AdminController`, `ChatController`, `WorkflowCheckpointController`, `AgentQuestionController`, `A2AMessageController`, `MedicalAgentController`, `A2aJsonRpcController`
- Ensure DTOs have proper `jakarta.validation.constraints` annotations (e.g., `@NotBlank`, `@NotNull`, `@Size`, `@Pattern`)
- Add `@Validated` at class level where group validation is needed

### 2. Write unit tests for zero-coverage modules
- **caseanalysis** (7 files): at minimum `CaseAnalysisServiceImplTest`
- **chunking** (14 files): at minimum `RecursiveChunkingStrategyTest`, `AdaptiveChunkingStrategyTest`, `SemanticChunkingStrategyTest`
- **clinicalexperience** (7 files): at minimum `ClinicalExperienceRepositoryTest`
- **facility** (7 files): at minimum `FacilityRepositoryTest`
- **medicalcoding** (12 files): at minimum `ICD10CodeRestControllerTest`

### 3. Complete BDD step definitions for 3 missing feature files
- Implement step definitions for: `clinical-guideline.feature`, `recommendation-engine.feature`, `triage.feature`
- Verify all 9 Cucumber feature files have working step definitions

### 4. Add unit tests for low-coverage core modules
- **graph** (1 test / 16 src): add `GraphQueryServiceTest`
- **retrieval** (4 tests / 35 src): add `MatchingServiceTest`, `RoutingServiceTest`
- **medicalcase** (2 tests / 16 src): add `MedicalCaseServiceTest`
- **evidence** (1 test / 8 src): add `EvidenceServiceTest`
- **doctor** (2 tests / 13 src): add `DoctorServiceTest`

### 5. Audit and harden security configuration
- Add explicit CORS configuration for production profiles
- Verify CSRF protection is appropriate for the Thymeleaf web UI
- Audit `LocalSecurityConfig`, `DockerSecurityConfig`, `LocalPermitAllSecurityConfig`

### 6. Update memory bank
- Append a progress entry.

### 7. Update `00-index.md` — register M122.

## Acceptance Criteria

- [ ] All 8 controllers validate `@RequestBody` with `@Valid`
- [ ] Zero-coverage modules have at least 1 unit test each
- [ ] All 9 Cucumber feature files have working step definitions
- [ ] Low-coverage modules have improved unit test coverage
- [ ] CORS/CSRF configuration is explicit and documented
- [ ] `mvn verify` passes
- [ ] No regressions in existing tests

## References

- `src/main/java/.../retrieval/rest/MatchOutcomeRestController.java`
- `src/main/java/.../core/rest/AdminController.java`
- `src/main/java/.../chat/rest/ChatController.java`
- `src/main/java/.../llm/rest/WorkflowCheckpointController.java`
- `src/main/java/.../llm/rest/AgentQuestionController.java`
- `src/main/java/.../llm/rest/A2AMessageController.java`
- `src/main/java/.../llm/rest/MedicalAgentController.java`
- `src/main/java/.../llm/rest/A2aJsonRpcController.java`
- `src/test/java/.../bdd/` — Cucumber feature files and step definitions

## Out of Scope

- New feature development.
- Production deployment at scale.
- Multi-tenancy or HIPAA certification.
- GPU fine-tune work (M60, deferred).
