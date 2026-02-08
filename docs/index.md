# MedExpertMatch Documentation

Welcome to the MedExpertMatch documentation!

## About MedExpertMatch

**MedExpertMatch** is an AI-powered medical expert recommendation system developed for the **MedGemma Impact Challenge
**. The system matches medical cases with appropriate doctors based on:

- Medical case analysis (symptoms, diagnoses, ICD-10 codes)
- Doctor expertise and clinical experience
- Clinical guidelines and evidence
- Similar case outcomes
- Specialty matching

MedExpertMatch leverages a **Hybrid GraphRAG architecture**, combining:

- **Vector Similarity Search** (PgVector) - Semantic matching based on clinical experiences (40% weight in scoring)
- **Graph Traversal** (Apache AGE) - Relationship-based discovery of doctor-case connections (30% weight in scoring)
- **Historical Performance** - Past outcomes, ratings, and success rates (30% weight in scoring)
- **Keyword Search** - Traditional text matching for medical terms
- **Semantic Reranking** - Precision optimization using MedGemma
- **LLM Orchestration** - Natural language answer generation with MedGemma models

**Note**: The Find Specialist flow actively uses Apache AGE graph for relationship scoring.
See [Find Specialist Flow](FIND_SPECIALIST_FLOW.md#apache-age-graph-usage) for details.

## MedGemma Impact Challenge

**MedExpertMatch** is being developed for the MedGemma Impact Challenge, a hackathon organized by Google Research on
Kaggle.

### Challenge Requirements

- **Use HAI-DEF Models**: MedGemma 1.5 4B, MedGemma 27B, MedASR, CXR Foundation, Derm Foundation, Path Foundation, HeAR
- **Human-Centered Design**: Focus on real clinical workflows and user experience
- **Privacy-First Architecture**: Local deployment capability, HIPAA compliance
- **Working Solution**: Full-featured prototype or MVP, not just a demo

### Submission Deadline

**February 24, 2026** - ~6 weeks development timeline

## Quick Links

### Getting Started

- [Vision](VISION.md) - Project vision and long-term goals
- [Product Requirements Document](PRD.md) - Complete product specification
- [MedExpertMatch Overview](MedExpertMatch.md) - Complete project overview
- [Use Cases](USE_CASES.md) - Core use cases and workflows
- [Problems Solved](PROBLEMS_SOLVED.md) - List of healthcare problems the application addresses
- [Target Audience](TARGET_AUDIENCE.md) - Primary users and roles (physicians, coordinators, CMO, regional operators)
- [Benefits](BENEFITS.md) - Advantages for patients, clinicians, and organizations
- [Unique Selling Propositions](UNIQUE_SELLING_PROPOSITIONS.md) - What differentiates MedExpertMatch from alternatives
- [Sales Copy: Pain – More Pain – Solution](SALES_COPY_PAIN_SOLUTION.md) - Copywriting blocks for landing pages and
  pitches
- [Sales Presentation (2.5-3 min)](PRESENTATION_SALES_3MIN.md) - Slide content and speaker script for a short pitch

### Architecture

- [Architecture Overview](ARCHITECTURE.md) - System architecture and design

### Development

- [Implementation Plan](IMPLEMENTATION_PLAN.md) - Detailed phase-by-phase implementation guide
- [Development Guide](DEVELOPMENT_GUIDE.md) - Setup and development workflow
- [Coding Rules](CODING_RULES.md) - Development guidelines and conventions
- [Testing Guide](TESTING.md) - Testing patterns and best practices
- [List Formatting Guide](LIST_FORMATTING_GUIDE.md) - Markdown list formatting examples and fixes
- [UI Flows and Mockups](UI_FLOWS_AND_MOCKUPS.md) - User interface flows and form mockups in PlantUML
- [Repository Methods Update](REPOSITORY_METHODS_UPDATE.md) - Recent repository method additions (findAll)
- [Synthetic Data Generator](SYNTHETIC_DATA_GENERATOR.md) - Comprehensive feature description
- [Demo Guide](DEMO_GUIDE.md) - How to prepare and run the MedExpertMatch demo

### Configuration

- [AI Provider Configuration](AI_PROVIDER_CONFIGURATION.md) - AI provider setup and configuration
- [MedGemma Configuration](MEDGEMMA_CONFIGURATION.md) - MedGemma model configuration
- [MedGemma Setup Guide](MEDGEMMA_SETUP.md) - Step-by-step guide for local MedGemma via OpenAI-compatible APIs (e.g.
  LiteLLM)

### Features

- [Find Specialist Flow](FIND_SPECIALIST_FLOW.md) - Detailed flow documentation for specialist matching
- [Consultation Queue](CONSULTATION_QUEUE.md) - Urgency-based queue prioritization for coordinators (Use Case 3)
- [Evidence Retrieval](EVIDENCE_RETRIEVAL.md) - Clinical guidelines and PubMed integration
- [Medical Agent Tools](MEDICAL_AGENT_TOOLS.md) - Complete documentation of all implemented LLM tools

## Key Features

- **Case Analysis**: Analyze medical cases using MedGemma to extract ICD-10 codes, urgency, and required specialty
- **Doctor Matching**: Match doctors to cases based on specialty, experience, and similar case outcomes
- **Consultation Queue**: Prioritize consult requests by clinical urgency so specialists see the sickest patients
  first ([Consultation Queue](CONSULTATION_QUEUE.md))
- **Evidence Retrieval**: Search clinical guidelines and PubMed for evidence-based recommendations (all tools
  implemented)
- **Clinical Recommendations**: Generate evidence-based clinical recommendations using MedGemma (all tools implemented)
- **Network Analytics**: Query graph for top experts and aggregate metrics (all tools implemented)
- **Regional Routing**: Score facility-case routing matches using Semantic Graph Retrieval (all tools implemented)
- **Agent Skills**: 7 medical-specific Agent Skills for modular knowledge
  management ([Medical Agent Tools](MEDICAL_AGENT_TOOLS.md))
- **Hybrid GraphRAG**: Combines vector, graph, and keyword search for optimal matching
- **Privacy-First**: Local deployment capability, HIPAA-compliant data handling
- **Simulated security**: User selector (Regular User / Administrator); Synthetic Data and Graph Visualization are
  admin-only. See [Architecture - Simulated Security](ARCHITECTURE.md#role-based-simulated-security).

## Core Use Cases

MedExpertMatch addresses six primary use cases:

1. **Specialist Matching for Complex Inpatient Cases** - Reduce consultation delays and length of stay
2. **Online Second Opinion / Telehealth** - Fast, accurate second opinion matching
3. **Prioritizing the Consultation Queue** - Ensure urgent cases are seen first
4. **Network Analytics** - Data-driven expertise mapping and routing policies
5. **Human-in-the-Loop Decision Support** - AI copilot with expert recommendations for specialists
6. **Cross-Organization / Regional Routing** - Optimal facility and specialist routing across networks

See [Use Cases](USE_CASES.md) for detailed workflows, API endpoints, and technical implementation.

## Architecture Overview

MedExpertMatch uses a modern, scalable architecture:

- **Backend**: Spring Boot 4.0.2, Java 21
- **Database**: PostgreSQL 17 with PgVector and Apache AGE 1.6.0
- **AI/ML**: Spring AI 2.0.0-M2 with MedGemma models
- **Vector Search**: PgVector with HNSW indexing
- **Graph Database**: Apache AGE for relationship traversal
- **Agent Skills**: Spring AI Agent Skills for medical domain knowledge

## Architecture Components

MedExpertMatch includes:

- **Infrastructure**: Database, vector search, graph, LLM integration
- **Services**: Retrieval, fusion, reranking with medical-specific adaptations
- **Domain Models**: Doctor, MedicalCase, ClinicalExperience, ICD10Code
- **Medical Components**: Medical domain modules, case analysis, evidence retrieval, consultation queue

## Project Status

**Current Phase**: MVP complete, feature refinement

- Challenge analysis completed
- Architecture design complete
- Core implementation complete (case analysis, matching, queue, evidence, network, routing)
- Consultation queue prioritization uses real cases from DB and deterministic urgency ordering

## Documentation Structure

This documentation is organized into several sections:

1. **Overview** - Project overview and challenge details
2. **Architecture** - System architecture and design
3. **Development** - Setup and development guides
4. **Configuration** - Configuration and optimization guides

## Important Disclaimers

⚠️ **MedGemma is NOT a Medical Device**:

- Models are not certified for clinical use
- Additional validation required for real-world deployment
- Not intended for diagnostic decisions without human-in-the-loop
- All applications are for research and educational purposes

⚠️ **HIPAA Compliance**:

- All patient data must be anonymized
- Local deployment option for privacy
- No transmission of PHI without proper safeguards

## Contributing

For development guidelines, see:

- [Development Guide](DEVELOPMENT_GUIDE.md)
- [Coding Rules](CODING_RULES.md)
- [Testing Guide](TESTING.md)

## Support

For questions or issues related to MedExpertMatch or the MedGemma Impact Challenge, please refer to the relevant
documentation section.

---

*Last updated: 2026-02-03*
