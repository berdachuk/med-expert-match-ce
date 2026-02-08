# MedExpertMatch Testing Guide

**Last Updated:** 2026-01-20

## Overview

This guide describes testing patterns and best practices for MedExpertMatch, with a focus on medical-specific test data
considerations.

## Testing Principles

- **Test-Driven Development**: Write tests first
- **Integration Tests Preferred**: Use Testcontainers with PostgreSQL
- **Real Database**: Never use H2 or in-memory databases
- **Test Independence**: Each test prepares its own data

## Medical Test Data

### Anonymization Requirements

- All patient data must be anonymized in tests
- Use synthetic test data, not real patient data
- No PHI in test fixtures or assertions

### Test Data Examples

```java
// Example: Anonymized medical case
MedicalCase testCase = new MedicalCase(
                "case123",
                "CASE-001",                    // Case ID (not patient ID)
                "PATIENT-ANON-001",            // Anonymized patient ID
                "Chest pain",                  // Chief complaint
                "Acute myocardial infarction", // Diagnosis
                List.of("I21.9"),              // ICD-10 codes
                // ... other fields
        );
```

## Test Structure

Follow these testing patterns:

- Extend `BaseIntegrationTest` for database tests
- Use Testcontainers for PostgreSQL
- Mock MedGemma API calls (don't call real APIs in tests)

## Embedding Tests

Integration tests for vector embeddings:

- **EmbeddingServiceIT**: Tests embedding generation (single and batch)
    - `testGenerateEmbedding()` - Single embedding generation
    - `testGenerateEmbeddingsBatch()` - Batch embedding generation
    - `testGenerateEmbeddingAsFloatArray()` - Float array format
    - `testGenerateEmbeddingEmptyText()` - Edge case handling

- **MedicalCaseRepositoryEmbeddingIT**: Tests repository embedding methods
    - `testFindWithoutEmbeddings()` - Find cases needing embeddings
    - `testUpdateEmbedding()` - Update case with embedding
    - `testUpdateEmbeddingWithSmallerDimension()` - Dimension normalization
    - `testFindWithoutEmbeddingsExcludesCasesWithEmbeddings()` - Query correctness

- **TestDataGeneratorEmbeddingIT**: Tests embedding generation in test data flow
    - `testGenerateEmbeddings()` - Standalone embedding generation
    - `testGenerateEmbeddingsInTestDataFlow()` - Integration with test data generation
    - `testGenerateEmbeddingsHandlesEmptyCases()` - Edge case handling

All embedding tests use mocked `EmbeddingModel` to avoid real API calls.

## Vector Similarity Testing

Vector similarity is tested as part of `SemanticGraphRetrievalService` tests:

- Tests verify cosine similarity calculation using pgvector operator `<=>`
- Tests handle missing embeddings gracefully (return neutral score)
- Tests compare query case with doctor's historical cases

## Text Input and Case Search Testing

### MedicalAgentController Tests

**File**: `MedicalAgentControllerIT.java`

Tests for the `matchFromText` endpoint:

- `testMatchFromText_WithRequiredCaseText()` - Tests basic text input with only required `caseText` parameter
    - Verifies case creation with correct chief complaint
    - Verifies default case type (INPATIENT)
    - Verifies case is persisted to database

- `testMatchFromText_WithAllOptionalParameters()` - Tests text input with all optional parameters
    - Verifies `symptoms`, `additionalNotes`, `patientAge`, `caseType` are saved correctly
    - Verifies all parameters are persisted

- `testMatchFromText_Validation_MissingCaseText()` - Tests validation error when `caseText` is missing
    - Verifies `IllegalArgumentException` is thrown

- `testMatchFromText_Validation_EmptyCaseText()` - Tests validation error when `caseText` is empty
    - Verifies `IllegalArgumentException` is thrown with appropriate message

- `testMatchFromText_WithInvalidCaseType()` - Tests that invalid `caseType` defaults to INPATIENT
    - Verifies graceful handling of invalid enum values

- `testMatchFromText_WithDifferentCaseTypes()` - Tests SECOND_OPINION and CONSULT_REQUEST case types
    - Verifies all case types are handled correctly

### MedicalCaseRepository Search Tests

**File**: `MedicalCaseRepositoryIT.java`

Tests for the `search()` method:

- `testSearch_ByQuery()` - Tests text search in chiefComplaint, symptoms, additionalNotes
    - Verifies case-insensitive search
    - Verifies partial matching

- `testSearch_BySpecialty()` - Tests filtering by required specialty
    - Verifies exact match filtering

- `testSearch_ByUrgencyLevel()` - Tests filtering by urgency level
    - Verifies enum value filtering

- `testSearch_WithAllFilters()` - Tests combined filters (query + specialty + urgency)
    - Verifies all filters work together
    - Verifies no results when filters don't match

- `testSearch_WithMaxResults()` - Tests result limiting
    - Verifies `LIMIT` clause works correctly

- `testSearch_EmptyQuery()` - Tests null/empty query handling
    - Verifies all cases returned when query is null/empty

- `testSearch_CaseInsensitive()` - Tests case-insensitive search
    - Verifies `ILIKE` operator works correctly

- `testSearch_PartialMatch()` - Tests partial text matching
    - Verifies substring matching in multiple fields

- `testSearch_OrderByCreatedAt()` - Tests result ordering
    - Verifies results ordered by `created_at DESC` (most recent first)

### MatchController Tests

**File**: `MatchControllerIT.java`

Tests for the case search endpoint (`GET /api/cases/search`):

- `testSearchCases_ByQuery()` - Tests text search via REST endpoint
- `testSearchCases_BySpecialty()` - Tests specialty filtering via REST endpoint
- `testSearchCases_ByUrgencyLevel()` - Tests urgency level filtering via REST endpoint
- `testSearchCases_WithAllFilters()` - Tests combined filters via REST endpoint
- `testSearchCases_WithMaxResults()` - Tests result limiting via REST endpoint
- `testSearchCases_EmptyQuery()` - Tests null/empty query handling via REST endpoint
- `testSearchCases_CaseInsensitive()` - Tests case-insensitive search via REST endpoint
- `testSearchCases_DefaultMaxResults()` - Tests default maxResults parameter

All tests follow TDD principles and use Testcontainers with PostgreSQL.

## Related Documentation

- [Development Guide](DEVELOPMENT_GUIDE.md)
- [Architecture](ARCHITECTURE.md)
- [Synthetic Data Generator](SYNTHETIC_DATA_GENERATOR.md) - Comprehensive feature description including vector
  embeddings
- [Find Specialist Flow](FIND_SPECIALIST_FLOW.md) - Text input endpoint and case search documentation

---

*Last updated: 2026-01-20*
