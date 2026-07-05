# Model Selection Guide

**Last Updated:** 2026-06-08  
**Related:** [AI Provider Configuration](AI_PROVIDER_CONFIGURATION.md), [M64 Cost-Quality Tier Routing ADR](decisions/M64-cost-quality-tier-routing.md), [FunctionGemma](FUNCTIONGEMMA.md), [Harness Architecture](HARNESS.md)

## Purpose

This guide maps **which model serves which role** in MedExpertMatch and recommends **two deployment profiles**:

1. **Local-only** — all inference on your machine (privacy-first, hackathon/demo default).
2. **Hybrid with Ollama Cloud** — lightweight/local auxiliary work + frontier cloud models for high-stakes harness paths.

MedExpertMatch uses **OpenAI-compatible endpoints only**. Configure models via role-specific environment variables
(`CLINICAL_*`, `UTILITY_*`, `TOOL_CALLING_*`, `EMBEDDING_*`, `RERANKING_*`). See [AI Provider Configuration](AI_PROVIDER_CONFIGURATION.md) for wiring details.

> **Disclaimer:** Models listed here are **not certified medical devices**. All clinical outputs require human review.
> Do not send PHI to cloud endpoints without proper agreements and anonymization.

---

## Model roles in the application

After M67, each LLM call uses a **role-specific bean** and concurrency pool:

| Role | Config prefix | Bean | Tier (M64) | What it does |
|------|---------------|------|------------|--------------|
| **Clinical** | `CLINICAL_*` | `clinicalChatModel` | **T3 / FULL** | Harness analyze/interpret, case analysis, match interpretation, policy-sensitive patient-facing text |
| **Utility** | `UTILITY_*` | `utilityChatModel` | **T2 / STANDARD** | Goal-classify LLM fallback, translation, summarization, synthetic case descriptions |
| **Tool calling** | `TOOL_CALLING_*` | `toolCallingChatModel` | **T1 / LIGHT** | Auto chat orchestrator — `@Tool` selection, simple Q&A, evidence browse |
| **Reranking** | `RERANKING_*` | `rerankingChatModel` | T2 (retrieval) | Semantic rerank of GraphRAG candidates (falls back to utility endpoint) |
| **Embedding** | `EMBEDDING_*` | `primaryEmbeddingModel` | — | PgVector embeddings for case/doctor similarity (not a chat model) |

**Routing rule:** expensive clinical reasoning runs only on FULL harness goals (`MATCH_DOCTORS`, `ROUTE_CASE`,
`ANALYZE_CASE`). Routine work should stay on utility or tool-calling models.

```text
User message
  → GoalClassifier (rules → keywords → utility LLM fallback)
  → FULL + caseId  → harness engines → clinical LLM (interpret)
  → LIGHT/STANDARD → FunctionGemma tool-calling chat
```

---

## Profile 1 — Local-only (recommended baseline)

**When to use:** development, demos, air-gapped or HIPAA-sensitive pilots, single-GPU workstation.

**Endpoint:** Ollama, LM Studio, or vLLM exposing OpenAI-compatible `http://localhost:PORT/v1`.

### Recommended model stack

| Role | Recommended model | Ollama tag (example) | VRAM / notes |
|------|-------------------|----------------------|--------------|
| Clinical (T3) | **MedGemma 1.5 4B** | `medgemma1.5:4b` | ~4–6 GB; HAI-DEF aligned with project origin; best local clinical choice |
| Utility (T2) | **Qwen3.5 4B** (preferred) or **Qwen3.5 2B** | `qwen3.5:4b` / `qwen3.5:2b` | ~3–4 GB / ~1–2 GB; better RU/EN classify & translate at 4B |
| Tool calling (T1) | **FunctionGemma 270M** | `functiongemma:270m` | ~0.5 GB; required for reliable tool selection |
| Reranking | Same as utility | `qwen3.5:4b` | Low temperature (0.1); same Ollama URL as utility |
| Embedding | **Nomic Embed Text** | `nomic-embed-text:v1.5` | 768 dims; must match `EMBEDDING_DIMENSIONS` |

### Example `application-local.yml` (local-only)

