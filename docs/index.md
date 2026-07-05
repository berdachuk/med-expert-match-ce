# MedExpertMatch Documentation

Welcome to the MedExpertMatch documentation.

## Language

All documentation in this site and in the repository `docs/` folder is written in **English**. User-facing UI strings in
the application may be localized separately; technical specs, guides, and README files stay in English.

## About MedExpertMatch

**MedExpertMatch** is an AI-powered medical expert recommendation system developed for the **MedGemma Impact Challenge
**. The system matches medical cases with appropriate doctors based on case analysis, doctor expertise, clinical
guidelines, and similar case outcomes.

For a comprehensive overview including architecture, key features, and use cases, see the
[MedExpertMatch Overview](MedExpertMatch.md).

## MedGemma Impact Challenge

**MedExpertMatch** was developed for the MedGemma Impact Challenge, a hackathon organized by Google Research on Kaggle.
See the [Product Requirements Document](pipeline/01-requirements.md) for full challenge alignment details.

**Challenge submission completed February 24, 2026** (6-week development timeline).

## Quick Links

### Getting Started

- [Vision](VISION.md) - Project vision and long-term goals
- [Product Requirements Document](pipeline/01-requirements.md) - Complete product specification
- [MedExpertMatch Overview](MedExpertMatch.md) - Complete project overview
- [Use Cases](use-cases.md) - Core use cases and workflows
- [Problems Solved](PROBLEMS_SOLVED.md) - List of healthcare problems the application addresses
- [Target Audience](TARGET_AUDIENCE.md) - Primary users and roles (physicians, coordinators, CMO, regional operators)
- [Benefits](BENEFITS.md) - Advantages for patients, clinicians, and organizations
- [Unique Selling Propositions](UNIQUE_SELLING_PROPOSITIONS.md) - What differentiates MedExpertMatch from alternatives
- [Sales Copy: Pain – More Pain – Solution](SALES_COPY_PAIN_SOLUTION.md) - Copywriting blocks for landing pages and
  pitches
- [Sales Presentation (2.5-3 min)](PRESENTATION_SALES_3MIN.md) - Slide content and speaker script for a short pitch

### Architecture

- [Architecture Overview](pipeline/02-architecture.md) - System architecture and design
- [M64 Cost-Quality Tier Routing (ADR)](decisions/M64-cost-quality-tier-routing.md) - Multi-tier LLM routing decision and full design
- [LLM Cost Model by Tier](eval/cost-model.md) - Tier → model → token budget reference (M64 / M68)

### Development

- [Fix Plan](FIX_PLAN.md) - Active tracked checklist for cleanup, alignment, and hardening work
- [Implementation Plan](IMPLEMENTATION_PLAN.md) - Detailed phase-by-phase implementation guide
- [Development Guide](DEVELOPMENT_GUIDE.md) - Setup and development workflow
- [Coding Rules](CODING_RULES.md) - Development guidelines and conventions
- [Testing Guide](pipeline/04-testing.md) - Testing patterns and best practices
- [List Formatting Guide](LIST_FORMATTING_GUIDE.md) - Markdown list formatting examples and fixes
- [UI Flows and Mockups](UI_FLOWS_AND_MOCKUPS.md) - User interface flows and form mockups in PlantUML
- [Repository Methods Update](REPOSITORY_METHODS_UPDATE.md) - Recent repository method additions (findAll)
- [Demo Guide](DEMO_GUIDE.md) - How to prepare and run the MedExpertMatch demo

### Configuration

- [AI Provider Configuration](AI_PROVIDER_CONFIGURATION.md) - AI provider setup and configuration
- [Model Selection Guide](MODEL_SELECTION_GUIDE.md) - Which models to use per role (local vs Ollama Cloud)
- [MedGemma Configuration](MEDGEMMA_CONFIGURATION.md) - MedGemma model configuration
- [MedGemma Setup Guide](MEDGEMMA_SETUP.md) - Step-by-step guide for local MedGemma via OpenAI-compatible APIs (e.g.
  LiteLLM)

### Features

- [Find Specialist Flow](FIND_SPECIALIST_FLOW.md) - Detailed flow documentation for specialist matching
- [Consultation Queue](CONSULTATION_QUEUE.md) - Urgency-based queue prioritization for coordinators (Use Case 3)
- [Evidence Retrieval](EVIDENCE_RETRIEVAL.md) - Clinical guidelines and PubMed integration
- [Medical Agent Tools](MEDICAL_AGENT_TOOLS.md) - Complete documentation of all implemented LLM tools

### AI & LLM

- [Harness Architecture](HARNESS.md) - Goal routing, workflow engines, configuration, and observability
- [M64 Cost-Quality Tier Routing (ADR)](decisions/M64-cost-quality-tier-routing.md) - Multi-tier LLM cost routing architecture decision
- [Harness presentation](presentations/medexpertmatch-harness.md) - Reveal.js deck: high-level steps and harness metaphor
- [Model Selection Guide](MODEL_SELECTION_GUIDE.md) - Recommended models by role (local-only vs Ollama Cloud hybrid)
- [FunctionGemma Tool Calling](FUNCTIONGEMMA.md) - Tool-calling model setup, tool pairs, and fine-tuning
- [Harness & Agent Patterns](HARNESS_AND_AGENT_USAGE.md) - Model vs harness terminology and repository conventions

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

**Warning: MedGemma is not a medical device**

- Models are not certified for clinical use
- Additional validation required for real-world deployment
- Not intended for diagnostic decisions without human-in-the-loop
- All applications are for research and educational purposes

**HIPAA compliance**

- All patient data must be anonymized
- Local deployment option for privacy
- No transmission of PHI without proper safeguards

## Contributing

For development guidelines, see:

- [Development Guide](DEVELOPMENT_GUIDE.md)
- [Coding Rules](CODING_RULES.md)
- [Testing Guide](pipeline/04-testing.md)

## Support

For questions or issues related to MedExpertMatch or the MedGemma Impact Challenge, please refer to the relevant
documentation section.

---

*Last updated: 2026-06-03*
