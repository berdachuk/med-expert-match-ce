# Domain Modeling

## Description
Creating and maintaining domain entities, value objects, DTOs, enums, and filter/wrapper types across all 17 modules. Covers entity ownership rules and record-vs-class decisions.

## When to use
- Creating a new domain entity or value object
- Adding fields, validation, or constructors to existing entities
- Deciding whether a type belongs as a record, class, or enum
- Understanding which module owns which domain models
- Answering: "Where should this entity live?"

## Instructions

### Entity Ownership

| Module | Owned Entities |
|--------|---------------|
| doctor | Doctor, MedicalSpecialty |
| medicalcase | MedicalCase, CaseType, UrgencyLevel |
| medicalcoding | ICD10Code, Procedure |
| facility | Facility |
| clinicalexperience | ClinicalExperience |
| caseanalysis | CaseAnalysisResult |
| retrieval | DoctorMatch, FacilityMatch, ConsultationMatch, ScoreResult, RouteScoreResult, PriorityScore, MatchOptions, RoutingOptions |
| llm | AnalyzeJobStatus, MatchJobStatus, PrioritizeJobStatus, RouteJobStatus |
| evidence | PubMedArticle |
| chunking | DocumentChunk |
| documents | SourceDocumentEntity |

`core` and `system` modules own NO domain entities.

### Records vs Classes

- **Java records** for domain entities (immutable data carriers) — all current entities are records
- **Regular classes** when behavior or mutability is needed (service implementations, builders)
- **Lombok** on service/impl classes only (`@Slf4j`, `@RequiredArgsConstructor`); NOT on domain records

### Naming and Structure

- Entity file: `{EntityName}.java` in `domain/` directory
- If a module owns multiple entities, one file per entity
- Nested records (e.g., `PotentialDiagnosis` inside `CaseAnalysisResult`) for tightly-coupled value types
- DTOs go in `domain/dto/` subdirectory
- Query filters go in `domain/filters/` subdirectory
- Response wrappers go in `domain/wrappers/` subdirectory

### Database Mapping

- DB columns: `snake_case` (e.g., `doctor_id`, `created_at`)
- JSON/API fields: `camelCase` (e.g., `doctorId`, `messageType`)
- Entity field names: `camelCase` (e.g., `private String doctorId`)

## Boundaries
- Do NOT create domain entities in `core` — it owns no domain
- Do NOT create cross-module entity references — use IDs, not object references
- Do NOT add Lombok annotations to domain records
