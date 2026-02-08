# Synthetic Data Generator

## Overview

The Synthetic Data Generator is a comprehensive service for generating realistic, anonymized medical test data for the
MedExpertMatch system. It creates FHIR R5 compliant resources including doctors, medical cases, clinical experiences,
facilities, and builds the Apache AGE graph database.

## Features

### Data Generation

The generator creates the following types of synthetic medical data:

- **Doctors**: Medical professionals with specialties, affiliations, and availability status
- **Medical Cases**: Patient cases with symptoms, diagnoses, ICD-10 codes, and urgency levels
- **Clinical Experiences**: Historical treatment records linking doctors to cases with outcomes and ratings
- **Facilities**: Healthcare facilities with types, capabilities, and capacity information
- **Medical Specialties**: Specialty definitions with ICD-10 code ranges
- **ICD-10 Codes**: Diagnosis codes with descriptions and categories
- **Procedures**: Medical procedures mapped to specialties

### Data Sizes

The generator supports multiple data size presets with estimated generation times based on real performance statistics:

- **tiny**: 3 doctors, 15 cases (~2 minutes)
- **micro**: 5 doctors, 150 cases (~36 minutes)
- **mini**: 10 doctors, 300 cases (~72 minutes / 1.2 hours)
- **compact**: 100 doctors, 5,000 cases (~1,200 minutes / 20 hours)
- **small**: 250 doctors, 12,500 cases (~3,000 minutes / 50 hours)
- **standard**: 500 doctors, 25,000 cases (~6,000 minutes / 100 hours)
- **medium**: 1,000 doctors, 50,000 cases (~12,000 minutes / 200 hours)
- **large**: 5,000 doctors, 250,000 cases (~60,000 minutes / 1,000 hours)
- **huge**: 10,000 doctors, 500,000 cases (~120,000 minutes / 2,000 hours)

**Note**: Estimated times are based on real statistics from micro generation runs. The primary bottleneck is LLM-based
description generation, which averages approximately 11.9 seconds per case. Times include overhead for other
operations (doctors, embeddings, graph building, etc.).

### Key Capabilities

#### FHIR R5 Compliance

All generated data follows FHIR R5 resource specifications:

- Uses proper FHIR resource structures
- Includes FHIR coding systems (ICD-10, SNOMED CT)
- Maintains FHIR data types and references

#### Data Anonymization

- No Protected Health Information (PHI) is generated
- All patient identifiers are synthetic
- Compliant with HIPAA requirements for test data

#### Realistic Data Patterns

- Uses Datafaker library for realistic names, addresses, and demographics
- Maintains referential integrity between entities
- Creates realistic relationships (doctor-case, doctor-facility, case-conditions)
- Generates appropriate clinical experiences based on doctor expertise

#### AI-Enhanced Descriptions

- Uses LLM (MedGemma) to generate comprehensive medical case descriptions
- Generates descriptions as a separate step before embedding generation
- Falls back to simple text concatenation if LLM fails
- Descriptions are stored in the `abstract` field of medical cases

#### Vector Embeddings

- Generates vector embeddings for semantic search using case descriptions
- Uses stored descriptions (from description generation step) for embedding creation
- Configurable thread pool for parallel embedding generation
- Embeddings are generated after descriptions are complete

#### Graph Database Integration

- Automatically builds Apache AGE graph after data generation
- Creates vertices for doctors, cases, ICD-10 codes, specialties, and facilities
- Creates relationships (TREATED, SPECIALIZES_IN, HAS_CONDITION, etc.)
- Enables graph-based queries and recommendations

#### Progress Tracking

- Real-time progress updates via `SyntheticDataGenerationProgressService`
- Supports cancellation of long-running generation jobs
- Provides detailed trace logs for debugging
- Progress percentage updates throughout generation process
- Progress flow:
    - 0%: Initializing
    - 2%: Clearing (if enabled)
    - 5%: ICD-10 Codes
    - 10%: Specialties
    - 12%: Procedures
    - 15%: Facilities
    - 20%: Doctors
    - 40%: Medical Cases
    - 55%: Descriptions (generating medical case descriptions)
    - 70-90%: Embeddings (generating vector embeddings)
    - 90%: Clinical Experiences
    - 95%: Graph Building
    - 100%: Complete

#### Batch Processing

- Configurable batch sizes for efficient database operations
- Processes data in chunks to manage memory
- Optimized for large-scale data generation

#### External Data Sources

- Loads reference data from CSV files:
    - Medical specialties
    - ICD-10 codes
    - Procedures
    - Facility types
    - Complexity levels
    - Outcomes
    - Symptoms
    - And more
- Supports hierarchical data reuse (e.g., ICD-10 codes for different sizes)

