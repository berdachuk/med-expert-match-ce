---
name: evidence-retriever
description: Search clinical guidelines, PubMed, and GRADE evidence summaries for evidence-based medicine
---

# Evidence Retriever

This skill guides you on how to retrieve clinical evidence from guidelines, PubMed, and GRADE summaries to support
evidence-based medical decisions.

## Overview

The Evidence Retriever skill helps you:

1. **Search clinical guidelines** - Find relevant clinical practice guidelines
2. **Query PubMed** - Search medical literature for evidence
3. **Retrieve GRADE summaries** - Access evidence quality assessments

## When to Use This Skill

Use this skill when:

- User needs evidence-based recommendations
- You need to find clinical guidelines for a condition
- You need to search medical literature
- You need evidence quality assessments
- You need to support clinical recommendations with citations

## Available Tools

### Search Clinical Guidelines: `search_clinical_guidelines`

Searches for clinical practice guidelines:

**Parameters:**

- `condition`: Medical condition or diagnosis
- `specialty`: Medical specialty (optional)
- `maxResults`: Maximum number of results (default: 10)

**Returns:**

- List of relevant clinical guidelines with citations

**Usage:**

```
search_clinical_guidelines(condition: "Acute myocardial infarction", specialty: "Cardiology", maxResults: 5)
```

### Query PubMed: `query_pubmed`

Searches PubMed medical literature database:

**Parameters:**

- `query`: Search query string
- `maxResults`: Maximum number of results (default: 10)
- `publicationDateFrom`: Start date for publication filter (optional)
- `publicationDateTo`: End date for publication filter (optional)

**Returns:**

- List of PubMed articles with titles, abstracts, and citations

**Usage:**

```
query_pubmed(query: "acute myocardial infarction treatment guidelines", maxResults: 10)
```

### Search GRADE Evidence: `search_grade_evidence`

Searches for GRADE evidence quality assessments:

**Parameters:**

- `condition`: Medical condition
- `intervention`: Treatment or intervention (optional)
- `maxResults`: Maximum number of results (default: 10)

**Returns:**

- List of GRADE evidence summaries with quality ratings

**Usage:**

```
search_grade_evidence(condition: "Heart failure", intervention: "ACE inhibitors")
```

## Workflow

1. **Identify information need** - What evidence is required?
2. **Search guidelines** for clinical practice recommendations
3. **Query PubMed** for recent research and literature
4. **Retrieve GRADE summaries** for evidence quality assessment
5. **Synthesize evidence** to support recommendations

## Output Format

When retrieving evidence, provide:

- **Source Type**: Guideline, PubMed article, or GRADE summary
- **Title**: Document or article title
- **Citation**: Full citation information
- **Relevance**: How relevant is this evidence to the query?
- **Key Findings**: Main findings or recommendations
- **Evidence Quality**: GRADE rating if available

## Medical Disclaimer

**IMPORTANT**: Evidence retrieval is for research and educational purposes only. All evidence should be reviewed by
qualified medical professionals before clinical application. This system is not certified for clinical use.
