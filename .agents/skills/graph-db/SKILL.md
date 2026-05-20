# Graph Database

## Description
Apache AGE Cypher query patterns, graph schema operations, and `GraphService` usage conventions. Covers `graph`, `retrieval`, and `ingestion` modules.

## When to use
- Writing or modifying Cypher queries for Apache AGE
- Building medical graph relationships (doctor-case, doctor-facility, etc.)
- Debugging graph query results or MERGE behavior
- Adding new vertex labels or edge types
- Answering: "How do I query the graph?"

## Instructions

### GraphService Interface (mandatory gateway)

ALL graph operations must go through `GraphService` interface:
```java
graphService.executeCypher(cypherQuery, parameters)
```

Never execute Cypher queries directly via JDBC.

### Parameter Embedding

- Parameters are embedded in the Cypher string via `$paramName` syntax
- `GraphServiceImpl.embedParameters()` auto-escapes strings and handles nulls
- Null values render as `null` (unquoted)
- Numbers and booleans are embedded as-is
- **Do NOT pass parameters as separate arguments** to `executeCypher()`

### MERGE Patterns (critical rule)

When using MERGE, include ALL properties in the MERGE clause itself:

```cypher
-- CORRECT
MERGE (d:Doctor {id: $doctorId, name: $name, email: $email})

-- INCORRECT (Apache AGE parser does not handle MERGE ... SET)
MERGE (d:Doctor {id: $doctorId})
SET d.name = $name, d.email = $email
```

### Null Handling

Always provide defaults for nullable parameters:
```java
Map.of("name", name != null ? name : "")
```

### Idempotent Operations

- Use MERGE for ALL vertex and edge creation
- Graph operations gracefully degrade — failures return empty results, do NOT throw exceptions
- Testing: graph operations tested with real Apache AGE in Testcontainers

### Graph Schema

| Vertex Type | Properties | Source Module |
|-------------|-----------|---------------|
| Doctor | id, name, specialty, ... | doctor |
| MedicalCase | id, diagnoses, urgency, ... | medicalcase |
| Facility | id, name, location, ... | facility |
| ICD10Code | code, description | medicalcoding |

| Edge Type | From | To | Meaning |
|-----------|------|----|---------|
| TREATED | Doctor | MedicalCase | Doctor treated this case type |
| EXPERIENCED | Doctor | ClinicalExperience | Doctor's experience record |
| LOCATED_AT | Doctor | Facility | Doctor works at facility |
| HAS_DIAGNOSIS | MedicalCase | ICD10Code | Case has this ICD-10 diagnosis |

### Graph Visualization

- `GraphVisualizationService` for rendering graph data to UI
- `GraphQueryService` for complex graph traversals and analytics
- Graph visualization data does NOT include PHI

## Boundaries
- Do NOT bypass `GraphService` for direct JDBC graph queries
- Do NOT use `MERGE ... SET` pattern (unsupported by AGE parser)
- Do NOT add graph operations to modules other than `graph`, `ingestion`, or `retrieval`
- Do NOT log raw Cypher queries containing medical data
