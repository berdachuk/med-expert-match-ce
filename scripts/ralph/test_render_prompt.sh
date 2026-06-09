#!/usr/bin/env bash
# Sanity tests for scripts/ralph/render_prompt.sh
# Each test should exit non-zero if render_prompt.sh behaves incorrectly.
# Run: bash scripts/ralph/test_render_prompt.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
RENDER="$REPO_ROOT/scripts/ralph/render_prompt.sh"

if [ ! -f "$RENDER" ]; then
    echo "SKIP: $RENDER does not exist yet (TDD: test before implementation)"
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

STORIES="$REPO_ROOT/.agents/plans/M77-stories.json"
if [ ! -f "$STORIES" ]; then
    echo "SKIP: $STORIES missing"
    exit 77
fi

# Pick a real story with a non-empty title/accept/skills.
# M77-02 has all of: title, 4 accept bullets, 3 skills.
STORY_ID="M77-02"
TITLE=$(jq -r --arg id "$STORY_ID" '.stories[] | select(.id == $id) | .title' "$STORIES")
ACCEPT_LINE_1=$(jq -r --arg id "$STORY_ID" '.stories[] | select(.id == $id) | .accept[0]' "$STORIES")
ACCEPT_LINE_2=$(jq -r --arg id "$STORY_ID" '.stories[] | select(.id == $id) | .accept[1]' "$STORIES")
SKILL_1=$(jq -r --arg id "$STORY_ID" '.stories[] | select(.id == $id) | .skills_to_load[0]' "$STORIES")
SKILL_2=$(jq -r --arg id "$STORY_ID" '.stories[] | select(.id == $id) | .skills_to_load[1]' "$STORIES")
SKILL_3=$(jq -r --arg id "$STORY_ID" '.stories[] | select(.id == $id) | .skills_to_load[2]' "$STORIES")

OUTPUT=$("$RENDER" "$STORY_ID" 2>&1) || {
    echo "FAIL: render_prompt.sh $STORY_ID exited non-zero (rc=$?)"
    echo "Output was: $OUTPUT"
    exit 1
}

# Assertion 1: output contains the story title verbatim
check "rendered output contains the story title" \
    bash -c "printf %s \"\$1\" | grep -Fq -- \"$TITLE\"" _ "$OUTPUT"

# Assertion 2: output contains the first two accept bullets verbatim
check "rendered output contains first accept bullet" \
    bash -c "printf %s \"\$1\" | grep -Fq -- \"$ACCEPT_LINE_1\"" _ "$OUTPUT"
check "rendered output contains second accept bullet" \
    bash -c "printf %s \"\$1\" | grep -Fq -- \"$ACCEPT_LINE_2\"" _ "$OUTPUT"

# Assertion 3: output contains all three skills_to_load names
check "rendered output contains skill[0]" \
    bash -c "printf %s \"\$1\" | grep -Fq -- \"$SKILL_1\"" _ "$OUTPUT"
check "rendered output contains skill[1]" \
    bash -c "printf %s \"\$1\" | grep -Fq -- \"$SKILL_2\"" _ "$OUTPUT"
check "rendered output contains skill[2]" \
    bash -c "printf %s \"\$1\" | grep -Fq -- \"$SKILL_3\"" _ "$OUTPUT"

echo
echo "Results: $pass passed, $fail failed"
exit $fail
