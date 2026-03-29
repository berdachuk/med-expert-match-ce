# Harness Engineering Ideas and Agent Usage in MedExpertMatch

**Purpose:** Relate common ideas from AI coding-agent discussions (model versus harness, tools, policy, repository
conventions) to how MedExpertMatch is built and how agents are expected to work in this repository.

**Audience:** Developers and coding agents (for example Cursor) that change or extend the codebase.

---

## 1. Core ideas (short)

| Idea | Meaning |
|------|---------|
| **Model** | The LLM that proposes text, plans, or structured actions. |
| **Harness** | Everything around the model: prompts, context assembly, tool definitions, execution, retries, logging policy, and safety rules. The harness turns model output into reliable behavior in your system. |
| **Agent loop** | Repeated cycle: send state and tools to the model, receive a proposed action, execute in the harness, feed results back. |
| **Policy** | What is allowed to run, what data may leave the system, and how failures are handled. Distinct from raw model capability. |

Stronger models help, but **reliability and safety are largely harness problems**: clear tools, bounded execution, good
context, and observable failures.

---

## 2. Two kinds of agents in this project

### 2.1 Coding agents (development time)

Tools like Cursor use repository context, `AGENTS.md`, tests, and build commands. They **edit and navigate** this codebase.
Their harness is the editor, rules, and your **MedExpertMatch Development Guide** (`AGENTS.md` at the repo root).

### 2.2 Medical LLM orchestration (runtime)

The application runs **Spring AI** orchestration: `MedicalAgentService` and workflow services call a chat client, use
**prompt templates** (`.st` files), and expose **tools** (`MedicalAgentTools`) over domain services. That stack is the
**product harness** for MedGemma (or other OpenAI-compatible chat endpoints), not a shell sandbox like a coding CLI.

Both layers share the same principle: **the model proposes; the harness constrains and executes**.

---

## 3. How MedExpertMatch maps the harness

| Harness concern | In this repository |
|-----------------|-------------------|
| **Orchestration** | `MedicalAgentService`, workflow services under `llm/service/impl/`, coordination with `MedicalAgentTools`. |
| **Tools** | `MedicalAgentTools`: Spring `@Tool` methods wired to repositories, retrieval, graph, evidence, and related services. |
| **Prompts** | External templates in `src/main/resources/prompts/`; `PromptTemplate` beans (see `PromptTemplateConfig` pattern). |
| **Skills (documentation for behavior)** | `src/main/resources/skills/*/SKILL.md` (for example `case-analyzer`, `doctor-matcher`, `evidence-retriever`). Optional runtime loading may be enabled via configuration; skills describe intent and boundaries for medical workflows. |
| **Graph access** | All Cypher goes through `GraphService` with embedded parameters; agents and developers must not bypass it with raw JDBC to the graph. |
| **Data and transactions** | Business rules and `@Transactional` at the service layer; repositories stay single-entity focused. |
| **Policy (medical and privacy)** | No PHI in logs; anonymized test data; fail-fast error handling; medical disclaimers in LLM-facing flows as required by project rules. |

---

## 4. Repository conventions as agent affordances

Industry discussion often stresses **`AGENTS.md`**, clear module boundaries, and naming so that **both humans and agents**
can search and change code safely.

MedExpertMatch encodes that in:

- **Root `AGENTS.md`** Build commands (`mvn` phases), test containers, Spring Modulith expectations, GraphService usage,
  prompt rules, and HIPAA-related constraints.
- **Spring Modulith** Packages with `package-info.java` and explicit allowed dependencies reduce accidental cross-module
  coupling when many edits are automated.
- **Predictable layout** Domain-driven packages (`doctor`, `medicalcase`, `retrieval`, `llm`, and so on) align with how
  tools and retrieval should route.

Treating **`AGENTS.md` as the contract** for coding agents matches the idea of keeping instructions **accurate and
minimal**: prefer pointers to code and commands over duplicated prose that drifts.

---

## 5. Practices that align with harness thinking

1. **Prefer the harness you already have** Extend `MedicalAgentTools` and domain services instead of ad-hoc LLM calls from
   controllers.
2. **Keep prompts in `.st` files** So prompt changes stay reviewable and separate from Java control flow.
3. **Preserve boundaries** New features should respect module dependencies declared in `package-info.java`.
4. **Tests as specification** Integration tests (`*IT`) and `BaseIntegrationTest` patterns document expected behavior for
   humans and for agents that run `mvn test` / `mvn verify`.
5. **Security and safety** Distinguish **what the infrastructure allows** (transactions, GraphService, no PHI in logs) from
   **what the model should recommend** (clinical disclaimers, human-in-the-loop for real care decisions).

---

## 6. Related documentation

- [AGENTS.md](../AGENTS.md) (repository root) Development guide for builds, tests, and conventions.
- [ARCHITECTURE.md](ARCHITECTURE.md) System architecture.
- [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) Phased delivery context (including agent skills implementation).

---

**Note:** This document describes **patterns and alignment** with common agent/harness terminology. It is not a transcript
or endorsement of any external product. For MedExpertMatch-specific requirements, the root `AGENTS.md` and
`.cursorrules` remain authoritative.
