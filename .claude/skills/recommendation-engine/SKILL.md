---
name: recommendation-engine
description: Generate clinical recommendations, diagnostic workup, and treatment options based on case analysis and
evidence
---

# Recommendation Engine

This skill guides you on how to generate clinical recommendations, diagnostic workup plans, and treatment options based
on case analysis and evidence.

## Overview

The Recommendation Engine skill helps you:

1. **Generate recommendations** - Create clinical recommendations based on case analysis
2. **Suggest diagnostic workup** - Propose diagnostic tests and procedures
3. **Recommend treatments** - Suggest treatment options with evidence support

## When to Use This Skill

Use this skill when:

- User needs clinical recommendations for a case
- You need to suggest diagnostic workup
- You need to propose treatment options
- You need evidence-based recommendations
- You need to support clinical decision-making

## Available Tools

### Generate Recommendations: `generate_recommendations`

Generates clinical recommendations for a medical case:

**Parameters:**

- `caseId`: The medical case ID
- `recommendationType`: Type of recommendation (DIAGNOSTIC, TREATMENT, FOLLOW_UP)
- `includeEvidence`: Include evidence citations (default: true)

**Returns:**

- List of recommendations with rationale and evidence

**Usage:**

```
generate_recommendations(caseId: "abc123def456", recommendationType: "TREATMENT", includeEvidence: true)
```

### Suggest Diagnostic Workup: `suggest_diagnostic_workup`

Suggests diagnostic tests and procedures:

**Parameters:**

- `caseId`: The medical case ID
- `urgencyLevel`: Urgency level for prioritization (optional)

**Returns:**

- List of recommended diagnostic tests with rationale

**Usage:**

```
suggest_diagnostic_workup(caseId: "abc123def456", urgencyLevel: "HIGH")
```

### Recommend Treatment Options: `recommend_treatment_options`

Recommends treatment options with evidence support:

**Parameters:**

- `caseId`: The medical case ID
- `includeAlternatives`: Include alternative treatment options (default: true)

**Returns:**

- List of treatment options with evidence and rationale

**Usage:**

```
recommend_treatment_options(caseId: "abc123def456", includeAlternatives: true)
```

## Workflow

1. **Analyze case** to understand clinical context
2. **Retrieve evidence** using Evidence Retriever skill
3. **Generate recommendations** based on case analysis and evidence
4. **Prioritize recommendations** by urgency and evidence quality
5. **Format output** with rationale and citations

## Output Format

When generating recommendations, provide:

- **Recommendation Type**: Diagnostic, Treatment, or Follow-up
- **Recommendation**: Specific recommendation text
- **Rationale**: Explanation of why this recommendation is made
- **Evidence**: Supporting evidence citations
- **Priority**: High, Medium, or Low priority
- **Urgency**: When this should be addressed

## Medical Disclaimer

**IMPORTANT**: All recommendations are for research and educational purposes only. This system is not certified for
clinical use and should not be used for diagnostic or treatment decisions without human-in-the-loop validation by
qualified medical professionals.
