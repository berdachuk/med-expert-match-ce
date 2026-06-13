# M106: Local Dev Experience and Test Infrastructure

**Status:** Active (planned 2026-06-13)
**Created:** 2026-06-13
**Depends on:** M105 (archived)

## Problem Statement

1. **Integration tests fail locally** — `medexpertmatch-postgres-test:latest` Docker image must be built manually via `./scripts/build-test-container.sh`. New contributors hit this immediately.
2. **No automated test image build** — the CI workflow doesn't build the test image; it relies on a pre-built image.
3. **`bun.lock` and `package.json` artifacts** — these untracked files appear on every branch and are gitignored, but the `.gitignore` entry was only added on `develop`, not `main`.

## Goal

1. Add `make test-image` or script that auto-builds the test container
2. Document the test image build in the development guide
3. Ensure `.gitignore` is consistent across branches
4. `mvn verify` green

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Add test image build to CI workflow or Makefile | Pending |
| 2 | Document test image build in DEVELOPMENT_GUIDE.md | Pending |
| 3 | Ensure `.gitignore` consistency | Pending |
| 4 | `mvn verify` green | Pending |
| 5 | Archive plan | Pending |

## References

- `./scripts/build-test-container.sh`
- `.github/workflows/ci.yml`
- `docs/DEVELOPMENT_GUIDE.md`
- `.gitignore`
