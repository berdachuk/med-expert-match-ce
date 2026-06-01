# M33: Harness Full Eval & Chain Visibility

Extends M32 with live LLM eval regression in CI and operator visibility into event chains.

**Prerequisite:** M32 complete (see `.agents/plans/archive/M32-harness-eval-and-ops-ui.md`).

## Scope

| # | Deliverable | Branch | Status | Effort |
|---|-------------|--------|--------|--------|
| 1 | Full `EvaluationService` pass-rate IT with mocked LLM | `feat/m33-eval-full-it` | ⬜ Planned | 10h |
| 2 | CI optional job: run eval IT + update baseline on green | `feat/m33-eval-ci` | ⬜ Planned | 6h |
| 3 | Admin UI: event chain trace (analysis→match→recommend) | `feat/m33-chain-trace-ui` | ⬜ Planned | 8h |
| 4 | REST list endpoint for harness runs (OpenAPI) | `feat/m33-harness-runs-api` | ⬜ Planned | 4h |
| 5 | Playwright smoke for harness-runs admin page | `feat/m33-harness-playwright` | ⬜ Planned | 4h |
| 6 | Harness backlog auto-link from run failure reason | `feat/m33-backlog-link` | ⬜ Planned | 3h |

**Total effort: ~35h**

---

## Step 1: Full eval IT

- Mock `MedicalAgentService` in IT to return ground-truth-aligned responses
- Assert `normalized_accuracy` ≥ baseline − 5% via `EvaluationReportParser`

---

## References

- M32 archive: `.agents/plans/archive/M32-harness-eval-and-ops-ui.md`
- `EvaluationService.java`, `HarnessRunsWebController.java`
