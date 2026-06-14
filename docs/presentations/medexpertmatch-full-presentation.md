---
revealjs:
  presentation: true
  height: 800
---

<!-- .slide: class="title-image-slide" -->

<img class="title-image" src="../images/spring-ai-agent-skills-medical.png" alt="Spring AI Agent Skills Medical" />
<p style="text-align:center;font-size:0.7rem;margin-top:0.3rem;opacity:0.7;">by Siarhei Berdachuk</p>

---

## Why this project

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**Validate end-to-end:** PostgreSQL + PgVector + Apache AGE + Spring AI + **agents**

**How:** A full application with **realistic, demanding workflows** (medical domain)

**Question:** Do relational data, vectors, graph, LLM, and agent orchestration hold together in **one** stack?

<div style="display:flex;align-items:center;gap:0.5rem;">
<div><b>Code:</b><br/><a href="https://github.com/berdachuk/med-expert-match-ce" target="_blank">github.com/berdachuk/med-expert-match-ce</a></div>
<img class="reveal-slide-qr" src="../images/qr-github-repo.png" alt="QR: GitHub repository" style="max-width:220px;max-height:220px;" />
</div>

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

1. **Problems and value**
2. **Architecture** 
3. **Live demo**
4. **Secret BONUS**
5. **Q&A**

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

<!-- .slide: class="system-overview-slide" -->

<h3 style="margin:0.1em 0 0.3em 0;font-size:1.1rem;">System Overview</h3>

<img src="../images/system-overview-medexpertmach.png" alt="System Overview" style="max-width:100%;max-height:700px;width:auto;height:auto;border:none;box-shadow:none;" />


Note: The LLM module uses event-driven agent orchestration (PlanReadyEvent → ContextReadyEvent). The retrieval module fuses vector, graph, and history signals. All domain data lives in PostgreSQL 17 with pgvector and Apache AGE extensions.

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

## Graph structure diagram

<pre class="mermaid reveal-graph-slide">
graph TB
    subgraph Vertices["Graph vertices"]
        D[Doctor]
        C[MedicalCase]
        I[ICD10Code]
        S[MedicalSpecialty]
        F[Facility]
    end
    D -->|TREATED| C
    D -->|CONSULTED_ON| C
    D -->|SPECIALIZES_IN| S
    D -->|TREATS_CONDITION| I
    D -->|AFFILIATED_WITH| F
    C -->|HAS_CONDITION| I
    C -->|REQUIRES_SPECIALTY| S
    style D fill:#e3f2fd
    style C fill:#fff3e0
    style I fill:#f3e5f5
    style S fill:#e8f5e9
    style F fill:#fce4ec
</pre>

Note: Built with `MedicalGraphBuilderService`; Cypher via `GraphService`. Same model as [Apache AGE graph analysis](../APACHE_AGE_GRAPH_ANALYSIS.md).

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

## The Three-Signal Scorer

Doctor-to-case matching blends three independent signals into a single score in the 0–100 range.

<pre class="mermaid">
flowchart LR
    subgraph Inputs
        C[MedicalCase<br/>embedding]
        D[Doctor<br/>profile]
    end

    subgraph "Signal 1 — Vector (40%)"
        VEC[pgvector cosine<br/>similarity]
    end

    subgraph "Signal 2 — Graph (30%)"
        GR[Apache AGE<br/>Cypher traversal]
    end

    subgraph "Signal 3 — History (30%)"
        HI[ClinicalExperience<br/>outcomes]
    end

    subgraph Fusion
        WA["Weighted Average<br/>or RRF (k=60)"]
        RERANK[Semantic<br/>Re-ranker]
    end

    C --> VEC
    D --> VEC
    C --> GR
    D --> GR
    D --> HI
    VEC -->|0.4| WA
    GR  -->|0.3| WA
    HI  -->|0.3| WA
    WA --> RERANK
    RERANK --> Score["Score 0–100<br/>+ rationale"]
</pre>

Note: Weights are configurable per deployment. RRF (k=60) is an alternative to weighted average when signal calibration is uncertain.

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
- **9 tool classes** (27 `@Tool` methods) — `CaseAnalysisAgentTools`, `DoctorMatchingAgentTools`, `EvidenceAgentTools`, etc. — calls into domain services (no raw LLM → DB)
- **`AutoMemoryTools`** — cross-session durable memory (LLM self-curates facts across sessions)
- **Session memory** — `SessionMemoryAdvisor` compacts history after 15 turns (JDBC-backed)
- **Prompts** — external `.st` templates, `PromptTemplate` beans (45 templates, StringTemplate4)  

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-llm-tools.png" alt="LLM orchestration and tools" />

</div>

</div>

Note: Orchestration (Java) vs prompt text (Spring AI). This is the “harness” around the model.

---

## Nine agent skills (documentation + tools)

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

1. **case-analyzer** — entities, ICD-10, urgency  
2. **doctor-matcher** — SGR-based match  
3. **evidence-retriever** — guidelines, PubMed  
4. **recommendation-engine** — recommendations  
5. **clinical-advisor** — differential, risk  
6. **network-analyzer** — graph analytics  
7. **routing-planner** — facility routing  
8. **clinical-guideline** — condition-specific guidelines  
9. **triage** — urgency assessment, care level  

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-agent-skills.png" alt="Nine agent skills" />

</div>

</div>

Note: Skills map to `skills/*/SKILL.md` and tool groups in the `llm` module.

---

## How Skills Work: Architecture and Execution Flow

Spring AI describes the skill lifecycle in three stages: **discovery**, **activation**, and **execution**. The flow is:

