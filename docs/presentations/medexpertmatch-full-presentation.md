---
revealjs:
  presentation: true
  height: 800
---

## Why this project

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**Validate end-to-end:** PostgreSQL + PgVector + Apache AGE + AI + **agents**

**How:** A full application with **realistic, demanding workflows** (medical domain)

**Question:** Do relational data, vectors, graph, LLM, and agent orchestration hold together in **one** stack?

**Code:** [github.com/berdachuk/med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce)

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-intro-why.png" alt="End-to-end stack validation concept" />

</div>

</div>

Note: Not a toy benchmark. MedExpertMatch must be a credible applied scenario.

---

## PostgreSQL as a unified data platform

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**MedExpertMatch** POC

Specialist matching and clinical decision support — network data, hybrid retrieval, LLM orchestration

The medical domain as a stress test for PostgreSQL, PgVector, Apache AGE, and the agent layer

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-postgresql-unified.png" alt="Unified PostgreSQL data platform" />

</div>

</div>

Note: Evidence from data, not only intuition. Support tool; clinicians and policy own decisions.

---

## Agenda (about 45 minutes)

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

1. **Problems and value** (~10 min)
2. **Architecture** (~20 min)
3. **Live demo** (~7 min)
4. **Wrap-up** (~2 min)
5. **Q&A** (~14 min) — separate block after slides

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-agenda.png" alt="Talk agenda overview" />

</div>

</div>

Note: Architecture follows the problem framing. Demo uses the same stack described here.

---

## Siarhei Berdachuk

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- Experienced IT professional with 30+ years in software development, AI integration, and Architecture design.

- Proven leader and mentor skilled in team coordination, Java, databases, TDD, microservices, and cloud-based solutions.

- Expert in legacy code refactoring, technical issue resolution, and delivering robust, high-quality solutions.

