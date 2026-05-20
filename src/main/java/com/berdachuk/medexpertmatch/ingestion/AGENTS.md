# Ingestion Module

Data ingestion via FHIR adapters and synthetic data generation for bootstrapping the system.

## Purpose

- FHIR adapters (`FhirPatientAdapter`, `FhirEncounterAdapter`, `FhirConditionAdapter`, `FhirObservationAdapter`, `FhirBundleAdapter`)
- Synthetic data generation via `SyntheticDataGenerationService` and domain-specific generators
- `SyntheticDataBootstrapService` — coordinates multi-module data population
- `SyntheticDataPostProcessingService` — builds graph relationships and embeddings post-generation
- REST endpoints for data ingestion via `SyntheticDataController`

## Module Dependencies

`@ApplicationModule(allowedDependencies = {"core", "doctor", "medicalcase", "clinicalexperience", "medicalcoding", "facility", "graph", "embedding"})`

## Conventions

- FHIR adapters implement the adapter pattern: FHIR Resource → internal domain entity
- Synthetic data generators produce anonymized records only (never real patient data)
- Bootstrap order: domain entities first → clinical experience → graph relationships → embeddings
- Use `SyntheticDataGenerationProgressService` to track long-running generation jobs

## Constraints

- Never use real patient data in synthetic generation
- FHIR adapters handle only R5 format; validate FHIR version before processing
- Generation must be idempotent — use MERGE patterns for graph, upsert for relational

## Related Skills

- `domain-modeling` — entity ownership across modules
- `graph-db` — graph relationship building during post-processing
- `db-migrations` — schema dependencies for ingestion tables
