#!/usr/bin/env bash
# Sanity checks for scripts/ralph.sh
# Each test should exit non-zero if ralph.sh behaves incorrectly.
# Run: bash scripts/ralph/test_ralph.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
RALPH="$REPO_ROOT/scripts/ralph.sh"

if [ ! -f "$RALPH" ]; then
    echo "SKIP: $RALPH does not exist yet (TDD: test before implementation)"
    exit 77
fi

fail=0
pass=0

check() {
    local desc="$1"
    shift
    if "$@"; then
        echo "PASS: $desc"
        pass=$((pass + 1))
    else
        echo "FAIL: $desc"
        fail=$((fail + 1))
    fi
}

# Test 1: no args -> non-zero
check "ralph.sh with no args exits non-zero" bash -c "! '$RALPH' >/dev/null 2>&1"

# Test 2: missing stories.json -> non-zero
TMPDIR_NO_STORIES="$(mktemp -d)"
check "ralph.sh with missing stories.json exits non-zero" \
    bash -c "! '$RALPH' M99 >/dev/null 2>&1"
rm -rf "$TMPDIR_NO_STORIES"

# Test 3: --max 0 -> non-zero
TMPDIR_MAX0="$(mktemp -d)"
mkdir -p "$TMPDIR_MAX0/.agents/plans"
echo '{"milestone":"M99","stories":[]}' > "$TMPDIR_MAX0/.agents/plans/M99-stories.json"
check "ralph.sh with --max 0 exits non-zero" \
    bash -c "cd '$TMPDIR_MAX0' && ! '$RALPH' M99 --max 0 >/dev/null 2>&1"
rm -rf "$TMPDIR_MAX0"

# Test 4: --dry-run with real M77-stories.json picks M77-01 and exits 0
if [ -f "$REPO_ROOT/.agents/plans/M77-stories.json" ]; then
    check "ralph.sh M77 --dry-run picks M77-01 and exits 0" \
        bash -c "cd '$REPO_ROOT' && '$RALPH' M77 --dry-run 2>&1 | grep -q 'picked=M77-01'"
else
    echo "SKIP: real M77-stories.json not present (test only runs in repos that have it)"
fi

# Test 5: --max 1 --agent openai without OPENAI_API_KEY exits 4 (M80: auth/missing-env)
if [ -f "$REPO_ROOT/.agents/plans/M77-stories.json" ]; then
    check "ralph.sh M77 --max 1 --agent openai without OPENAI_API_KEY exits 4" \
        env -u OPENAI_API_KEY -u OPENAI_BASE_URL -u OPENAI_MODEL \
            bash -c "cd '$REPO_ROOT' && '$RALPH' M77 --max 1 --agent openai >/dev/null 2>&1; [ \$? -eq 4 ]"
else
    echo "SKIP: M77-stories.json not present (test 5 only runs in repos that have it)"
fi

# Test 6: --max-time 0 exits 1 (M81: validation rejects 0, same as --max 0)
check "ralph.sh with --max-time 0 exits 1 (validation)" \
    bash -c "! '$RALPH' M77 --max-time 0 >/dev/null 2>&1"

# Test 7: --max-consecutive-failures 0 exits 1 (bad arg, like --max 0)
check "ralph.sh with --max-consecutive-failures 0 exits 1" \
    bash -c "! '$RALPH' M77 --max-consecutive-failures 0 >/dev/null 2>&1"

# Test 8: --max-consecutive-failures N enables continue-on-red.
# With --max 1 and an agent that fails, the loop should NOT exit 4
# (the agent's rc) but should continue and exit 0 because --max 1 was
# reached without a green commit. The [RED-AGENT] block is in progress.txt.
# Note: this test mutates progress.txt (writes a [RED-AGENT] block). It is
# the last test that runs, so the file ends with one new block from it.
check "ralph.sh --max 1 --max-consecutive-failures 3 --agent openai (no key) exits 0 (continue-on-red)" \
    env -u OPENAI_API_KEY -u OPENAI_BASE_URL -u OPENAI_MODEL \
        bash -c "cd '$REPO_ROOT' && '$RALPH' M77 --max 1 --max-consecutive-failures 3 --agent openai >/dev/null 2>&1; [ \$? -eq 0 ]"

echo
echo "Results: $pass passed, $fail failed"
exit $fail