```yaml
# Clinical — harness + case analysis (T3)
CLINICAL_BASE_URL: http://127.0.0.1:11434/v1
CLINICAL_API_KEY: none
CLINICAL_MODEL: medgemma1.5:4b
CLINICAL_TEMPERATURE: 0.7
CLINICAL_MAX_TOKENS: 6000

# Utility — classify, translate, summarization (T2)
UTILITY_BASE_URL: http://127.0.0.1:11434/v1
UTILITY_API_KEY: none
UTILITY_MODEL: qwen3.5:4b
UTILITY_TEMPERATURE: 0.1
UTILITY_MAX_TOKENS: 4096

# Tool calling — Auto chat orchestrator (T1)
TOOL_CALLING_BASE_URL: http://127.0.0.1:11434/v1
TOOL_CALLING_API_KEY: none
TOOL_CALLING_MODEL: functiongemma:270m
TOOL_CALLING_MAX_TOKENS: 4096

# Embeddings — GraphRAG vectors
EMBEDDING_BASE_URL: http://127.0.0.1:11434/v1
EMBEDDING_API_KEY: none
EMBEDDING_MODEL: nomic-embed-text:v1.5
EMBEDDING_DIMENSIONS: 768

# Reranking — optional; falls back to UTILITY_* if unset
RERANKING_MODEL: qwen3.5:4b
RERANKING_TEMPERATURE: 0.1
```

### Pull commands (Ollama)

```bash
ollama pull medgemma1.5:4b
ollama pull qwen3.5:4b
ollama pull functiongemma:270m
ollama pull nomic-embed-text:v1.5
```

### Local-only trade-offs

| Pros | Cons |
|------|------|
| No data leaves the machine | MedGemma 4B weaker than frontier cloud on complex cases |
| Predictable cost | Single Ollama queue limits parallel clinical + utility (`clinical: 1`, `utility: 2` defaults) |
| Matches MedGemma Impact Challenge story | FunctionGemma tool accuracy may need server-side guards (see [FUNCTIONGEMMA.md](FUNCTIONGEMMA.md)) |

**Minimum GPU:** 8 GB VRAM for 4B clinical + small utility serially; **16 GB+** comfortable for parallel utility + clinical.

---

## Profile 1b — Workstation with 2× 16 GB (single Ollama)

**Reference hardware:** Ubuntu 22.04, **RTX 5060 Ti 16 GB** + **RTX 4060 Ti 16 GB**, 64 GB RAM, Core i5-13400.

**Setup:** one `ollama serve` on **`http://127.0.0.1:11434/v1`**. Ollama uses both GPUs automatically (layer split / shared VRAM pool). M67 **role separation is by model name**, not by port — same pattern as [application-local.yml.sample](../src/main/resources/application-local.yml.sample).

### Recommended model stack (this server)

| Role | Model | Base URL |
|------|-------|----------|
| Clinical | `medgemma1.5:4b` | `http://127.0.0.1:11434/v1` |
| Utility | `qwen3.5:4b` | `http://127.0.0.1:11434/v1` |
| Tool calling | `functiongemma:270m` | `http://127.0.0.1:11434/v1` |
| Embedding | `nomic-embed-text:v1.5` | `http://127.0.0.1:11434/v1` |
| Reranking | `qwen3.5:4b` | `http://127.0.0.1:11434/v1` |

**Why `qwen3.5:4b` over `2b`:** with ~32 GB combined VRAM, 4B utility fits comfortably and improves Russian/English classify/translate. Use `qwen3.5:2b` only if you need the smallest latency footprint.

### Start Ollama (Ubuntu 22.04)

```bash
ollama serve
ollama pull medgemma1.5:4b qwen3.5:4b functiongemma:270m nomic-embed-text:v1.5
```

Optional — keep models warm:

```bash
ollama run medgemma1.5:4b "ping"
ollama run qwen3.5:4b "ping"
```

Copy [application-local.yml.sample](../src/main/resources/application-local.yml.sample) to `application-local.yml` (gitignored).

### Concurrency (single Ollama queue)

One daemon serializes GPU work per loaded model. Suggested limits:

| Setting | Value |
|---------|-------|
| `clinical.max-concurrent-calls` | **1** |
| `utility.max-concurrent-calls` | **2** |
| `tool-calling.max-concurrent-calls` | **1** |
| `embedding.max-concurrent-calls` | **1** |

Clinical should stay at `1` on a shared endpoint; utility `2` is the [application.yml](../src/main/resources/application.yml) default.

### Optional tuning

| Env / setting | Purpose |
|---------------|---------|
| `OLLAMA_MAX_LOADED_MODELS=3` | Reduce model swap latency between clinical and utility |
| `OLLAMA_NUM_PARALLEL=2` | Allow limited parallel requests (watch VRAM) |
| `qwen3.5:2b` utility | Faster classify if 4B swaps too often |

---

## Profile 2 — Hybrid with [Ollama Cloud](https://ollama.com/search?c=cloud)

**When to use:** production pilots needing stronger reasoning without running 100B+ models locally; burst capacity for harness FULL paths while keeping embeddings local.

