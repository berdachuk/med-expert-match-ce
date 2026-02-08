---
name: network-analyzer
description: Network expertise analytics, graph-based expert discovery, and aggregate metrics for medical expertise
networks
---

# Network Analyzer

This skill guides you on how to analyze medical expertise networks, discover experts through graph relationships, and
aggregate performance metrics.

## Overview

The Network Analyzer skill helps you:

1. **Query top experts** - Find top-performing experts for specific conditions
2. **Aggregate metrics** - Calculate aggregate performance metrics
3. **Graph-based discovery** - Discover experts through network relationships
4. **Network analytics** - Analyze expertise networks and collaboration patterns

## When to Use This Skill

Use this skill when:

- User needs to find top experts for a condition
- You need to analyze expertise networks
- You need aggregate performance metrics
- You need to discover experts through relationships
- You need network analytics and insights

## Available Tools

### Graph Query Top Experts: `graph_query_top_experts`

Queries the graph to find top experts for a condition:

**Parameters:**

- `conditionCode`: ICD-10 code for the condition
- `period`: Time period for analysis (optional)
- `maxResults`: Maximum number of experts (default: 10)

**Returns:**

- List of top experts with metrics and scores

**Usage:**

```
graph_query_top_experts(conditionCode: "I21.9", maxResults: 5)
```

### Aggregate Metrics: `aggregate_metrics`

Aggregates performance metrics for experts or conditions:

**Parameters:**

- `entityType`: Type of entity (DOCTOR, CONDITION, FACILITY)
- `entityId`: Entity ID (optional, for specific entity)
- `metricType`: Type of metrics (PERFORMANCE, OUTCOMES, VOLUME)

**Returns:**

- Aggregate metrics with statistics

**Usage:**

```
aggregate_metrics(entityType: "DOCTOR", metricType: "PERFORMANCE")
```

### Network Analysis: `network_analysis`

Analyzes expertise network relationships:

**Parameters:**

- `conditionCode`: ICD-10 code for condition (optional)
- `analysisType`: Type of analysis (COLLABORATION, EXPERTISE_CLUSTERS, REFERRAL_PATTERNS)

**Returns:**

- Network analysis results with insights

**Usage:**

```
network_analysis(conditionCode: "I21.9", analysisType: "COLLABORATION")
```

## Workflow

1. **Identify analysis need** - What network insights are needed?
2. **Query graph** for top experts or relationships
3. **Aggregate metrics** for performance analysis
4. **Analyze network** patterns and relationships
5. **Generate insights** from network data

## Output Format

When analyzing networks, provide:

- **Top Experts**: List of experts with rankings and metrics
- **Aggregate Metrics**: Statistics and performance indicators
- **Network Patterns**: Discovered patterns and relationships
- **Insights**: Key findings from network analysis

## Medical Disclaimer

**IMPORTANT**: Network analytics are for research and educational purposes only. Expert rankings and metrics are based
on available data and should not be the sole basis for clinical decisions. This system is not certified for clinical
use.
