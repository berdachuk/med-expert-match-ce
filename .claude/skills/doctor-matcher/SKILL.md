---
name: doctor-matcher
description: Match doctors to medical cases using multiple signals including vector similarity, graph relationships, and
historical performance
---

# Doctor Matcher

This skill guides you on how to match doctors to medical cases using sophisticated scoring that combines multiple
signals.

## Overview

The Doctor Matcher skill helps you:

1. **Find candidate doctors** - Query doctors based on specialty, availability, and other criteria
2. **Score doctor matches** - Calculate match scores using vector similarity, graph relationships, and historical
   performance
3. **Rank matches** - Sort doctors by match quality and relevance

## When to Use This Skill

Use this skill when:

- User needs to find specialists for a medical case
- You need to match doctors based on case requirements
- You need to score and rank doctor-case matches
- You need to find doctors with specific specialties or capabilities
- You need to prioritize doctors based on historical performance

## Available Tools

### Primary Tool: `query_candidate_doctors`

Finds candidate doctors based on case requirements and filters:

**Parameters:**

- `caseId`: The medical case ID
- `specialty`: Required medical specialty (optional)
- `requireTelehealth`: Require telehealth capability (optional)
- `maxResults`: Maximum number of results (default: 10)

**Returns:**

- List of candidate doctors matching the criteria

**Usage:**

```
query_candidate_doctors(caseId: "abc123def456", specialty: "Cardiology", requireTelehealth: true, maxResults: 5)
```

### Score Doctor Match: `score_doctor_match`

Scores a doctor-case match using multiple signals:

**Parameters:**

- `caseId`: The medical case ID
- `doctorId`: The doctor ID to score

**Returns:**

- Match score (0-100) with component scores and rationale

**Usage:**

```
score_doctor_match(caseId: "abc123def456", doctorId: "8760000000000420950")
```

### Match Doctors to Case: `match_doctors_to_case`

Complete matching workflow that finds and scores doctors:

**Parameters:**

- `caseId`: The medical case ID
- `maxResults`: Maximum number of matches (default: 10)
- `minScore`: Minimum match score threshold (optional)
- `preferredSpecialties`: List of preferred specialties (optional)
- `requireTelehealth`: Require telehealth capability (optional)

**Returns:**

- List of doctor matches sorted by score (best match first)

**Usage:**

```
match_doctors_to_case(
  caseId: "abc123def456",
  maxResults: 5,
  minScore: 70.0,
  preferredSpecialties: ["Cardiology"],
  requireTelehealth: true
)
```

## Scoring Components

Doctor-case matches are scored using three components:

1. **Vector Similarity** (40% weight) - Semantic similarity between case and doctor's experience
2. **Graph Relationships** (30% weight) - Graph-based relationships (doctor-case connections)
3. **Historical Performance** (30% weight) - Past outcomes, ratings, success rates

## Workflow

1. **Analyze case** to determine requirements (specialty, urgency, complexity)
2. **Query candidates** using `query_candidate_doctors` or `match_doctors_to_case`
3. **Review matches** sorted by score with rationale
4. **Filter results** if needed (by score threshold, specialty, telehealth)

## Output Format

When matching doctors, provide:

- **Doctor Information**: Name, specialty, certifications, availability
- **Match Score**: Overall score (0-100) with component breakdown
- **Rank**: Position in match results (1 = best match)
- **Rationale**: Explanation of why this doctor was matched

## Medical Disclaimer

**IMPORTANT**: Doctor matching is for research and educational purposes only. All matches should be reviewed by
qualified medical professionals before making clinical decisions.