- **Site:** [berdachuk.com](https://berdachuk.com)

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/siarhei-berdachuk.jpeg" alt="Siarhei Berdachuk" />

</div>

</div>

Note: Keep employer or affiliation out unless your venue requires it; add one line here if needed.

---

## Prototype and privacy

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **Scenarios:** Inpatient match, queue, second opinion, routing, case analysis
- **Demo data:** Synthetic / anonymized; **no PHI in logs** in public demos
- **Stack:** Spring Boot, PostgreSQL 17, PgVector, Apache AGE, Thymeleaf UI, OpenAI-compatible LLM APIs

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-prototype-privacy.png" alt="Prototype demo and privacy safeguards" />

</div>

</div>

Note: Reproducible demo profile; see Demo Guide. Do not claim certifications you do not have.

---

## Problem 1: Consultation delays

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **Symptom:** Days or weeks to see the right specialist
- **Impact:** Risk, length of stay, ward load
- **Product angle:** Matching and queue **prioritization in minutes** at selection time (not replacing clinician time)

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/problem1-consultation-delays.png" alt="Abstract illustration: consultation delay versus fast prioritization" />

</div>

</div>

Note: USE_CASES §1 inpatient, §3 queue. Story: case arrives overnight; by morning you need ranked candidates with rationale.

---

## Problem 2: Invisible expertise

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **Symptom:** “Who is best at X?” lives in heads, not systems
- **Impact:** Wrong routing, uneven load, weak mentorship signals
- **Product angle:** Explicit **doctor–case–context** graph and **network analytics**

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/problem2-invisible-expertise.png" alt="Hidden expertise versus visible network graph" />

</div>

</div>

Note: Org chart of specialties is not the map of real competence.

---

## Problem 3: Fragmented decision support

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **Symptom:** Case breakdown, evidence, and recommendations sit in different tools
- **Product angle:** **One agent-driven path** — analysis, PubMed/guidelines, recommendations, matching

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/problem3-fragmented-support.png" alt="Fragmented tools versus unified agent path" />

</div>

</div>

Note: Second opinion and decision-support flows in USE_CASES and Demo Guide.

---

## Problem 4: Suboptimal use of resources

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **Symptom:** “Nearest” facility is not always **appropriate**
- **Product angle:** **routing-planner** skill + semantic and graph signals for facilities

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/problem4-resources.png" alt="Appropriate facility routing versus nearest only" />

</div>

</div>

Note: Regional routing; web UI `/routing`.

---

## Problems to product (summary)

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

| Problem | MedExpertMatch response |
|---------|-------------------------|
| Delays | Match + queue priority |
| Hidden expertise | Graph + network analytics |
| Fragmented support | Agent: analysis + evidence + recommendations |
| Resources | Facility routing with capabilities |

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-problems-summary.png" alt="Problems mapped to product responses" />

</div>

</div>

---

## Six core scenarios

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

| # | Scenario | Primary actors |
|---|----------|----------------|
| 1 | Complex inpatient specialist match | Attending, case manager |
| 2 | Second opinion / telehealth | Referring physician, portal |
| 3 | Consult queue prioritization | Coordinator |
| 4 | Network expertise analytics | CMO, analytics |
| 5 | Regional routing to facilities | Operations |
| 6 | Decision support (analysis + evidence) | Clinician |

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-six-scenarios.png" alt="Six core usage scenarios" />

</div>

</div>

Note: Typical inputs: **FHIR bundle** or **text** (`match-from-text`) depending on integration.

---

## Metrics and honesty

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **Direction:** Reduce time-to-match; aim for reliable operation of critical matching paths (see PRD)
- **Maturity:** Distinguish **MVP / prototype** from full PRD scope when you speak

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-metrics-honesty.png" alt="Honest metrics and maturity framing" />

</div>

</div>

Note: Align numbers with your actual release; do not oversell production metrics.

---

## Request path (Find Specialist)

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**One thread:** `MedicalCase` stored and embedded (PgVector) → **Agent API** → **case-analyzer** → **doctor-matcher** → **SemanticGraphRetrievalService** scores candidates → ranked list + rationale in UI/API

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-request-path.png" alt="Find Specialist request path pipeline" />

</div>

</div>

Note: Walk this once slowly; it anchors the rest of the architecture section.

---

## Hybrid retrieval (GraphRAG)

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**Channels:** vector + graph + keyword → **fusion (RRF)** → semantic **rerank**

- **RRF** — *reciprocal rank fusion*: each channel returns a **ranked** list; RRF merges those lists using rank-based scores so you **do not** have to put vector, graph, and keyword scores on one calibrated scale.

**Why not embeddings alone?** Similar text profiles can hide different **real** caseloads and outcomes.

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-hybrid-graphrag.png" alt="Hybrid GraphRAG retrieval channels" />

</div>

</div>

Note: Deeper question: each hit earns **1 / (k + rank)** from every list where it appears (**k** is a small constant in the paper; implementations pick a value). Scores add across lists—still no raw-score calibration between channels.

---

## Semantic Graph Retrieval (SGR)

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**SGR** = **Semantic Graph Retrieval** — `SemanticGraphRetrievalService`

**Typical composite weights (doctor–case match):**

- **40%** — PgVector similarity  
- **30%** — Apache AGE graph relationships  
- **30%** — historical performance (outcomes, ratings)

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-sgr.png" alt="Semantic Graph Retrieval scoring blend" />

</div>

</div>

Note: Same ideas feed network-analyzer and routing where applicable.

---

## Spring Modulith and domains

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**Examples:** `core`, `doctor`, `medicalcase`, `caseanalysis`, `embedding`, `retrieval`, `llm`, `graph`, `ingestion`, `web`

**Intent:** Clear module boundaries; **`llm`** deliberately orchestrates cross-domain medical flows

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-modulith.png" alt="Spring Modulith bounded contexts" />

</div>

</div>

Note: `web` composes UI; core domain logic stays in domain modules.

---

## LLM orchestration and tools

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **`MedicalAgentService`** + workflow services  
- **`MedicalAgentTools`** — Spring AI `@Tool` calls into domain services (no raw LLM → DB)  
- **Prompts** — external `.st` templates, `PromptTemplate` beans  

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-llm-tools.png" alt="LLM orchestration and tools" />

</div>

</div>

Note: Orchestration (Java) vs prompt text (Spring AI). This is the “harness” around the model.

---

## Seven agent skills (documentation + tools)

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

1. **case-analyzer** — entities, ICD-10, urgency  
2. **doctor-matcher** — SGR-based match  
3. **evidence-retriever** — guidelines, PubMed  
4. **recommendation-engine** — recommendations  
5. **clinical-advisor** — differential, risk  
6. **network-analyzer** — graph analytics  
7. **routing-planner** — facility routing  

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-seven-skills.png" alt="Seven agent skills" />

</div>

</div>

Note: Skills map to `skills/*/SKILL.md` and tool groups in the `llm` module.

---

## Data access: graph and SQL

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **Graph:** all Cypher via **`GraphService`** (parameter embedding, consistent policy)  
- **SQL:** JDBC repositories, queries in **`.sql`** files, dedicated mappers  

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-data-access.png" alt="Graph and SQL data access" />

</div>

</div>

Note: Easier security review when graph access has a single front door.

---

## Ingestion and FHIR

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **`ingestion`** module — FHIR adapters (Patient, Condition, Encounter, Observation, …)  
- **Synthetic data** path — embeddings + **graph build** after load  

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-ingestion-fhir.png" alt="FHIR ingestion pipeline" />

</div>

</div>

Note: Deepen only if time; otherwise one slide is enough.

---

## Privacy and deployment

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- Minimize PHI; anonymized test and demo data  
- Medical disclaimers in LLM-facing outputs (project policy)  
- **OpenAI-compatible** chat/embedding endpoints; profiles (`local`, `demo`, …)

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-privacy-deploy.png" alt="Privacy and deployment configuration" />

</div>

</div>

Note: Principles and architecture readiness, not a legal “HIPAA certified” claim without evidence.

---

## Find Specialist flow (steps)

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

1. Create / ingest case (FHIR or UI) → **`MedicalCase`**  
2. Generate **embedding** → PgVector  
3. Call agent match — **`POST /api/v1/agent/match/{caseId}`** (or `match-from-text`)  
4. **case-analyzer** refines case  
5. **doctor-matcher** + **`score(case, doctor)`** loop via SGR  
6. Return ranked doctors with scores and rationale  

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-flow-steps.png" alt="Find Specialist flow steps" />

</div>

</div>

Note: USE_CASES §1 has the full sequence diagram.

---

## Demo: preparation

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- PostgreSQL 17 + PgVector + AGE; profile **`demo`**  
- Docker: `postgres-demo` per **Demo Guide**  
- Generate **medium** dataset (e.g. hundreds of doctors / cases)  

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-demo-prep.png" alt="Demo environment preparation" />

</div>

</div>

Note: If live fails, fall back to screenshots or recorded video.

---

## Demo: dashboard

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **URL:** `/`  
- Show live counts after data generation  

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-demo-dashboard.png" alt="Application dashboard" />

</div>

</div>

Note: Sets context before match and analyze.

---

## Demo: specialist match

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **URL:** `/match` — select case → **Match Doctors**  
- Call out **scores + rationale** and tie to **SGR** (vector + graph + history)  

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-demo-match.png" alt="Specialist match UI" />

</div>

</div>

Note: Core proof of the stack.

---

## Demo: case analysis

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **URL:** `/analyze/{caseId}`  
- ICD-10 / analysis text / recommendations / links to evidence  

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-demo-analyze.png" alt="Case analysis screen" />

</div>

</div>

Note: Shows agent + clinical path beyond pure ranking.

---

## Demo: queue or analytics

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **Queue:** `/queue` — **Prioritize Queue** (urgency, complexity)  
- **or Analytics:** `/analytics` — e.g. expertise by ICD-10 code  

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-demo-queue.png" alt="Queue prioritization or analytics" />

</div>

</div>

Note: Pick one if short on time.

---

## Demo: routing

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **URL:** `/routing` — case → **Route Case**  
- Mention facility factors (capacity, geography, performance)  

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-demo-routing.png" alt="Regional facility routing" />

</div>

</div>

Note: Same data stack; different UI entry point.

---

## Demo: API reminder

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

Example: **`POST /api/v1/agent/match/{caseId}`**

Same stack whether called from EMR integration or the bundled UI.

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-demo-api.png" alt="REST API integration" />

</div>

</div>

Note: Point to OpenAPI / Medical Agent docs if asked.

---

## Conclusion

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**Stack experiment:** PostgreSQL + PgVector + AGE + AI + agents on **hard** workflows

**Problems addressed:** delays, invisible expertise, fragmented support, resource mismatch

**Design pillars:** **GraphRAG + SGR**, **Modulith**, **agent + tools**, **data and graph policy**

**Next:** **Q&A** — short questions; deep dives offline

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-conclusion.png" alt="Conclusion design pillars" />

</div>

</div>

Note: Prototype end-to-end validation, not exhaustive production load testing—unless your team has that evidence.

---

## Thank you

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**Questions?**

Further reading: 

- [Use Cases](../../USE_CASES/)
- [Architecture](../../ARCHITECTURE/)
- **Site:** [berdachuk.com](https://berdachuk.com)
- **Code:** [github.com/berdachuk/med-expert-match-ce](https://github.com/berdachuk/med-expert-match-ce)

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-thank-you.png" alt="Thank you" />

</div>

</div>

Note: Use the Q&A bank in PRESENTATION_PLAN_RU (speaker script) for framing answers.

=====

## Below the deck (references)

This section is normal MkDocs content (printable, searchable).

- [Architecture overview](../ARCHITECTURE.md)
- [Use cases](../USE_CASES.md)
- [Demo guide](../DEMO_GUIDE.md)
- [Harness and agent usage](../HARNESS_AND_AGENT_USAGE.md)
- [Presentation plan — speaker script (RU)](../PRESENTATION_PLAN_RU.md)
