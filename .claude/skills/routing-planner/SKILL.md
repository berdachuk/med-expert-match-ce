---
name: routing-planner
description: Facility routing optimization, multi-facility scoring, and geographic routing for medical cases
---

# Routing Planner

This skill guides you on how to optimize facility routing, score multiple facilities, and plan geographic routing for
medical cases.

## Overview

The Routing Planner skill helps you:

1. **Query candidate facilities** - Find facilities suitable for case routing
2. **Score facility routes** - Calculate route scores using multiple factors
3. **Optimize routing** - Determine optimal facility routing based on case requirements

## When to Use This Skill

Use this skill when:

- User needs to route a case to a facility
- You need to find candidate facilities for routing
- You need to score and compare facilities
- You need geographic routing optimization
- You need multi-facility routing planning

## Available Tools

### Graph Query Candidate Centers: `graph_query_candidate_centers`

Queries the graph to find candidate facilities for a condition:

**Parameters:**

- `conditionCode`: ICD-10 code for the condition
- `maxResults`: Maximum number of facilities (default: 10)

**Returns:**

- List of candidate facilities with capabilities

**Usage:**

```
graph_query_candidate_centers(conditionCode: "I21.9", maxResults: 5)
```

### Semantic Graph Retrieval Route Score: `semantic_graph_retrieval_route_score`

Scores a facility-case routing match using Semantic Graph Retrieval:

**Parameters:**

- `caseId`: The medical case ID
- `facilityId`: The facility ID to score

**Returns:**

- Route score (0-100) with component scores and rationale

**Usage:**

```
semantic_graph_retrieval_route_score(caseId: "abc123def456", facilityId: "8009377469709733890")
```

### Match Facilities for Case: `match_facilities_for_case`

Complete routing workflow that finds and scores facilities:

**Parameters:**

- `caseId`: The medical case ID
- `maxResults`: Maximum number of matches (default: 5)
- `minScore`: Minimum route score threshold (optional)
- `preferredFacilityTypes`: List of preferred facility types (optional)
- `requiredCapabilities`: List of required capabilities (optional)
- `maxDistanceKm`: Maximum distance in kilometers (optional)

**Returns:**

- List of facility matches sorted by route score (best match first)

**Usage:**

```
match_facilities_for_case(
  caseId: "abc123def456",
  maxResults: 3,
  minScore: 70.0,
  preferredFacilityTypes: ["ACADEMIC", "SPECIALTY_CENTER"],
  requiredCapabilities: ["ICU", "CARDIOLOGY"],
  maxDistanceKm: 50.0
)
```

## Scoring Components

Facility-case routing matches are scored using four components:

1. **Complexity Match** (30% weight) - Match between case complexity and facility capabilities
2. **Historical Outcomes** (30% weight) - Past outcomes for similar cases at facility
3. **Capacity** (20% weight) - Facility capacity and current occupancy
4. **Geographic Proximity** (20% weight) - Distance and accessibility

## Workflow

1. **Analyze case** to determine routing requirements
2. **Query candidates** using `graph_query_candidate_centers` or `match_facilities_for_case`
3. **Score facilities** using `semantic_graph_retrieval_route_score` for each candidate
4. **Rank facilities** by route score
5. **Filter results** if needed (by score, type, capabilities, distance)

## Output Format

When routing cases, provide:

- **Facility Information**: Name, type, location, capabilities
- **Route Score**: Overall score (0-100) with component breakdown
- **Rank**: Position in routing results (1 = best match)
- **Rationale**: Explanation of why this facility was selected
- **Geographic Info**: Distance and accessibility details

## Medical Disclaimer

**IMPORTANT**: Facility routing is for research and educational purposes only. All routing decisions should be reviewed
by qualified medical professionals and consider patient preferences, insurance coverage, and other factors beyond
technical scoring.
