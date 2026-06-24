#!/usr/bin/env bash
# Enrich M01-M110 progress stubs with structured metadata from archived plans
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RECORDS_DIR="$ROOT/.agents/memory-bank/records/progress"
ARCHIVE_DIR="$ROOT/.agents/plans/archive"

# Map milestone number -> enriched content
# Format: modules|reqRefs|decRefs|summary
declare -A ENRICHED

ENRICHED[M01]="core/config,embedding|—|—|Upgraded spring-ai-bom 2.0.0-M2→M6 and spring-ai-agent-utils 0.4.2→0.5.2. Adapted OpenAiEmbeddingModel 4-arg constructor, reviewed all M6 breaking changes."
ENRICHED[M02]="llm,core|—|—|5 improvements: OrchestrationContextHolder, inline prompts→.st files, SessionMemoryAdvisor config, AutoMemory tools, evaluation module stub."
ENRICHED[M03]="documents,chunking,embedding,retrieval,llm|—|—|7 improvements: browser auto-launch, health check caching, structured output parsing, RRF fusion, re-ranking integration, evaluation framework, document ingestion+chunking."
ENRICHED[M04]="documents,chunking,embedding,retrieval,llm|—|—|8 improvements: DB tables for document/chunk modules, repository impls, Modulith boundary fixes, feature flags, document chunk embedding pipeline, DocumentSearchService, test coverage, Modulith verification."
ENRICHED[M05]="chunking,documents,retrieval,web|—|—|4 improvements: Chunker strategy tests, document ingest & embedding pipeline tests, evaluation REST endpoint, DocumentSearchServiceIT."
ENRICHED[M06]="retrieval,documents,web,llm|—|—|7 improvements: expanded evaluation dataset, document search REST API, document search web UI, end-to-end document pipeline IT, complete evaluation REST API, reranking health indicator, resolved llm→web circular dependency."
ENRICHED[M07]="web,core,documents|—|—|6 improvements: web UI integration tests, API rate limiting + input validation, OpenAPI/Swagger docs, production Docker Compose, PDF ingest IT, query optimization."
ENRICHED[M08]="core,ci,monitoring|—|—|6 improvements: structured JSON logging + trace IDs, GitHub Actions CI pipeline, Spring Boot Actuator + Prometheus, Grafana dashboard, RFC 7807 global exception handler, response time SLI logging."
ENRICHED[M09]="core,llm,docs|—|—|6 improvements: SonarQube clean, HikariCP tuning, async LLM job cleanup, README refresh, test coverage, connection leak detection."
ENRICHED[M10]="core,web,scripts|—|—|6 improvements: API key auth (ApiKeyAuthFilter), Caffeine caching, k6 load scripts, rate limit Prometheus metrics, WebSocket job status push, DB seed CLI runner."
ENRICHED[M11]="web,core,test|—|—|E2E acceptance tests for 6 use cases, feature flags for ingestion/GraphRAG/agent skills, Thymeleaf i18n (EN+RU), ARIA accessibility, request body size limits, graceful shutdown."
ENRICHED[M12]="core,web,documents|—|—|API v2 routing + ApiVersionFilter, faceted document search, GZip compression, API usage analytics (ApiUsageInterceptor), Flyway V2 (ApiSessionToken/AuditLog), RELEASE_CHECKLIST.md + CHANGELOG.md."
ENRICHED[M13]="chat,llm|—|—|AI Chat tab, per-user sessions, specialized agents. See archive plan for details."
ENRICHED[M14]="chat,llm|—|—|AI Chat agent routing implementation."
ENRICHED[M15]="chat,llm|—|—|A2A interop, chat streaming, production hardening."
ENRICHED[M16]="chat,llm|—|—|Full A2A integration and M08 closeout."
ENRICHED[M17]="chat,llm|—|—|Chat agent polish and A2A hardening."
ENRICHED[M18]="chat,llm|—|—|Chat production readiness and observability."
ENRICHED[M19]="chat,llm|—|—|Chat Ops, rate limits, browser interop."
ENRICHED[M20]="chat,llm|—|—|Chat governance and A2A contract hardening."
ENRICHED[M21]="chat,llm|—|—|Chat admin and observability."
ENRICHED[M22]="chat,llm|—|—|A2A federation and chat lifecycle."
ENRICHED[M23]="chat,llm|—|—|Chat security and A2A governance."
ENRICHED[M24]="chat,llm|—|—|A2A production readiness."
ENRICHED[M25]="chat,llm|—|—|Chat platform hardening."
ENRICHED[M26]="chat,llm|—|—|Chat federation and compliance."
ENRICHED[M27]="chat,llm|—|—|Chat observability and governance."
ENRICHED[M28]="chat,llm,web|—|—|Agent card JSON Schema + contract validation IT, export bundle OpenAPI \$ref to standalone JSON Schema, admin retention API, retention scheduler health indicator, A2A stream parity contract IT."
ENRICHED[M29]="llm,llm/harness|—|—|Agent tools split, orchestrator hardening, harness engineering improvements (verify, critic, context bundle, tool scope, doctor-match state machine)."
ENRICHED[M30]="llm/harness|—|—|Harness orchestration expansion: CaseIntakeWorkflowEngine, RoutingWorkflowEngine, JdbcAgentPlanArtefactStore, A2A tool scope, eval/ops wiring."
ENRICHED[M31]="llm/harness|—|—|Harness human checkpoint and event handoffs."
ENRICHED[M32]="llm/harness,web|—|—|Harness eval regression and Ops UI."
ENRICHED[M33]="llm/harness,web|—|—|Harness full eval and chain visibility."
ENRICHED[M34]="llm/harness,web|—|—|Live eval baseline updates, Grafana alert on harness verify failure, chain replay API, clinician role UI parity, harness run retention + purge, end-to-end harness IT."
ENRICHED[M35]="chat,llm|—|—|Fix follow-up turn flow: ConversationGoalContext (session-keyed map), GoalClassifier follow-up detection, ChatAssistantServiceImpl turn continuity wiring, integration test for Find Specialist→yes round-trip."
ENRICHED[M36]="chat,llm,core|—|—|Caffeine-backed bounded cache for ConversationGoalContext, DB-persisted goal context (chat_goal_context table + Flyway), follow-up detection edge-case hardening, end-to-end harness IT, JMH microbenchmark."
ENRICHED[M37]="llm/harness,chat,llm|—|—|Per-goal CaseContextIntent (MATCH_DOCTORS→MATCH etc.), harness run retention purge job, harness failure rate Grafana alert, LLM fallback on zero results, streaming harness state timeline to chat UI."
ENRICHED[M38]="llm,core|—|—|Multi-agent event-driven pipeline: domain events (GoalIdentifiedEvent, PlanReadyEvent, ContextReadyEvent, ResultsReadyEvent, DoneEvent), PlannerAgent, ContextBuilderAgent, ExecutionAgent, CriticAgent."
ENRICHED[M39]="chat,llm|—|—|Wire GoalClassifier to publish GoalIdentifiedEvent, GoalIdentifiedEventPublisher, PlannerAgent sync fallback via CompletableFuture, AgentCoordinatorService, @Profile(\"event-driven\") gate."
ENRICHED[M40]="web|—|—|Remove dedicated right panel, move agent activity into inline collapsible panel above chat input. Changes to chat.html, chat.js, chat.css."
ENRICHED[M41]="chat,llm|—|—|5 root causes fixed: stop clearing ConversationGoalContext at end of turn, expand isFollowUpSignal(), inject conversation history into buildUserPrompt, fall back to context in CaseIdExtractor, persist messages to SessionService in stream path."
ENRICHED[M42]="llm,core,web|—|—|Event pipeline metrics (Micrometer counters/timers per agent stage), dead letter event handling, pipeline progress via SSE, inline panel shows pipeline stages, integration test with metrics."
ENRICHED[M43]="llm,core,web|—|—|Secure dead letter admin endpoints (AdminAccessGuard), live pipeline stage tracking (in_progress status), PipelineHealthIndicator (dead letter queue size, failure rates), backend tests."
ENRICHED[M44]="core,llm,chat|—|—|Wire recordStageInProgress into 4 agents, LogStreamService unit tests, ChatFollowUpContextIT integration test, SessionCompactionHealthIndicator test, GraphDatabaseHealthCheck test."
ENRICHED[M45]="docs|—|—|Fix Spring AI version mismatches (M2→M6), FHIR version inconsistency (R4→R5), dead links, outdated deadlines, duplicate content across docs."
ENRICHED[M46]="core,llm,ingestion|—|—|Service refactoring (SyntheticDataGenerator split), startup smoke tests, config property binding tests, doc-sensitive path tests, security defaults review, AI config startup validation, skills directory validation."
ENRICHED[M47]="core,pom.xml|—|—|Bump testcontainers.version from 2.0.3 to 2.0.5. No breaking changes for PostgreSQLContainer or junit-jupiter."
ENRICHED[M48]="core,llm,retrieval|—|—|Extract GeoDistance utility to core/util/, move RerankingServiceImpl hardcoded prompt to external .st file, bump safe dependency versions (minor/patch)."
ENRICHED[M49]="llm,core|—|—|Replace deprecated defaultToolCallbacks with defaultTools, fix AutoMemoryTools NoSuchBeanDefinitionException in test context (@Nullable params), restore full integration test suite."
ENRICHED[M50]="test,llm,web|—|—|Fix 19 remaining IT failures: event-driven profile tests (skills.enabled=true), web test context failures, SQL/baseline issues (missing tables), ModulithVerificationIT."
ENRICHED[M51]="system,evidence,documents,retrieval,ingestion|—|—|Add health indicators (Evidence, PgVector, AgeGraph, EmbeddingPool), improve evidence module coverage (18%→60%+), documents module coverage (8%→50%+), retrieval module coverage (30%→60%+)."
ENRICHED[M52]="evidence,test,pom.xml|—|DEC-010|Add WireMock test dependency, refactor PubMedServiceImpl for injectable RestTemplate, record real PubMed API responses as fixtures, rewrite PubMedServiceIT with WireMock."
ENRICHED[M53]="ingestion,test|—|—|FHIR adapter unit tests (FhirR5DoctorAdapterTest, FhirR5CaseAdapterTest), synthetic data generator edge case tests, overall coverage target >75%."
ENRICHED[M54]="core,system|—|—|Health indicator unit tests (GraphHealthIndicator, DatabaseHealthIndicator, PgVectorHealthIndicator), monitoring coverage, shutdown coverage, overall target >75%."
ENRICHED[M55]="documents,embedding,doctor,test|—|—|Documents REST controller tests (DocumentSearchController, DocumentSearchV2Controller), embedding configuration tests, doctor REST controller tests."
ENRICHED[M56]="chat,llm|—|—|Route MATCH_DOCTORS/ROUTE_CASE in streamMessage() to harness, ChatUserContentSanitizer (strip CoT from pasted Abstract), EmbeddingDescriptionSanitizer, prompt rules, ChatToolContextHolder tool scope."
ENRICHED[M57]="chat,llm|—|—|Session-first rules, English keyword fast path, session-aware LLM fallback, post-classification safety overrides, chat prompt continuity, optional harness route for ANALYZE_CASE."
ENRICHED[M58]="llm,scripts|—|—|Policy eval JSONL, ToolSelectionEvalTest, ToolSelectionGuardingResolver, training data script, live golden eval. GPU fine-tune deferred to M60."
ENRICHED[M59]="chat,llm,web|—|—|Follow-up goal routing (elaboration→ANALYZE_CASE), reasoning split (LlmResponseSanitizer), collapsible UI (<details class=\"llm-thinking\">), case analysis interpretation prompts, flat sidebar list."
ENRICHED[M60]="llm,scripts|—|—|FunctionGemma fine-tune execution. Deferred—resume when GPU capacity available. Live baseline eval, fine-tune run, deploy to tool-calling endpoint, before/after eval, rollback runbook."
ENRICHED[M61]="llm|—|—|Policy layer v2 (confidence router): confidence policy router between retrieval/scoring and response generation. Policy actions: ANSWER, CLARIFY, ESCALATE, REFUSE."
ENRICHED[M62]="llm/eval,scripts|—|—|Unified eval aggregator (EvalFlywheelReport), CLI script (run-eval-flywheel.sh), CI gate for deterministic evals, A/B harness topology, release checklist (RELEASE_GATE.md)."
ENRICHED[M63]="retrieval,llm,core|—|—|MatchOutcome entity + match_outcomes table, POST /api/v1/match-outcomes, historical weight calibration job, GraphQualityHealthIndicator, evidence freshness TTL."
ENRICHED[M64]="llm,chat|—|DEC-012|Cost-quality tier routing: route requests by goal tier (Light/Standard/Full) with token budgets and Prometheus visibility. Light→FunctionGemma, Standard→tools+retrieval, Full→harness engines+GraphRAG."
ENRICHED[M65]="llm/harness,web|—|—|Human-in-the-loop harness: HUMAN_REVIEW checkpoint in harness state machine, admin approve/reject UI, audit trail (llm_harness_adjudication_log), chat blocked until approved, reject→OVERRIDDEN to M63."
ENRICHED[M66]="web,llm,retrieval|—|—|Agent vs chat product packaging: chat mode selector UI (Quick vs Expert), explainability panel (vector/graph/history breakdown), pitch deck sync, case study template, doc cross-links."
ENRICHED[M67]="llm,core|—|DEC-007,DEC-008|LLM role endpoint separation: clinicalChatModel/utilityChatModel beans, LlmClientType.CLINICAL/UTILITY, limiter semaphores, consumer wiring, health indicators, tier max-tokens."
ENRICHED[M68]="llm/harness|—|—|Harness context summarizer: HarnessContextWhitelist, deterministic summarizers (doctor/evidence/routing/generic), wire into harness engines, regression tests, docs."
ENRICHED[M69]="llm/eval|—|—|Adjudication eval flywheel: golden cases (policy-adjudication-cases.jsonl), AdjudicationEvalRunner, 7th family in EvalFlywheelAggregator, ROI note in case study."
ENRICHED[M70]="web,retrieval|—|—|Find specialist explainability panel: reuse MatchExplainabilityService metadata on Find Specialist SSR results. Add per-signal breakdown table (vector/graph/history percentages) to match.html."
ENRICHED[M71]="llm,core,web|—|—|Unified LLM metadata capture (input/output tokens, latency, model, prompt size, cache hits), Micrometer metrics, agent activity panel summary, cache hit distinction."
ENRICHED[M72]="evidence,retrieval,ingestion|—|—|Fix PubMed 0-article query (chiefComplaint was ObjectId), fix 'current transaction is aborted' (missing match_outcomes table), fix Flyway checksum repair gap."
ENRICHED[M73]="ingestion,llm|—|—|Fix synthetic doctor pool (primary specialty, SQL↔graph sync, guaranteed specialist per core specialty), LLM cache hit visibility in telemetry."
ENRICHED[M74]="llm,web|—|—|Strip raw JSON blocks from assistant responses, improve LlmResponseSanitizer.toHumanReadable() to render structured data as prose, fix case-analysis and verify run output."
ENRICHED[M75]="graph,retrieval,ingestion|—|—|Heal REQUIRES_SPECIALTY edges (only 11% of cases had them), add substring/contains specialty name matching in Cypher queries, reconcileCaseSpecialtyGraph()."
ENRICHED[M76]="medicalcase,llm,embedding,caseanalysis,graph|—|—|Move MedicalCaseDescriptionService from medicalcase to llm module to break 4 Modulith cycles. Update synthetic data time estimates, add very-large size (1000 doctors, 20000 cases)."
ENRICHED[M77]="ingestion,core|—|—|Measure actual generation runs (persisted table), expose last-actual via API, auto-adjust estimates in data-sizes.csv, per-phase breakdown, tests."
ENRICHED[M79]="scripts,docs|—|—|Implement Ralph loop infrastructure (bash loop, stories JSON, progress.txt), run smoke test on M77 with --max 1, document in AGENTS.md and ai-context-strategy.md."
ENRICHED[M83]="docs|—|—|Expand §2.2 with Mermaid flow diagram, ten-layer narrative, LlmClientType table, skills table, configuration paragraph. Remove stale §2.1."
ENRICHED[M84]="medicalcase,llm,embedding,caseanalysis,graph|—|—|Move MedicalCaseDescriptionService from medicalcase to llm module. 4 cycles rooted in ChatClient import inside medicalcase."
ENRICHED[M86]="medicalcase,llm,embedding,caseanalysis,graph|—|—|Execution of M84: move MedicalCaseDescriptionService to llm, update 10 import sites (5 main + 5 test), verify ModulithVerificationIT passes."
ENRICHED[M89]="test|—|—|Run mvn verify after M86, fix remaining failures, establish green CI baseline. Result: 544 tests, 0 failures, ModulithVerificationIT green."
ENRICHED[M90]="ingestion,core|—|—|Implement 10 M77 stories: SyntheticDataGenerationRunRepository, Flyway migration, run tracking, EstimateAdjustmentService, @Scheduled CSV writer, admin API, tests."
ENRICHED[M91]="ingestion,llm|—|—|Wire clinicalExperienceMs recording, remove duplicate @EnableScheduling from HarnessConfiguration, add unit test."
ENRICHED[M92]="llm,documents,chunking|—|—|Wire SessionMemoryAdvisor with TurnCountTrigger+TokenCountTrigger, build chunk embedding pipeline, implement DocumentSearchService with PgVector, wire local doc search into evidence-retriever skill."
ENRICHED[M93]="documents,llm,core|—|—|Add @Scheduled cron for NULL embedding backfill, ChunkRepositoryIT, security review of agent tool surface, dependency freshness, multi-module test-coverage closure."
ENRICHED[M94]="llm,web,core|—|—|Add caseId validation to risk_assessment(), fix DocumentSearchController error message disclosure, sanitize tool-call logs, add security posture for document search REST endpoints."
ENRICHED[M95]="llm,ingestion|—|—|Audit case analysis interpretation prompt, add explicit output format, reduce prompt token count, add ICD-10 fallback/validation, parallelize synthetic data description generation."
ENRICHED[M96]="chat,llm,web|—|—|Remove Quick question chat mode, fix LLM response sanitizer (strip echo/headers/JSON blocks), add Russian route-case keywords, add 'find case information' to ANALYZE_CASE patterns."
ENRICHED[M97]="documents,llm,core|—|—|Document backfill config properties, admin endpoint for on-demand backfill, remove @Deprecated primaryChatModel() and LlmClientType.CHAT, extract inline prompts to .st files."
ENRICHED[M98]="core,web|—|—|Enable auth by default (medexpertmatch.auth.enabled=true), add @PreAuthorize to document endpoints, add AdminAccessGuard to backfill endpoint, update LocalSecurityConfig and DockerSecurityConfig to deny-by-default."
ENRICHED[M99]="ingestion,retrieval,medicalcase|—|—|Assign random coordinates to synthetic cases, fix validateGeographicFilteringSupport() to degrade gracefully instead of throwing, verify Haversine pipeline produces differentiated scores."
ENRICHED[M100]="core,docs|—|—|Sync main with develop, verify DockerSecurityConfig parity, clean up stale remote feature branches, archive M98 plan."
ENRICHED[M101]="documents,llm|—|—|Wire search_local_documents into recommendation-engine skill, add startup trigger for embedding backfill, remove lingering references to deleted config files."
ENRICHED[M102]="web,chat,llm|—|—|Add per-signal breakdown to Find Specialist results, harden chat context across session compaction, wire AskUserQuestionTool into case intake workflow."
ENRICHED[M103]="docs,pom.xml|—|—|Remove dead config file references from docs, update docs with current API names, check dependency versions."
ENRICHED[M104]="docs|—|—|Replace LlmClientType.CHAT→CLINICAL and primaryChatModel→clinicalChatModel in docs, fix dead config file references."
ENRICHED[M105]="pom.xml,docs|—|—|Check pom.xml dependency versions, remove remaining dead config file references from docs, final documentation consistency pass."
ENRICHED[M106]="scripts,docs|—|—|Add test image build to CI/Makefile, document in DEVELOPMENT_GUIDE.md, ensure .gitignore consistency across branches."
ENRICHED[M107]="pom.xml,docs|—|—|Check pom.xml dependency versions, remove remaining dead config file references from docs, final documentation consistency pass."
ENRICHED[M109]="docs|—|—|Review remaining improvement areas, define next set of milestones. All M01-M108 complete, 885 tests green."
ENRICHED[M110]="scripts,docs,ci|—|—|Add test image build to CI/Makefile, document in DEVELOPMENT_GUIDE.md, ensure .gitignore consistency. All phases complete."

