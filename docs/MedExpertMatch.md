# MedExpertMatch: AI-Powered Medical Expert Recommendation System

**Project Name:** MedExpertMatch  
**Version:** 1.2  
**Date:** 2026-05-19  
**Status:** MVP complete with agentic improvements and DocuRAG improvements

## Overview

**MedExpertMatch** is an AI-powered medical expert recommendation system developed for the MedGemma Impact Challenge.
The system matches medical cases with appropriate doctors based on case analysis, doctor expertise, clinical guidelines,
and similar case outcomes.

## Project Goals

**Match Medical Cases to Doctors**: Automatically match medical cases with appropriate specialists based on:

- Case analysis (symptoms, diagnoses, ICD-10 codes)
- Doctor expertise and clinical experience
- Clinical guidelines and evidence
- Similar case outcomes

**Use MedGemma Models**: Leverage MedGemma 1.5 4B and other HAI-DEF models for:

- Medical case analysis
- Entity extraction (ICD-10 codes, specialties, symptoms)
- Clinical recommendations
- Evidence retrieval

**Privacy-First Architecture**:

- Local deployment capability
- HIPAA-compliant data handling
- Anonymized patient data

**Human-Centered Design**:

- Focus on real clinical workflows
- User-friendly for doctors and medical staff
- Evidence-based recommendations

## Architecture

MedExpertMatch uses a modern, scalable architecture:

- **Hybrid GraphRAG**: Vector search + Graph traversal + Keyword search
- **Spring AI Integration**: MedGemma models via Spring AI
- **Agent Skills**: 7 medical-specific skills for modular knowledge
- **Session Memory**: Conversation history compaction via SessionMemoryAdvisor (JDBC-backed)
- **AutoMemory**: Cross-session durable memory (LLM self-curated facts)
- **PostgreSQL + PgVector + Apache AGE**: Unified database architecture

## Key Features

### 1. Case Analysis

- Extract ICD-10 codes from case descriptions
- Classify case urgency (Critical, Urgent, Routine)
- Determine required medical specialty
- Analyze case complexity

### 2. Doctor Matching

- Match doctors to cases based on:
    - Specialty alignment
    - Clinical experience with similar cases
    - Case outcomes and success rates
    - Board certifications

### 3. Evidence Retrieval

- Search clinical guidelines
- Query PubMed for evidence
- GRADE evidence level assessment
- Clinical decision support

### 4. Agent Skills

Seven medical-specific Agent Skills:

- **case-analyzer**: Analyze cases, extract entities, ICD-10 codes, classify urgency and complexity
- **doctor-matcher**: Match doctors to cases, scoring and ranking using multiple signals
- **evidence-retriever**: Search guidelines, PubMed, GRADE evidence summaries
- **recommendation-engine**: Generate clinical recommendations, diagnostic workup, treatment options
- **clinical-advisor**: Differential diagnosis, risk assessment
- **network-analyzer**: Network expertise analytics, graph-based expert discovery, aggregate metrics
- **routing-planner**: Facility routing optimization, multi-facility scoring, geographic routing

## Technology Stack

- **Backend**: Spring Boot 4.0.6, Java 21
- **Database**: PostgreSQL 17, PgVector 0.1.6 (client), Apache AGE 1.6.0
- **AI Framework**: Spring AI 2.0.0-M8
- **Session**: Spring AI Session JDBC 0.3.0
- **Agentic Patterns**: Spring AI Agent Utils 0.8.0
- **Testing**: JUnit 5, Testcontainers 2.0.5

## Development Timeline

- **Week 1-2**: Foundation (domain models, database schema)
- **Week 3**: Core services (MedGemma integration, case analysis)
- **Week 4**: Agent Skills implementation
- **Week 5-6**: Integration, testing, documentation, demo

## Important Disclaimers

**Warning: MedGemma is not a medical device**

- Models are not certified for clinical use
- Additional validation required for real-world deployment
- Not intended for diagnostic decisions without human-in-the-loop
- All applications are for research and educational purposes

**HIPAA compliance**

- All patient data must be anonymized
- Local deployment option for privacy
- No transmission of PHI without proper safeguards

## Use Cases

MedExpertMatch supports six core use cases:

1. **Specialist Matching for Complex Inpatient Cases** - Quick specialist finding for attending physicians
2. **Online Second Opinion / Telehealth** - Second opinion matching for patients and referring physicians
3. **Prioritizing the Consultation Queue** - Urgency-based queue prioritization for coordinators
4. **Network Analytics** - Data-driven expertise mapping for medical directors
5. **Human-in-the-Loop Decision Support** - AI copilot with expert matching for specialists
6. **Cross-Organization / Regional Routing** - Optimal facility routing for regional operators

See [Use Cases](USE_CASES.md) for detailed workflows and technical implementation.

## Related Documentation

- [Use Cases](USE_CASES.md) - Detailed use case workflows
- [Architecture](ARCHITECTURE.md) - System architecture and design

---

*Last updated: 2026-05-19*
