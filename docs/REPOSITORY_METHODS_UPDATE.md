# Repository Methods Update

**Last Updated:** 2026-01-19  
**Status:** ✅ **COMPLETED**

## Overview

Added `findAll()` methods to repositories to support database existence checks in the synthetic data generator
optimization.

## Changes Made

### 1. ICD10CodeRepository

**File**: `src/main/java/com/berdachuk/medexpertmatch/medicalcoding/repository/ICD10CodeRepository.java`

**Added Method**:

```java
/**
 * Finds all ICD-10 codes.
 *
 * @return List of all ICD-10 codes
 */
List<ICD10Code> findAll();
```

**Implementation**:
`src/main/java/com/berdachuk/medexpertmatch/medicalcoding/repository/impl/ICD10CodeRepositoryImpl.java`

```java

@Override
public List<ICD10Code> findAll() {
    return namedJdbcTemplate.query(findAllSql, Map.of(), icd10CodeMapper);
}
```

**SQL File**: `src/main/resources/sql/medicalcoding/findAll.sql`

```sql
SELECT id, code, description, category, parent_code, related_codes
FROM medexpertmatch.icd10_codes
ORDER BY code
```

### 2. MedicalSpecialtyRepository

**Status**: ✅ Already existed - no changes needed

**File**: `src/main/java/com/berdachuk/medexpertmatch/doctor/repository/MedicalSpecialtyRepository.java`

**Existing Method**:

```java
/**
 * Finds all medical specialties.
 *
 * @return List of all medical specialties
 */
List<MedicalSpecialty> findAll();
```

**Implementation**:
`src/main/java/com/berdachuk/medexpertmatch/doctor/repository/impl/MedicalSpecialtyRepositoryImpl.java`

```java

@Override
public List<MedicalSpecialty> findAll() {
    return namedJdbcTemplate.query(findAllSql, medicalSpecialtyMapper);
}
```

**SQL File**: `src/main/resources/sql/doctor/medicalspecialty/findAll.sql`

```sql
SELECT id, name, normalized_name, description, icd10_code_ranges, related_specialties
FROM medexpertmatch.medical_specialties
ORDER BY name
```

## Usage in SyntheticDataGenerator

Both `findAll()` methods are used in the synthetic data generator to check if sufficient data already exists in the
database before making LLM calls:

### ICD-10 Codes Check

```java
// In generateIcd10Codes()
int existingCount = icd10CodeRepository.findAll().size();
int targetCount = calculateTargetIcd10Count(size);

if(existingCount >=targetCount){
        log.

info("Sufficient ICD-10 codes exist in database ({} >= {}), loading existing codes and skipping LLM generation",
     existingCount, targetCount);

List<ICD10Code> existingCodes = icd10CodeRepository.findAll();
extendedIcd10Codes =existingCodes.

stream()
            .

map(ICD10Code::code)
            .

distinct()
            .

limit(targetCount)
            .

collect(Collectors.toList());
        return;
        }
```

### Medical Specialties Check

```java
// In generateMedicalSpecialties()
int existingCount = medicalSpecialtyRepository.findAll().size();
int targetCount = calculateTargetSpecialtyCount(size);

if(existingCount >=targetCount){
        log.

info("Sufficient medical specialties exist in database ({} >= {}), skipping LLM generation",
     existingCount, targetCount);
    return;
            }
```

## Benefits

1. **Reduced LLM Calls**: Eliminates unnecessary LLM API calls when data already exists
2. **Database Reuse**: Leverages existing data instead of regenerating
3. **Performance**: Faster generation when data is already available
4. **Cost Savings**: Reduces API costs for repeated generations

## Testing

Both methods are tested through:

- Integration tests in `SyntheticDataGeneratorIT`
- Database existence checks in `generateIcd10Codes()` and `generateMedicalSpecialties()`

## Related Documentation

- [Synthetic Data Generator](./SYNTHETIC_DATA_GENERATOR.md)
- [Architecture](./ARCHITECTURE.md)
