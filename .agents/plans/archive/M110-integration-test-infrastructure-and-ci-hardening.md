# M110: Integration Test Infrastructure and CI Hardening

**Status:** Active (planned 2026-06-13)
**Created:** 2026-06-13
**Depends on:** M109 (archived)

## Problem Statement

1. **Integration tests fail locally** — `medexpertmatch-postgres-test:latest` Docker image must be built manually via `./scripts/build-test-container.sh`. New contributors hit this immediately.
2. **No automated test image build** — the CI workflow uses `./scripts/ensure-test-container.sh` but the image must exist first.
3. **`bun.lock` and `package.json` artifacts** — these untracked files appear on every branch and are gitignored, but the `.gitignore` entry was only added on `develop`, not `main`.

## Goal

1. Add `make test-image` target (already done in M106)
2. Document test image build in DEVELOPMENT_GUIDE.md
3. Ensure `.gitignore` is consistent across branches
4. `mvn verify` green

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Add test image build to CI workflow or Makefile | Complete |
| 2 | Document test image build in DEVELOPMENT_GUIDE.md | Complete |
| 3 | Ensure `.gitignore` consistency | Complete |
| 4 | `mvn verify` green | Complete |
| 5 | Archive plan | Complete |

## References

- `./scripts/build-test-container.sh`
- `.github/workflows/ci.yml`
- `docs/DEVELOPMENT_GUIDE.md`
- `.gitignore`