<pre class="mermaid">
sequenceDiagram
    actor User
    participant SkillsTool as SkillsTool<br/>(registry)
    participant LLM
    participant Skill as SKILL.md<br/>(loaded on demand)
    participant Tool as @Tool method
    participant Service as Domain Service

    User->>SkillsTool: User message
    SkillsTool->>LLM: Registry (names + descriptions only)
    LLM-->>SkillsTool: Semantic match: "triage" skill identified
    SkillsTool->>Skill: Load full SKILL.md into context
    Skill->>Tool: Model calls tool function
    Tool->>Service: Tool invokes Spring service
    Service-->>Tool: Structured result
    Tool-->>Skill: Tool response in context
    Skill-->>User: Generated response with rationale
</pre>

1. **Discovery** — At startup, `SkillsTool` scans `src/main/resources/skills/` directories and extracts only `name` and `description` from each `SKILL.md` frontmatter. This lightweight registry is sent to the LLM.
2. **Activation** — When a user message semantically matches a skill, the model invokes `Skill("skill-name")`. Only then is the full `SKILL.md` content loaded into the context window.
3. **Execution** — Inside the skill's instructions, the model decides which `@Tool` methods to call. Tools execute in the local environment and return results into the skill's context.

> **Important**: Scripts referenced by a skill execute directly in the local environment without sandboxing. This is fine for read-only operations but requires careful scoping for anything that modifies state.

---

## Data access: graph and SQL

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

- **Graph:** all Cypher via **`GraphService`** (parameter embedding, consistent policy)  
- **SQL:** JDBC repositories, queries in **`.sql`** files, dedicated mappers  

**Example Cypher query** — scoring relationship depth (30% graph weight):

```cypher
MATCH (d:Doctor {id: $doctorId})
      -[:TREATED]->
      (c:MedicalCase)
      -[:HAS_CONDITION]->
      (i:ICD10Code {code: $icd10Code})
RETURN count(*)
```

Same pattern for `SPECIALIZES_IN`, `TREATS_CONDITION`, `AFFILIATED_WITH` — each returns a **normalized 0–1** score.

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

## Harness — idea

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**Idea:** The **model proposes**; the **harness constrains and executes**.

Java orchestration around the LLM — routing, context, tools, verify — not “hope the prompt picks the right action.”

**Benefit:** **Reliable** medical chat workflows — correct goal and case, observable progress, fewer dead-end replies.

The harness is the **leash**: direction and safety while the model does the work.

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-harness-metaphor.png" alt="Harness metaphor: guided control — model under harness direction" />

</div>

</div>

Note: ~15 s. Contrast with prompt-only agents. Next slide: workflow states.

---

## Harness — workflow

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

Doctor match (and routing / analyze) run a **state machine** — streamed as SSE `HARNESS_STATE`:

`PLANNING` → `CONTEXT_BUILT` → **`TOOLS_EXECUTED` ⇄ `VERIFYING`** → `POLICY_GATE` → `DONE`

- **Verify loop** — retry tools when output fails (default **2** passes)
- **Policy gate** — final safety check (PHI, disclaimer); fail-closed, no loop back

`GoalClassifier` routes match / analyze / route **before** the engine starts.

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-harness-workflow.png" alt="Harness workflow state machine with verify retry loop" />

</div>

</div>

Note: ~20 s. Walk the state line once; mention SSE in AI Chat. Details: [Harness Architecture](../HARNESS.md).

---

## Agent vs chat — four layers & ROI test

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**Product packaging (M66)** — single chat mode:

| Mode | Path | Relative cost |
|------|------|----------------|
| **Expert match (harness)** | GraphRAG workflow + MedGemma interpretation (FULL) | ~2–3× token budget |

**Four layers:** Chat (UX) → Harness (verify + tools) → Policy (ANSWER/CLARIFY/ESCALATE) → Data (outcomes flywheel)

**Go / no-go rule for paying for the agent:** harness must deliver **≥20% quality uplift** at **≤2× cost** vs chat-only on held-out eval (see [cost model](../eval/cost-model.md)).

Case study template: [agent-vs-chat-case-study-template.md](agent-vs-chat-case-study-template.md)

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-harness-workflow.png" alt="Harness vs chat packaging" />

</div>

</div>

Note: ~90 s. Demo the chat mode selector and match explainability panel (vector / graph / history %).

---

## FunctionGemma — tool calling

<div class="reveal-slide-row">

<div class="reveal-slide-text-col">

**Two models in AI Chat:**

| Role | Model | Job |
|------|-------|-----|
| Medical reasoning | **MedGemma** `1.5:4b` | Analysis, interpretation, goals, translation |
| Tool calling | **FunctionGemma** `270m` | Auto orchestrator — pick and invoke `@Tool` methods |

**Why split?** MedGemma excels at clinical text; FunctionGemma is fine-tuned for **structured function calls**.

**When harness routes** (match / route / analyze + case ID) → workflow engines — FunctionGemma **skipped**.

**Otherwise** → Auto chat path: FunctionGemma + tool classes (evidence, edge cases).

Config: `TOOL_CALLING_*` env vars → `functiongemma:270m` (Ollama OpenAI-compatible).

</div>

<div class="reveal-slide-image-col">

<img class="reveal-slide-image" width="768" height="1024" src="../images/slide-functiongemma.png" alt="FunctionGemma tool calling — MedGemma to tools flow" />

</div>

</div>

Note: ~25 s. Harness handles high-value flows; FunctionGemma covers the long tail. Details: [FunctionGemma Tool Calling](../FUNCTIONGEMMA.md).

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
- [Harness architecture](../HARNESS.md)
- [FunctionGemma tool calling](../FUNCTIONGEMMA.md)
- [Presentation plan — speaker script (RU)](../PRESENTATION_PLAN_RU.md)
