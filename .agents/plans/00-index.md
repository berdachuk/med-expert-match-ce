# Milestone Plan Index

## Active

| # | Plan | Description |
|---|------|-------------|
| M94 | [`M94-security-hardening-and-tool-surface-closeout.md`](M94-security-hardening-and-tool-surface-closeout.md) | Security hardening: risk_assessment caseId validation, fix error disclosure in DocumentSearchController, sanitize tool log messages, explicit REST endpoint security posture |

## Deferred

Postponed indefinitely; repo scaffolding from M58 remains. Resume only when GPU fine-tune capacity is available and a stakeholder confirms the fine-tune is worth the engineering cost vs. the server-side `ToolSelectionGuardingResolver` (M57) which already covers the production gap.

| # | Plan | Description |
|---|------|-------------|
| M60 | [`M60-functiongemma-finetune-execution-next.md`](M60-functiongemma-finetune-execution-next.md) | GPU fine-tune execution + live before/after eval (postponed 2026-06-09) |
| — | [`M60-functiongemma-finetune-execution.md`](M60-functiongemma-finetune-execution.md) | Superseded by `M60-functiongemma-finetune-execution-next.md` (same scope, refreshed 2026-06-08) |

## Archive

| # | Plan | Description |
|---|------|-------------|
| M01 | [`M01-upgrade-spring-ai-to-m6.md`](archive/M01-upgrade-spring-ai-to-m6.md) | Upgrade Spring AI to M6 |
| M02 | [`M02-agentic-orchestration-improvements.md`](archive/M02-agentic-orchestration-improvements.md) | Agentic orchestration improvements |
| M03 | [`M03-retrieval-and-evaluation-improvements.md`](archive/M03-retrieval-and-evaluation-improvements.md) | Retrieval and evaluation improvements |
| M04 | [`M04-production-hardening.md`](archive/M04-production-hardening.md) | Production hardening |
| M05 | [`M05-remaining-quality-gaps.md`](archive/M05-remaining-quality-gaps.md) | Remaining quality gaps |
| M06 | [`M06-doc-search-api-eval-expansion.md`](archive/M06-doc-search-api-eval-expansion.md) | Doc search API eval expansion |
| M07 | [`M07-security-web-ui-prod-readiness.md`](archive/M07-security-web-ui-prod-readiness.md) | Security, web UI, prod readiness |
| M08 | [`M08-agentic-patterns-improvements.md`](archive/M08-agentic-patterns-improvements.md) | Agentic patterns improvements |
| M08 | [`M08-observability-ci-cd-monitoring.md`](archive/M08-observability-ci-cd-monitoring.md) | Observability CI/CD monitoring |
| M09 | [`M09-code-quality-performance-docs.md`](archive/M09-code-quality-performance-docs.md) | Code quality, performance, docs |
| M10 | [`M10-auth-caching-load-testing.md`](archive/M10-auth-caching-load-testing.md) | Auth, caching, load testing |
| M11 | [`M11-e2e-tests-feature-flags-ui-polish.md`](archive/M11-e2e-tests-feature-flags-ui-polish.md) | E2E tests, feature flags, UI polish |
| M12 | [`M12-api-versioning-search-release.md`](archive/M12-api-versioning-search-release.md) | API versioning, search, release |
| M13 | [`M13-ai-chat-tab-and-specialized-agents.md`](archive/M13-ai-chat-tab-and-specialized-agents.md) | AI chat tab and specialized agents |
| M13 | [`M13-ai-chat-tab-phase-a.md`](archive/M13-ai-chat-tab-phase-a.md) | AI chat tab phase A |
| M14 | [`M14-ai-chat-agent-routing.md`](archive/M14-ai-chat-agent-routing.md) | AI chat agent routing |
| M15 | [`M15-a2a-streaming-hardening.md`](archive/M15-a2a-streaming-hardening.md) | A2A streaming hardening |
| M16 | [`M16-a2a-full-integration-and-m08-closeout.md`](archive/M16-a2a-full-integration-and-m08-closeout.md) | A2A full integration |
| M17 | [`M17-chat-agent-polish-and-a2a-hardening.md`](archive/M17-chat-agent-polish-and-a2a-hardening.md) | Chat agent polish, A2A hardening |
| M18 | [`M18-chat-production-readiness.md`](archive/M18-chat-production-readiness.md) | Chat production readiness |
| M19 | [`M19-chat-ops-and-interop.md`](archive/M19-chat-ops-and-interop.md) | Chat ops and interop |
| M20 | [`M20-chat-governance-and-a2a-contracts.md`](archive/M20-chat-governance-and-a2a-contracts.md) | Chat governance, A2A contracts |
| M21 | [`M21-chat-admin-and-observability.md`](archive/M21-chat-admin-and-observability.md) | Chat admin and observability |
| M22 | [`M22-a2a-federation-and-chat-lifecycle.md`](archive/M22-a2a-federation-and-chat-lifecycle.md) | A2A federation, chat lifecycle |
| M23 | [`M23-chat-security-and-a2a-governance.md`](archive/M23-chat-security-and-a2a-governance.md) | Chat security, A2A governance |
| M24 | [`M24-a2a-production-readiness.md`](archive/M24-a2a-production-readiness.md) | A2A production readiness |
| M25 | [`M25-chat-platform-hardening.md`](archive/M25-chat-platform-hardening.md) | Chat platform hardening |
| M26 | [`M26-chat-federation-and-compliance.md`](archive/M26-chat-federation-and-compliance.md) | Chat federation and compliance |
| M27 | [`M27-chat-observability-and-governance.md`](archive/M27-chat-observability-and-governance.md) | Chat observability and governance |
| M28 | [`M28-chat-trust-and-interoperability.md`](archive/M28-chat-trust-and-interoperability.md) | Chat trust and interoperability |
| M29 | [`M29-agent-tools-split-and-orchestrator-hardening.md`](archive/M29-agent-tools-split-and-orchestrator-hardening.md) | Agent tools split, orchestrator hardening |
| M29 | [`M29-harness-engineering-improvements.md`](archive/M29-harness-engineering-improvements.md) | Harness engineering improvements |
| M30 | [`M30-harness-orchestration-expansion.md`](archive/M30-harness-orchestration-expansion.md) | Harness orchestration expansion |
| M31 | [`M31-harness-human-checkpoint-and-events.md`](archive/M31-harness-human-checkpoint-and-events.md) | Harness human checkpoint and events |
| M32 | [`M32-harness-eval-and-ops-ui.md`](archive/M32-harness-eval-and-ops-ui.md) | Harness eval and ops UI |
| M33 | [`M33-harness-full-eval-and-chain-ui.md`](archive/M33-harness-full-eval-and-chain-ui.md) | Harness full eval and chain UI |
| M34 | [`M34-harness-production-readiness.md`](archive/M34-harness-production-readiness.md) | Harness production readiness |
| M35 | [`M35-chat-conversation-context-turn-continuity.md`](archive/M35-chat-conversation-context-turn-continuity.md) | Chat conversation context turn continuity |
| M36 | [`M36-chat-context-hardening.md`](archive/M36-chat-context-hardening.md) | Chat context hardening |
| M37 | [`M37-harness-production-hardening.md`](archive/M37-harness-production-hardening.md) | Harness production hardiness |
| M38 | [`M38-multi-agent-event-pipeline.md`](archive/M38-multi-agent-event-pipeline.md) | Multi-agent event pipeline |
| M39 | [`M39-wire-event-agents-into-chat.md`](archive/M39-wire-event-agents-into-chat.md) | Wire event agents into chat |
| M40 | [`M40-inline-agent-activity-panel.md`](archive/M40-inline-agent-activity-panel.md) | Inline agent activity panel |
| M41 | [`M41-fix-chat-follow-up-context-loss.md`](archive/M41-fix-chat-follow-up-context-loss.md) | Fix chat follow-up context loss |
| M42 | [`M42-event-pipeline-observability-recovery.md`](archive/M42-event-pipeline-observability-recovery.md) | Event pipeline observability recovery |
| M43 | [`M43-pipeline-security-health-live-tracking.md`](archive/M43-pipeline-security-health-live-tracking.md) | Pipeline security, health, live tracking |
| M44 | [`M44-core-infrastructure-test-coverage.md`](archive/M44-core-infrastructure-test-coverage.md) | Core infrastructure test coverage |
| M45 | [`M45-doc-inconsistency-fixes.md`](archive/M45-doc-inconsistency-fixes.md) | Doc inconsistency fixes |
| M46 | [`M46-fix-plan-finalization.md`](archive/M46-fix-plan-finalization.md) | Fix plan finalization |
| M47 | [`M47-upgrade-testcontainers-to-2.0.5.md`](archive/M47-upgrade-testcontainers-to-2.0.5.md) | Upgrade Testcontainers to 2.0.5 |
| M48 | [`M48-code-quality-and-dependency-freshness.md`](archive/M48-code-quality-and-dependency-freshness.md) | Code quality and dependency freshness |
| M49 | [`M49-spring-ai-m8-api-migration.md`](archive/M49-spring-ai-m8-api-migration.md) | Spring AI M8 API migration |
| M50 | [`M50-fix-remaining-test-failures.md`](archive/M50-fix-remaining-test-failures.md) | Fix remaining test failures |
| M51 | [`M51-production-hardening-and-coverage.md`](archive/M51-production-hardening-and-coverage.md) | Production hardening and coverage |
| M52 | [`M52-mock-external-services.md`](archive/M52-mock-external-services.md) | Mock external services |
| M53 | [`M53-ingestion-coverage.md`](archive/M53-ingestion-coverage.md) | Ingestion coverage |
| M54 | [`M54-system-health-monitoring-coverage.md`](archive/M54-system-health-monitoring-coverage.md) | System health monitoring coverage |
| M55 | [`M55-rest-controller-config-coverage.md`](archive/M55-rest-controller-config-coverage.md) | REST controller config coverage |
| M56 | [`M56-chat-auto-find-specialist-fix.md`](archive/M56-chat-auto-find-specialist-fix.md) | Chat Auto Find Specialist harness routing and CoT sanitization |
| M57 | [`M57-goal-classifier-hybrid-session-routing.md`](archive/M57-goal-classifier-hybrid-session-routing.md) | Hybrid GoalClassifier: session rules + LLM context + chat continuity + PolicyGate rename |
| M58 | [`M58-functiongemma-tool-calling-finetune.md`](archive/M58-functiongemma-tool-calling-finetune.md) | FunctionGemma tool-selection policy, live eval pipeline, Unsloth export (GPU train → M60) |
| M59 | [`M59-chat-collapsible-reasoning-and-follow-up.md`](archive/M59-chat-collapsible-reasoning-and-follow-up.md) | Collapsible CoT in chat, follow-up routing, case-analysis interpretation, chat UI |
| M61 | [`M61-policy-layer-v2.md`](archive/M61-policy-layer-v2.md) | Confidence policy router: ANSWER / CLARIFY / ESCALATE / REFUSE |
| M62 | [`M62-eval-flywheel-release-gate.md`](archive/M62-eval-flywheel-release-gate.md) | Unified eval flywheel report + ROI release gate |
| M63 | [`M63-match-outcome-data-flywheel.md`](archive/M63-match-outcome-data-flywheel.md) | Match outcome labels + historical weight calibration |
| M65 | [`M65-human-in-the-loop-harness.md`](archive/M65-human-in-the-loop-harness.md) | HUMAN_REVIEW checkpoint, adjudication audit, M63 override hook |
| M66 | [`M66-agent-vs-chat-packaging.md`](archive/M66-agent-vs-chat-packaging.md) | Chat mode selector, match explainability, pitch/case study |
| M69 | [`M69-adjudication-eval-flywheel.md`](archive/M69-adjudication-eval-flywheel.md) | Adjudication eval family + release gate (7th flywheel family) |
| M64 | [`M64-cost-quality-routing.md`](archive/M64-cost-quality-routing.md) | Cost-quality tier routing (LIGHT/STANDARD/FULL), token budgets, Prometheus metrics |
| M67 | [`M67-llm-role-endpoint-separation.md`](archive/M67-llm-role-endpoint-separation.md) | Clinical + utility LLM endpoints (`CLINICAL_*`, `UTILITY_*`); M64 ADR Phase 2 |
| M68 | [`M68-harness-context-summarizer.md`](archive/M68-harness-context-summarizer.md) | Harness context summarizer before clinical LLM (M64 ADR Phase 3) |
| M70 | [`M70-find-specialist-explainability.md`](archive/M70-find-specialist-explainability.md) | Find Specialist explainability panel (SSR + AJAX) |
| M71 | [`M71-llm-usage-telemetry.md`](archive/M71-llm-usage-telemetry.md) | LLM usage metadata capture + compact summary in agent activity panel |
| M72 | [`M72-find-specialist-data-quality-fixes.md`](archive/M72-find-specialist-data-quality-fixes.md) | PubMed ObjectId guard, LLM session ID in advisor context, circular dep fix, missing `match_outcomes` tables, match validation with extended context, file-backed logs |
| M73 | [`M73-synthetic-data-quality-and-llm-cache-logging.md`](archive/M73-synthetic-data-quality-and-llm-cache-logging.md) | Synthetic data quality: `MAJOR_SPECIALTIES` round-robin, `reconcileSpecialtyGraph()` (idempotent MERGE), admin endpoint `POST /api/v1/admin/synthetic-data/reconcile-specialties`, unconditional `INFO` LLM-usage log with `cache_hit=true\|false` |
| M74 | [`M74-human-readable-response-rendering.md`](archive/M74-human-readable-response-rendering.md) | Render LLM JSON blocks embedded in narrative as human-readable prose, **UI path only** |
| M75 | [`M75-find-specialist-case-specialty-reconcile.md`](archive/M75-find-specialist-case-specialty-reconcile.md) | `reconcileCaseSpecialtyGraph()` + bidirectional substring match |
| M76 | [`M76-update-data-sizes-and-add-very-large.md`](archive/M76-update-data-sizes-and-add-very-large.md) | Fix wrong time estimates in `data-sizes.csv`, add `very_large` size |
| M77 | [`M77-runtime-measured-estimates.md`](archive/M77-runtime-measured-estimates.md) | Measure every actual run; persist in `synthetic_data_generation_runs` table; surface "Last actual" in admin UI; nightly job auto-adjusts estimates |
| M83 | [`M83-harness-section-2-2-depth.md`](archive/M83-harness-section-2-2-depth.md) | Add depth to `HARNESS_AND_AGENT_USAGE` §2.2 |
| M84 | [`M84-resolve-modulith-cycle.md`](archive/M84-resolve-modulith-cycle.md) | Resolve the pre-existing ModulithVerificationIT cycle |
| M86 | [`M86-implement-m84-modulith-cycle.md`](archive/M86-implement-m84-modulith-cycle.md) | Execute the M84 spec |
| M90 | [`M90-implement-m77-feature.md`](archive/M90-implement-m77-feature.md) | Implement M77 runtime-measured synthetic data estimates (consolidated M81+M82) |
| M91 | [`M91-m77-fixes-and-session-compaction.md`](archive/M91-m77-fixes-and-session-compaction.md) | Fix clinicalExperienceMs tracking gap, remove duplicate @EnableScheduling |
| M92 | [`M92-session-compaction-document-rag.md`](archive/M92-session-compaction-document-rag.md) | Wire DocumentSearch into evidence-retriever skill; add chunk NULL embedding backfill; add DocumentSearchServiceTest |

## Creating a New Plan

See root `AGENTS.md` → Plan Files for naming and lifecycle rules.