# Enrich each stub
for num in $(seq -w 1 110); do
  # Remove leading zeros for the milestone number
  mnum=$((10#$num))
  key="M$(printf '%02d' $mnum)"
  # Try zero-padded and non-padded
  for try in "M$(printf '%02d' $mnum)" "M$mnum"; do
    if [[ -f "$RECORDS_DIR/$try.md" ]]; then
      file="$RECORDS_DIR/$try.md"
      break
    fi
  done
  if [[ -z "${file:-}" ]]; then
    continue
  fi

  content="${ENRICHED[$try]:-}"
  if [[ -z "$content" ]]; then
    continue
  fi

  IFS='|' read -r modules reqRefs decRefs summary <<< "$content"

  # Read existing file to get title
  title_line=$(head -1 "$file")
  title="${title_line#\# }"

  cat > "$file" <<EOF
# $title

- **Milestone:** $try
- **Modules:** $modules
- **REQ:** $reqRefs
- **DEC:** $decRefs
- **Date completed:** unknown
- **Status:** Archived (legacy plan)

## Summary

$summary

## Note

Enriched by M135 memory-bank enrichment. See \`.agents/plans/archive/\` for the full plan.
EOF

  echo "Enriched $try"
  file=""
done

echo "Done enriching M01-M110 stubs"