## Configuration

### Application Properties

```yaml
medexpertmatch:
  synthetic-data:
    batch-size: 5000
    progress-update-interval: 100
    embedding:
      thread-pool-size: 10
    llm:
      timeout-seconds: 60
    data-files:
      medical-specialties: classpath:/data/medical-specialties.csv
      icd10-codes: classpath:/data/icd10-codes.csv
      procedures: classpath:/data/procedures.csv
      # ... other data files
```

### Data File Locations

All CSV data files are located in `src/main/resources/data/`:

- `medical-specialties.csv`
- `icd10-codes.csv`
- `procedures.csv`
- `facility-types.csv`
- `complexity-levels.csv`
- `outcomes.csv`
- `availability-statuses.csv`
- `severities.csv`
- `symptoms.csv`
- `encounter-classes.csv`
- `encounter-types.csv`
- `complications.csv`
- `facility-capabilities.csv`
- `specialty-procedures.csv`

## Usage

### Programmatic Usage

```java

@Autowired
private SyntheticDataGenerator syntheticDataGenerator;

// Generate small dataset, clearing existing data
syntheticDataGenerator.

generateTestData("small",true);

// Generate with progress tracking
String jobId = "generation-job-123";
syntheticDataGenerator.

generateTestData("medium",false,jobId);
```

### API Endpoints

The generator is exposed via REST API endpoints (see `SyntheticDataController`):

- `POST /api/v1/synthetic-data/generate?size={size}&clear={clear}` - Generate synthetic data
    - `size`: Data size (tiny, micro, mini, compact, small, standard, medium, large, huge)
    - `clear`: Whether to clear existing data first (default: false)
- `GET /api/v1/synthetic-data/progress/{jobId}` - Get generation progress
- `POST /api/v1/synthetic-data/cancel/{jobId}` - Cancel generation job
- `GET /api/v1/synthetic-data/sizes` - Get available data size configurations with estimated times

## Generation Process

1. **Initialization**: Load reference data from CSV files
2. **Clear Data** (optional): Remove existing synthetic data
3. **Generate Reference Data**:
    - ICD-10 codes
    - Medical specialties
    - Procedures
4. **Generate Entities**:
    - Facilities
    - Doctors (with specialties and facility affiliations)
    - Medical cases (with ICD-10 codes and symptoms)
5. **Generate Descriptions**: Create comprehensive medical case descriptions using LLM
    - Uses `MedicalCaseDescriptionService` to generate enhanced descriptions
    - Falls back to simple text concatenation if LLM fails
    - Descriptions stored in the `abstract` field of medical cases
6. **Generate Embeddings**: Create vector embeddings for medical cases
    - Uses stored descriptions from the previous step
    - Generates 1536-dimensional embeddings for semantic search
7. **Generate Clinical Experiences**: Link doctors to cases with outcomes and ratings
8. **Build Graph**: Create Apache AGE graph with vertices and relationships

## Performance Optimizations

- **Parallel Embedding Generation**: Uses thread pool for concurrent embedding creation
- **Batch Database Operations**: Processes data in configurable batches
- **Caching**: Caches extended data lists for hierarchical reuse
- **Progress Updates**: Configurable update intervals to balance performance and feedback
- **Cancellation Support**: Allows stopping long-running generation jobs

## Metrics

The generator exposes Micrometer metrics:

- `synthetic.data.embeddings.generated` - Counter for embeddings created
- `synthetic.data.embeddings.duration` - Timer for embedding generation time
- `synthetic.data.doctors.generated` - Counter for doctors created
- `synthetic.data.cases.generated` - Counter for cases created

## Testing

The generator is tested via `SyntheticDataGeneratorIT` integration test:

- Tests all data size presets
- Verifies data integrity and relationships
- Validates graph building
- Tests cancellation functionality
- Verifies progress tracking

## Dependencies

- **Datafaker**: Realistic synthetic data generation
- **FHIR R5**: HL7 FHIR resource models
- **Spring AI**: LLM integration for text enhancement
- **Micrometer**: Metrics collection
- **Apache AGE**: Graph database integration

## Limitations

- Maximum batch sizes enforced:
    - Doctors: 100,000 per batch
    - Cases: 1,000,000 per batch
    - Facilities: 10,000 per batch
- LLM timeout: 60 seconds (configurable via `medexpertmatch.synthetic-data.llm.timeout-seconds`)
- Embedding generation requires AI provider configuration
- Description generation is the primary bottleneck - large datasets may take significant time

## Future Enhancements

Potential improvements:

- Support for additional FHIR resource types
- More sophisticated relationship generation
- Custom data size configurations
- Export to FHIR Bundle format
- Import from external data sources
