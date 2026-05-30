---
name: clinical-guideline
description: Ground clinical recommendations in published clinical guideline references and grade their strength
---

# Clinical Guideline

This skill guides you on how to ground clinical recommendations in published clinical practice
guidelines, cite the governing source, and communicate the strength of each recommendation.

## Overview

The Clinical Guideline skill helps you:

1. **Identify governing guidelines** - Determine which clinical practice guideline(s) apply to a case
2. **Ground recommendations** - Tie each recommendation back to a specific guideline statement
3. **Grade strength** - Communicate recommendation strength and evidence quality (e.g. GRADE)
4. **Surface gaps** - Flag where no guideline coverage exists and human judgment is required

## When to Use This Skill

Use this skill when:

- A recommendation must be justified against an authoritative clinical guideline
- You need to cite the source guideline for a diagnostic or treatment suggestion
- You need to communicate how strong or conditional a recommendation is
- You need to check whether a proposed action aligns with standard of care
- A case falls outside published guidance and the limitation must be stated

## Available Tools

This skill is primarily advisory and composes with the agent's evidence and case tools. Use the
`evidence-retriever` skill's tools to fetch supporting literature, and read reference files via the
file-read tool when guideline excerpts are provided as attachments.

## Workflow

1. **Understand the case** - Identify the clinical question (diagnosis, workup, or treatment)
2. **Select guidelines** - Identify the most relevant, current clinical practice guideline(s)
3. **Map recommendations** - Align each suggestion to a specific guideline statement
4. **Grade strength** - Note recommendation strength (strong/conditional) and evidence quality
5. **Cite sources** - Provide the guideline name, issuing body, and year for every claim
6. **Flag gaps** - State explicitly where guidance is absent or conflicting

## Output Format

When grounding recommendations, include:

- **Clinical Question**: The decision the recommendation addresses
- **Governing Guideline**: Name, issuing body, and year of the source guideline
- **Recommendation**: The grounded recommendation text
- **Strength of Recommendation**: Strong or conditional
- **Quality of Evidence**: High, moderate, low, or very low (e.g. GRADE)
- **Citation**: Specific guideline reference supporting the recommendation
- **Gaps / Caveats**: Areas not covered by guidelines or requiring clinical judgment

## Medical Disclaimer

**IMPORTANT**: All guideline-grounded recommendations are for research and educational purposes only.
This system is not certified for clinical use and should not be used for diagnostic or treatment
decisions without human-in-the-loop validation by qualified medical professionals. Guideline
citations are advisory references, not a substitute for reviewing the primary source and applying
clinical judgment to the individual patient.
