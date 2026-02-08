---
name: case-analyzer
description: Analyze medical cases, extract entities, ICD-10 codes, classify urgency and complexity
---

# Case Analyzer

This skill guides you on how to analyze medical cases, extract clinical entities, ICD-10 codes, and classify urgency and
complexity levels.

## Overview

The Case Analyzer skill helps you:

1. **Analyze case text** - Extract key clinical information from unstructured case descriptions
2. **Extract ICD-10 codes** - Identify diagnosis codes from case text
3. **Classify urgency** - Determine urgency level (CRITICAL, HIGH, MEDIUM, LOW)
4. **Determine required specialty** - Identify which medical specialty is needed

## When to Use This Skill

Use this skill when:

- User provides unstructured case text that needs analysis
- You need to extract structured information from case descriptions
- You need to classify case urgency or complexity
- You need to determine which medical specialty should handle the case
- You need to extract ICD-10 codes from case text

## Available Tools

### Primary Tool: `analyze_case_text`

Analyzes unstructured case text and extracts key clinical information:

**Parameters:**

- `caseText`: The unstructured case description text

**Returns:**

- Structured case analysis with chief complaint, symptoms, diagnosis, urgency level, required specialty

**Usage:**

```
analyze_case_text(caseText: "45-year-old patient presents with severe chest pain radiating to left arm, diaphoresis, and nausea. ECG shows ST elevation in leads II, III, aVF.")
```

### Extract ICD-10 Codes: `extract_icd10_codes`

Extracts ICD-10 diagnosis codes from case text:

**Parameters:**

- `caseText`: The case description text

**Returns:**

- List of ICD-10 codes (e.g., ["I21.9", "E11.9"])

**Usage:**

```
extract_icd10_codes(caseText: "Patient with acute myocardial infarction and type 2 diabetes")
```

### Classify Urgency: `classify_urgency`

Classifies case urgency level based on clinical presentation:

**Parameters:**

- `caseText`: The case description text

**Returns:**

- Urgency level: CRITICAL, HIGH, MEDIUM, or LOW

**Usage:**

```
classify_urgency(caseText: "Patient with acute MI, hemodynamically unstable")
```

### Determine Specialty: `determine_required_specialty`

Determines which medical specialty should handle the case:

**Parameters:**

- `caseText`: The case description text

**Returns:**

- Required specialty name (e.g., "Cardiology", "Neurology", "Emergency Medicine")

**Usage:**

```
determine_required_specialty(caseText: "Patient with acute stroke symptoms")
```

## Workflow

1. **Receive case text** from user or system
2. **Analyze case** using `analyze_case_text` to extract structured information
3. **Extract ICD-10 codes** if needed for matching or coding
4. **Classify urgency** to prioritize case handling
5. **Determine specialty** to route to appropriate specialists

## Output Format

When analyzing cases, provide structured output:

- **Chief Complaint**: Primary reason for visit
- **Symptoms**: List of symptoms and clinical findings
- **Current Diagnosis**: Primary diagnosis if available
- **ICD-10 Codes**: Extracted diagnosis codes
- **Urgency Level**: CRITICAL, HIGH, MEDIUM, or LOW
- **Required Specialty**: Medical specialty needed
- **Case Type**: INPATIENT, SECOND_OPINION, or CONSULT_REQUEST

## Medical Disclaimer

**IMPORTANT**: All case analysis is for research and educational purposes only. This system is not certified for
clinical use and should not be used for diagnostic decisions without human-in-the-loop validation.