Ollama Cloud hosts **tools** and **thinking** models (see the [cloud model catalog](https://ollama.com/search?c=cloud)). After `ollama signin`, the local Ollama daemon can run cloud-backed models by name through the same OpenAI-compatible `http://localhost:11434/v1` API — model tags route to cloud inference.

### Recommended hybrid stack

| Role | Recommended model | Ollama cloud tag | Rationale |
|------|-------------------|------------------|-----------|
| Clinical (T3) | **DeepSeek V4 Pro** | `deepseek-v4-pro` | Frontier MoE with reasoning modes; strong agentic/harness interpretation ([catalog](https://ollama.com/search?c=cloud)) |
| Utility (T2) | **DeepSeek V4 Flash** | `deepseek-v4-flash` | 284B MoE / 13B active — efficient for classify, translate, rerank at scale |
| Tool calling (T1) | **FunctionGemma 270M** (local) or **Qwen3-Coder-Next** (cloud) | `functiongemma:270m` / `qwen3-coder-next` | Keep FunctionGemma local for cost; cloud coder model if tool latency is acceptable |
| Reranking | **DeepSeek V4 Flash** | `deepseek-v4-flash` | Share utility endpoint; low temperature |
| Embedding | **Nomic Embed Text** (local) | `nomic-embed-text:v1.5` | Keep vectors on-prem; cloud catalog is chat-focused |

**Why DeepSeek V4 Pro + Flash together:** Pro handles **high-stakes clinical polish** (match interpretation, case analysis narrative); Flash handles **high-volume auxiliary** work (goal classify fallback, translation, rerank) at lower cost — matching the M64 T3/T2 split.

### Alternative cloud picks (same roles)

| Role | Alternatives on Ollama Cloud | When to prefer |
|------|------------------------------|----------------|
| Clinical | `gemma4:26b`, `qwen3.5:27b`, `nemotron-3-super:120b` | Need multimodal (Gemma 4) or NVIDIA agent stack |
| Utility | `qwen3.5:4b`, `nemotron-3-nano:4b`, `gemma4:12b` | Cheaper/smaller cloud utility slot |
| Tool calling | `gemma4:12b`, `minimax-m2.5` (tools + cloud) | FunctionGemma insufficient; willing to pay cloud tool latency |

### Example `application-local.yml` (hybrid cloud)

```yaml
# Prerequisites: ollama signin && ollama pull deepseek-v4-pro && ollama pull deepseek-v4-flash

# Clinical — cloud frontier (T3 / FULL harness)
CLINICAL_BASE_URL: http://127.0.0.1:11434/v1
CLINICAL_API_KEY: none
CLINICAL_MODEL: deepseek-v4-pro
CLINICAL_TEMPERATURE: 0.5
CLINICAL_MAX_TOKENS: 6000

# Utility — cloud efficient (T2)
UTILITY_BASE_URL: http://127.0.0.1:11434/v1
UTILITY_API_KEY: none
UTILITY_MODEL: deepseek-v4-flash
UTILITY_TEMPERATURE: 0.1
UTILITY_MAX_TOKENS: 4096

# Tool calling — keep local for cost (recommended)
TOOL_CALLING_BASE_URL: http://127.0.0.1:11434/v1
TOOL_CALLING_MODEL: functiongemma:270m

# Embeddings — stay local (PHI-sensitive vectors)
EMBEDDING_BASE_URL: http://127.0.0.1:11434/v1
EMBEDDING_MODEL: nomic-embed-text:v1.5
EMBEDDING_DIMENSIONS: 768

RERANKING_MODEL: deepseek-v4-flash
RERANKING_TEMPERATURE: 0.1
```

### Hybrid trade-offs

| Pros | Cons |
|------|------|
| Frontier quality on harness without local 100B+ GPU | Cloud calls: latency, cost, data-processing agreements |
| Flash/Pro split mirrors M64 cost-quality design | Must not send PHI in prompts without anonymization |
| Embeddings stay local | Requires Ollama Cloud subscription / sign-in |

---

## Quick comparison

| Concern | Local single-GPU | 2×16 GB + single Ollama | Hybrid (DeepSeek V4) |
|---------|-----------------|-------------------------|----------------------|
| Privacy | Highest | Highest | Clinical/utility may leave premises |
| Clinical quality | Good (MedGemma 4B) | Good (MedGemma 4B) | Excellent (V4 Pro) |
| Parallel clinical + utility | Serial | Serial (one Ollama queue); 32 GB VRAM helps swaps | Cloud scale |
| Auxiliary cost | Low (Qwen3.5 4B) | Low (Qwen3.5 4B) | Low–medium (V4 Flash) |
| Tool calling | FunctionGemma local | FunctionGemma local | FunctionGemma local (recommended) |
| Setup complexity | `ollama pull` × 4 | One `ollama serve` + pulls | `ollama signin` + cloud pulls |
| Best for | Dev, demo | **5060+4060 workstation** | Pilot scale, complex cases |

---

## Model × application feature matrix

| Application feature | Primary role | Local model | Hybrid cloud model |
|--------------------|--------------|-------------|-------------------|
| Find Specialist harness | Clinical + Tool | `medgemma1.5:4b` + `functiongemma:270m` | `deepseek-v4-pro` + `functiongemma:270m` |
| Case analysis / interpret | Clinical | `medgemma1.5:4b` | `deepseek-v4-pro` |
| Follow-up goal classify | Utility | `qwen3.5:4b` | `deepseek-v4-flash` |
| Chat translation (RU/EN) | Utility | `qwen3.5:4b` | `deepseek-v4-flash` |
| GraphRAG vector search | Embedding | `nomic-embed-text:v1.5` | `nomic-embed-text:v1.5` (local) |
| Doctor reranking | Reranking / Utility | `qwen3.5:4b` | `deepseek-v4-flash` |
| Simple medical Q&A | Tool | `functiongemma:270m` | `functiongemma:270m` or `qwen3-coder-next` |

---

## Sizing and concurrency

Default limits in [application.yml](../src/main/resources/application.yml):

```yaml
medexpertmatch.llm.clinical.max-concurrent-calls: 1    # FULL harness — heavy
medexpertmatch.llm.utility.max-concurrent-calls: 2    # auxiliary — lighter (single Ollama queue)
medexpertmatch.llm.tool-calling.max-concurrent-calls: 1
medexpertmatch.llm.embedding.max-concurrent-calls: 1
medexpertmatch.llm.reranking.max-concurrent-calls: 1
```

Override via `MEDEXPERTMATCH_LLM_*_MAX_CONCURRENT_CALLS`. Raise utility on cloud profiles; keep clinical at 1–2 unless you have quota headroom.

Token budgets (M64 tiers) are enforced per role:

| Tier | Default max tokens | Role |
|------|-------------------|------|
| FULL | 6000 | Clinical |
| STANDARD | 4096 | Utility |
| LIGHT | 2048 | Tool calling |

---

## Validation checklist

After changing models:

1. `curl http://<ollama-host>:11434/v1/models` — all role model tags visible (e.g. `medgemma1.5:4b`, `qwen3.5:4b`, `functiongemma:270m`, `nomic-embed-text:v1.5`).
2. Actuator health — `clinicalLlm` and `utilityLlm` UP (`/actuator/health`).
3. `mvn test` — unit tests (mocked LLM).
4. Manual smoke: harness match turn + Russian follow-up classify (see [DEMO_GUIDE.md](DEMO_GUIDE.md)).
5. Optional: `./scripts/run-tool-selection-live-eval.sh baseline` if tool model changed.

---

## Agent session memory tuning (`agent.session.*`)

Spring AI Session JDBC backs chat short-term memory. Tune via `application.yml` or env vars:

| Property | Default | Effect |
|----------|---------|--------|
| `agent.session.max-turns` | 20 | Turn-count compaction trigger |
| `agent.session.max-tokens` | 4000 | Estimated-token compaction trigger (either trigger fires compaction) |
| `agent.session.max-window-turns` | 30 | Turns retained after compaction (non-LLM window; PHI-safe) |
| `agent.session.retention-days` | `chat.retention.idle-days` | JDBC session purge aligned with chat retention |

After compaction, the orchestrator can call `conversation_search` to keyword-search full history. See [Harness and Agent Usage](HARNESS_AND_AGENT_USAGE.md) §2.3.

---

## Structured JSON output (M140)

Case analysis and goal classification use Spring AI 2.0 `.entity(..., validateSchema())` with `LenientJsonOutputConverter` for fence-tolerant parsing and up to three schema self-correction retries.

| Setting | Default | Effect |
|---------|---------|--------|
| `medexpertmatch.llm.structured-output.provider-native-enabled` | `false` | When `true` and endpoint is not local Ollama, adds `useProviderStructuredOutput()` per call |

Metrics: `llm.structured-output.validation.retry` and `.failure` (see [chat-ops-runbook.md](chat-ops-runbook.md)). Prefer keeping provider-native off for local MedGemma/Ollama (RISK-143).

---

## References

- [Ollama Cloud models](https://ollama.com/search?c=cloud) — catalog (DeepSeek V4, Qwen3.5, Gemma 4, Nemotron, etc.)
- [AI Provider Configuration](AI_PROVIDER_CONFIGURATION.md) — env vars and fallback chains
- [M64 ADR](decisions/M64-cost-quality-tier-routing.md) — tier architecture
- [FunctionGemma](FUNCTIONGEMMA.md) — tool-calling constraints
- [MedGemma Configuration](MEDGEMMA_CONFIGURATION.md) — MedGemma-specific deployment
