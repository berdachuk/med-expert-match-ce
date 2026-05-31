# M29: Agent Tools Split & Orchestrator Hardening

## Goal

Split monolithic `MedicalAgentTools` into focused components, harden chat/orchestrator tool-calling,
externalize LLM prompts as Spring AI resources, and add verification scripts.

## Completed

- [x] Split `MedicalAgentTools` into 6 `@Component` classes + `support/` utilities
- [x] `AgentToolCallingConfiguration`, `NormalizingToolCallbackResolver`, `AgentToolNameNormalizer`
- [x] `MatchToolParameterSanitizer` for poisoned `preferredSpecialties` / telehealth args
- [x] `CaseIdExtractor`, `ChatCasePromptSupport`, chat case-ID hints
- [x] `ChatAssistantServiceImpl` empty-stream sync fallback
- [x] Resource-backed `.st` prompts + `PromptTemplateConfig` beans (llm-prompts skill rule)
- [x] `MedicalAgentLlmSupportServiceImpl` MedGemma null finish_reason retry
- [x] Remove `@Transactional` from `matchFromText` for case visibility
- [x] Verification scripts: `verify-orchestrator.sh`, `verify-specialist-case.sh`, curl helpers
- [x] Unit/wiring tests updated for split tools and prompt templates

## Verification

```bash
mvn test -Dtest=MatchToolParameterSanitizerTest,DoctorMatchingAgentToolsMatchFromTextTest,MedicalAgentMemoryWiringTest,ChatCasePromptSupportTest
./start-stack.sh --build --no-mkdocs
bash scripts/verify-orchestrator.sh
bash scripts/verify-specialist-case.sh
```

## Next

See **`.agents/plans/M28-chat-trust-and-interoperability.md`** for chat trust/interop follow-ups.
