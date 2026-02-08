---
name: clinical-advisor
description: Provide differential diagnosis, risk assessment, and clinical advisory services
---

# Clinical Advisor

This skill guides you on how to provide differential diagnosis, risk assessment, and clinical advisory services.

## Overview

The Clinical Advisor skill helps you:

1. **Differential diagnosis** - Generate differential diagnosis lists based on symptoms and findings
2. **Risk assessment** - Assess patient risk factors and complications
3. **Clinical advisory** - Provide clinical guidance and decision support

## When to Use This Skill

Use this skill when:

- User needs differential diagnosis for symptoms
- You need to assess patient risk factors
- You need clinical decision support
- You need to evaluate diagnostic possibilities
- You need risk stratification

## Available Tools

### Differential Diagnosis: `differential_diagnosis`

Generates differential diagnosis list based on symptoms and findings:

**Parameters:**

- `caseId`: The medical case ID
- `symptoms`: List of symptoms (optional, if not in case)
- `findings`: Clinical findings (optional)
- `maxResults`: Maximum number of diagnoses (default: 10)

**Returns:**

- List of differential diagnoses with likelihood and rationale

**Usage:**

```
differential_diagnosis(caseId: "abc123def456", maxResults: 5)
```

### Risk Assessment: `risk_assessment`

Assesses patient risk factors and complications:

**Parameters:**

- `caseId`: The medical case ID
- `riskType`: Type of risk assessment (COMPLICATION, MORTALITY, READMISSION, etc.)

**Returns:**

- Risk assessment with factors, scores, and recommendations

**Usage:**

```
risk_assessment(caseId: "abc123def456", riskType: "COMPLICATION")
```

### Clinical Decision Support: `clinical_decision_support`

Provides clinical decision support for a case:

**Parameters:**

- `caseId`: The medical case ID
- `decisionType`: Type of decision support needed (DIAGNOSTIC, TREATMENT, MANAGEMENT)

**Returns:**

- Clinical decision support with recommendations and rationale

**Usage:**

```
clinical_decision_support(caseId: "abc123def456", decisionType: "TREATMENT")
```

## Workflow

1. **Analyze case** to understand clinical presentation
2. **Generate differential diagnosis** based on symptoms and findings
3. **Assess risks** for complications or adverse outcomes
4. **Provide decision support** with recommendations
5. **Prioritize** by likelihood and urgency

## Output Format

When providing clinical advice, include:

- **Differential Diagnoses**: List of possible diagnoses with likelihood
- **Risk Factors**: Identified risk factors and their significance
- **Risk Scores**: Calculated risk scores if applicable
- **Recommendations**: Clinical recommendations based on assessment
- **Rationale**: Explanation of reasoning

## Medical Disclaimer

**IMPORTANT**: All clinical advice is for research and educational purposes only. This system is not certified for
clinical use and should not be used for diagnostic or treatment decisions without human-in-the-loop validation by
qualified medical professionals. Differential diagnoses and risk assessments are suggestions only and must be validated
by clinical judgment.
