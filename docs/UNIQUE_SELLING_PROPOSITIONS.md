# Unique Selling Propositions (USPs)

This document states the main unique selling propositions of MedExpertMatch: what distinguishes it from manual
processes, simple directories, or generic matching tools.

## 1. Match in Minutes, Not Days

**USP:** Specialist matching in minutes instead of days; target is to reduce consultation delays from days to hours.

**Why it is unique:** Combines vector similarity, graph relationships, and historical performance in a single scoring
pipeline (Hybrid GraphRAG). Replaces ad-hoc "who do I know?" with a consistent, automated process that returns ranked
specialists with rationales. No other single system in the scope combines these three signals with medical case analysis
in one flow.

---

## 2. "Who Is Good at What" Becomes Visible

**USP:** Real expertise is visible: which specialists and facilities actually handle complex cases in specific domains (
e.g. by ICD-10 code), not only job titles or self-declared specialties.

**Why it is unique:** Network analytics on a graph of doctors, cases, conditions, and facilities. Queries like "top
experts for I21.9 in the past 2 years" return data-driven rankings. Replaces implicit, anecdotal knowledge with
transparent, measurable expertise mapping for routing, planning, and mentorship.

---

## 3. One AI Copilot: Analysis, Evidence, Recommendations, and Experts

**USP:** A single flow delivers structured case analysis, differential diagnosis, evidence retrieval (guidelines,
PubMed), clinical recommendations, and matched colleagues to discuss with.

**Why it is unique:** One medical-domain copilot instead of separate tools. Case-analyzer, evidence-retriever,
recommendation-engine, clinical-advisor, and doctor-matcher work together in one conversation or API flow. Specialists
get analysis plus evidence plus recommendations plus expert matching without switching systems.

---

## 4. Three-Signal Scoring: Vector + Graph + History

**USP:** Matching score combines semantic similarity (40%), graph relationships (30%), and historical performance (30%)
in a single, explainable formula.

**Why it is unique:** Hybrid GraphRAG with explicit weights tuned for medical matching. Not only keyword or only vector
or only graph: all three with Apache AGE for doctor–case–condition relationships and PgVector for embeddings. Rationales
support transparency and audit.

---

## 5. Urgent First, Not First-In-First-Out

**USP:** Consultation queue is ordered by clinical urgency (CRITICAL / HIGH / MEDIUM / LOW), so the sickest patients are
seen first.

**Why it is unique:** AI classifies urgency and risk; queue is prioritized by clinical need, not arrival time. Reduces
the risk of critical consults waiting behind routine ones and gives coordinators a single, urgency-based view.

---

## 6. Right Sub-Specialist, Not Just "Oncologist" or "Cardiologist"

**USP:** Second opinions and complex cases are matched to the appropriate sub-specialist by diagnosis and complexity,
not only by broad specialty.

**Why it is unique:** Matching uses case analysis (ICD-10/SNOMED, complexity) and experience in that specific diagnosis.
Reduces misrouting to a generic specialty when a sub-specialist is needed; can prioritize telehealth when relevant.

---

## 7. Facility Matches Case Complexity (Regional Routing)

**USP:** Regional routing scores facilities by case complexity, historical outcomes, capacity, and geography, and
suggests lead specialists.

**Why it is unique:** Reduces facility–case mismatch in hierarchical systems. Not only "nearest" or "first available":
routing is driven by capability and outcomes, with ranked facilities and explanations. Referrals and transfers become
transparent and measurable.

---

## 8. Medical-Domain AI and Agent Skills

**USP:** MedGemma models and seven medical-specific agent skills (case-analyzer, doctor-matcher, evidence-retriever,
recommendation-engine, clinical-advisor, network-analyzer, routing-planner) for end-to-end clinical workflows.

**Why it is unique:** Purpose-built for medical expert matching and decision support, not a generic chatbot. Skills are
modular and documented; prompts and tools are medical-domain (ICD-10, guidelines, PubMed, risk, differential diagnosis).

---

## 9. Privacy-First and Deployment-Friendly

**USP:** Architecture supports local deployment and HIPAA-aware data handling; no PHI in logs or error messages;
anonymization in code and test data.

**Why it is unique:** Designed for healthcare privacy from the start. Can run on-premises or in a controlled cloud; AI
outputs include disclaimers; human-in-the-loop is explicit. Suited for environments where data cannot leave the
organization.

---

## 10. FHIR and EMR-Ready

**USP:** Consumes FHIR Bundles (Patient, Condition, Observations, Encounter); supports EMR integration and portals; REST
APIs for matching, queue, analytics, and routing.

**Why it is unique:** Fits into real clinical workflows via standard interoperability. Same logic supports direct text
input (e.g. after OCR) and FHIR-based flows, so hospitals and regional systems can integrate without replacing existing
EMRs.

---

## Summary Table

| USP | One-line claim                                               |
|-----|--------------------------------------------------------------|
| 1   | Match in minutes, not days                                   |
| 2   | "Who is good at what" made visible (graph + analytics)       |
| 3   | One copilot: analysis + evidence + recommendations + experts |
| 4   | Three-signal scoring: vector + graph + history               |
| 5   | Urgent first, not FIFO queue                                 |
| 6   | Right sub-specialist, not just specialty                     |
| 7   | Facility matches case complexity (regional routing)          |
| 8   | Medical-domain AI and agent skills                           |
| 9   | Privacy-first, local deployment, HIPAA-aware                 |
| 10  | FHIR and EMR-ready                                           |

---

*Last updated: 2026-02-08*
