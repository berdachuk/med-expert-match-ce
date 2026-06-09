#!/usr/bin/env bash
# Ralph-style autonomous iteration loop for MedExpertMatch.
#
# Usage: ./scripts/ralph.sh M{NN} [--max N] [--dry-run]
#
# Reads .agents/plans/M{NN}-stories.json, picks the highest-priority unpassed
# story (lowest priority int where passes == false), invokes the agent (stub
# for now) in a fresh subprocess, runs the story's test_target, on green
# commits with no Co-authored-by trailer, marks passes:true in the JSON, writes
# commit_sha, and appends a block to .agents/plans/progress.txt.
#
# This is the M79 build of the loop. Real agent invocation is a TODO stub
# (see invoke_agent below). The M80 pilot will wire the LLM API in.

set -euo pipefail

usage() {
    cat <<EOF
Usage: $(basename "$0") M{NN} [--max N] [--agent stub|openai|<path>] [--max-time SECONDS] [--max-consecutive-failures N] [--dry-run]

Arguments:
  M{NN}          Milestone id (e.g. M77). Reads .agents/plans/M{NN}-stories.json.

Options:
  --max N                       Run at most N stories (default: 999999 = run until done).
  --agent MODE                  Agent mode: stub (default, human-driven), openai (call the
                                OpenAI-compatible chat endpoint with env vars OPENAI_API_KEY,
                                OPENAI_BASE_URL, OPENAI_MODEL), or a path to an external
                                script that reads a story id and prints a patch on stdout.
  --max-time SECONDS            Wall-clock cap. If elapsed time exceeds this, append a
                                [RED-TIMEOUT] block to progress.txt and exit 8. Default: unset.
  --max-consecutive-failures N  If N stories in a row fail with the same exit class (4/5/6/3),
                                append [RED-LOOP-GIVEUP] and exit 7. Default: 3.
  --dry-run                     Print the chosen story and exit without invoking agent/tests.
  -h, --help                    Show this help.

Exit codes:
  0   All stories passed (or --max reached without error).
  1   Bad usage.
  2   Missing stories.json or repo state.
  3   Test failure on the chosen story.
  4   Agent invocation failure (missing env, non-200, empty response, etc.).
  5   Agent returned no parseable patch.
  6   Patch did not apply cleanly.
  7   Loop gave up: N consecutive same-class failures.
  8   Wall-clock cap exceeded.
EOF
}

# ---- arg parsing ----

if [ $# -lt 1 ]; then
    usage
    exit 1
fi

case "${1:-}" in
    -h|--help) usage; exit 0 ;;
esac

MILESTONE="$1"
shift

MAX=999999
DRY_RUN=0
AGENT_MODE="stub"
MAX_TIME=""
MAX_CONSECUTIVE_FAILURES=3
# When --max-consecutive-failures is set, the loop continues past red stories
# instead of exiting on the first one (M81). When unset, exit on first red (M80).
CONTINUE_ON_RED=0
while [ $# -gt 0 ]; do
    case "$1" in
        --max)
            MAX="${2:-}"
            shift 2
            ;;
        --max=*)
            MAX="${1#--max=}"
            shift
            ;;
        --dry-run)
            DRY_RUN=1
            shift
            ;;
        --agent)
            AGENT_MODE="${2:-stub}"
            shift 2
            ;;
        --agent=*)
            AGENT_MODE="${1#--agent=}"
            shift
            ;;
        --max-time)
            MAX_TIME="${2:-}"
            shift 2
            ;;
        --max-time=*)
            MAX_TIME="${1#--max-time=}"
            shift
            ;;
        --max-consecutive-failures)
            MAX_CONSECUTIVE_FAILURES="${2:-}"
            CONTINUE_ON_RED=1
            shift 2
            ;;
        --max-consecutive-failures=*)
            MAX_CONSECUTIVE_FAILURES="${1#--max-consecutive-failures=}"
            CONTINUE_ON_RED=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage
            exit 1
            ;;
    esac
done

if ! [[ "$MAX" =~ ^[0-9]+$ ]] || [ "$MAX" -eq 0 ]; then
    echo "ERROR: --max must be a positive integer (got: '$MAX')" >&2
    exit 1
fi

if [ -n "$MAX_TIME" ] && { ! [[ "$MAX_TIME" =~ ^[0-9]+$ ]] || [ "$MAX_TIME" -eq 0 ]; }; then
    echo "ERROR: --max-time must be a positive integer (got: '$MAX_TIME')" >&2
    exit 1
fi

if ! [[ "$MAX_CONSECUTIVE_FAILURES" =~ ^[0-9]+$ ]] || [ "$MAX_CONSECUTIVE_FAILURES" -eq 0 ]; then
    echo "ERROR: --max-consecutive-failures must be a positive integer (got: '$MAX_CONSECUTIVE_FAILURES')" >&2
    exit 1
fi

# Wall-clock start time for --max-time accounting. Use SECONDS so we get
# monotonic integer seconds since the script started.
LOOP_START_SECONDS="${SECONDS:-0}"

# ---- locate repo and stories ----

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
STORIES_FILE="$REPO_ROOT/.agents/plans/${MILESTONE}-stories.json"
PROGRESS_FILE="$REPO_ROOT/.agents/plans/progress.txt"

if [ ! -f "$STORIES_FILE" ]; then
    echo "ERROR: $STORIES_FILE not found" >&2
    exit 2
fi

if ! jq -e . "$STORIES_FILE" >/dev/null 2>&1; then
    echo "ERROR: $STORIES_FILE is not valid JSON" >&2
    exit 2
fi

# ---- helpers ----

log() {
    printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"
}

pick_next_story() {
    jq -r '
        .stories
        | map(select(.passes == false))
        | sort_by(.priority // 9999)
        | .[0].id // empty
    ' "$STORIES_FILE"
}

get_field() {
    local story_id="$1"
    local field="$2"
    jq -r --arg id "$story_id" --arg f "$field" '
        .stories[] | select(.id == $id) | .[$f] // ""
    ' "$STORIES_FILE"
}

append_progress() {
    local block="$1"
    printf '\n%s\n' "$block" >> "$PROGRESS_FILE"
}

mark_passed() {
    local story_id="$1"
    local commit_sha="$2"
    local tmp
    tmp="$(mktemp)"
    jq --arg id "$story_id" --arg sha "$commit_sha" --arg now "$(date -u +%Y-%m-%dT%H:%M:%SZ)" '
        (.stories[] | select(.id == $id)) |= (.passes = true | .commit_sha = $sha | .finished_at = $now)
    ' "$STORIES_FILE" > "$tmp" && mv "$tmp" "$STORIES_FILE"
}

invoke_agent() {
    # Dispatch on $AGENT_MODE. The default 'stub' is the M79 no-op behavior:
    # a human has already implemented the story in the working tree, the
    # loop just runs the test and commits. M80 adds 'openai' (real LLM call)
    # and arbitrary <path> (custom agent script).
    local story_id="$1"
    case "$AGENT_MODE" in
        stub)
            log "agent stub: assuming $story_id is already implemented in working tree"
            return 0
            ;;
        openai)
            invoke_agent_openai "$story_id"
            ;;
        *)
            # Treat as a path to an external agent script
            if [ -x "$AGENT_MODE" ] || [ -x "$(command -v "$AGENT_MODE" 2>/dev/null || true)" ]; then
                invoke_agent_path "$story_id" "$AGENT_MODE"
            else
                log "ERROR: --agent value '$AGENT_MODE' is not 'stub', 'openai', or an executable path"
                exit 1
            fi
            ;;
    esac
}

invoke_agent_openai() {
    local story_id="$1"
    local prompt
    local response
    local patch_path

    log "agent openai: rendering prompt for $story_id"
    prompt=$("$SCRIPT_DIR/ralph/render_prompt.sh" "$story_id") || {
        log "ERROR: render_prompt.sh failed"
        return 4
    }

    log "agent openai: calling ${OPENAI_BASE_URL:-unset}/chat/completions (model=${OPENAI_MODEL:-unset})"
    response=$(printf '%s' "$prompt" | "$SCRIPT_DIR/ralph/call_openai.sh") || {
        log "ERROR: call_openai.sh failed (rc=$?)"
        return 4
    }

    log "agent openai: extracting patch from response"
    patch_path=$(printf '%s' "$response" | "$SCRIPT_DIR/ralph/extract_patch.sh") || {
        log "ERROR: extract_patch.sh found no ```diff block"
        return 5
    }

    log "agent openai: applying patch from $patch_path"
    "$SCRIPT_DIR/ralph/apply_patch.sh" "$patch_path" || {
        log "ERROR: apply_patch.sh failed (rc=$?)"
        return 6
    }
}

invoke_agent_path() {
    local story_id="$1"
    local agent_path="$2"
    local response
    local patch_path

    log "agent path: invoking $agent_path for $story_id"
    response=$("$agent_path" "$story_id") || {
        log "ERROR: external agent $agent_path failed (rc=$?)"
        return 4
    }
    patch_path=$(printf '%s' "$response" | "$SCRIPT_DIR/ralph/extract_patch.sh") || {
        log "ERROR: extract_patch.sh found no ```diff block in $agent_path output"
        return 5
    }
    "$SCRIPT_DIR/ralph/apply_patch.sh" "$patch_path" || {
        log "ERROR: apply_patch.sh failed (rc=$?)"
        return 6
    }
}

run_test() {
    local test_target="$1"
    if [ -z "$test_target" ]; then
        log "no test_target set; skipping test run"
        return 0
    fi
    if [[ "$test_target" == *.IT ]] || [[ "$test_target" == *IT.java ]]; then
        log "running mvn verify -Dit.test='$test_target'"
        (cd "$REPO_ROOT" && mvn -q verify -Dit.test="$test_target")
    else
        log "running mvn test -Dtest='$test_target'"
        (cd "$REPO_ROOT" && mvn -q test -Dtest="$test_target")
    fi
}

# ---- main loop ----

log "ralph.sh start: milestone=$MILESTONE max=$MAX dry_run=$DRY_RUN agent=$AGENT_MODE max_time=${MAX_TIME:-unset} max_consec_failures=$MAX_CONSECUTIVE_FAILURES"

# Consecutive-failure tracking. last_fail_rc holds the rc of the previous
# failure; consec_count counts how many in a row share that rc.
last_fail_rc=""
consec_count=0

check_wall_clock() {
    if [ -z "$MAX_TIME" ]; then
        return 0
    fi
    local now="${SECONDS:-0}"
    local elapsed=$((now - LOOP_START_SECONDS))
    if [ "$elapsed" -ge "$MAX_TIME" ]; then
        log "wall-clock cap exceeded: elapsed=${elapsed}s >= ${MAX_TIME}s"
        return 1
    fi
    return 0
}

record_failure() {
    local rc="$1"
    if [ "$rc" = "$last_fail_rc" ]; then
        consec_count=$((consec_count + 1))
    else
        last_fail_rc="$rc"
        consec_count=1
    fi
    if [ "$consec_count" -ge "$MAX_CONSECUTIVE_FAILURES" ]; then
        log "consecutive-failure cap: $consec_count stories in a row failed with rc=$rc"
        return 1
    fi
    return 0
}

iter=0
while [ "$iter" -lt "$MAX" ]; do
    iter=$((iter + 1))

    if ! check_wall_clock; then
        append_progress "## $(date -u +%Y-%m-%d) M81-LOOP [RED-TIMEOUT]
- elapsed >= ${MAX_TIME}s wall-clock cap
- iter=$iter story_id=$story_id (about to start)
- Discovered: pilot timed out
- Next: resume with a larger --max-time or fix the bottleneck"
        log "wall-clock cap reached; appended [RED-TIMEOUT] block"
        exit 8
    fi

    story_id="$(pick_next_story)"
    if [ -z "$story_id" ] || [ "$story_id" = "null" ]; then
        log "no unpassed stories remaining; exiting"
        exit 0
    fi

    title="$(get_field "$story_id" title)"
    test_target="$(get_field "$story_id" test_target)"

    log "iter=$iter picked=$story_id title='$title' test_target='$test_target'"

    if [ "$DRY_RUN" -eq 1 ]; then
        log "dry-run: would invoke agent, run '$test_target', commit, mark passes"
        exit 0
    fi

    set +e
    invoke_agent "$story_id"
    agent_rc=$?
    set -e

    if [ "$agent_rc" -ne 0 ]; then
        # 4 = openai/auth error, 5 = no patch, 6 = patch did not apply
        append_progress "## $(date -u +%Y-%m-%d) ${story_id} (${title}) [RED-AGENT]
- agent mode=$AGENT_MODE rc=$agent_rc (4=auth/llm, 5=no-patch, 6=apply-failed)
- Discovered: agent invocation failed; see logs above
- Next: fix agent config or re-run ralph.sh"
        log "agent failed (rc=$agent_rc); appended red-agent block to progress.txt"
        if [ "$CONTINUE_ON_RED" -eq 1 ]; then
            if ! record_failure "$agent_rc"; then
                append_progress "## $(date -u +%Y-%m-%d) M81-LOOP [RED-LOOP-GIVEUP]
- $MAX_CONSECUTIVE_FAILURES consecutive failures all with rc=$last_fail_rc
- Last failed story: $story_id
- Discovered: loop is in a rut; need a human to break out
- Next: address the root cause, reset, and re-run ralph.sh"
                log "consecutive-failure cap reached; appended [RED-LOOP-GIVEUP] block"
                exit 7
            fi
            continue
        fi
        exit "$agent_rc"
    fi

    set +e
    run_test "$test_target"
    test_rc=$?
    set -e

    if [ "$test_rc" -ne 0 ]; then
        append_progress "## $(date -u +%Y-%m-%d) ${story_id} (${title}) [RED]
- test_target '$test_target' failed (rc=$test_rc)
- Discovered: $(cd "$REPO_ROOT" && mvn test -Dtest="$test_target" 2>&1 | tail -20 || true)
- Next: fix and re-run ralph.sh"
        log "test failed; appended red block to progress.txt"
        if [ "$CONTINUE_ON_RED" -eq 1 ]; then
            if ! record_failure 3; then
                append_progress "## $(date -u +%Y-%m-%d) M81-LOOP [RED-LOOP-GIVEUP]
- $MAX_CONSECUTIVE_FAILURES consecutive failures all with rc=3
- Last failed story: $story_id
- Discovered: loop is in a rut; need a human to break out
- Next: address the root cause, reset, and re-run ralph.sh"
                log "consecutive-failure cap reached; appended [RED-LOOP-GIVEUP] block"
                exit 7
            fi
            continue
        fi
        exit 3
    fi

    # Green path: reset failure tracking and proceed with commit.
    last_fail_rc=""
    consec_count=0

    # green: commit + mark
    commit_msg="${MILESTONE}-${story_id}: ${title}"
    (cd "$REPO_ROOT" && git add -A && git -c trailer.co-authored-by= commit -m "$commit_msg" -m "no Co-authored-by trailer per AGENTS.md")
    commit_sha="$(cd "$REPO_ROOT" && git rev-parse HEAD)"

    mark_passed "$story_id" "$commit_sha"

    append_progress "## $(date -u +%Y-%m-%d) ${story_id} (${title}) [GREEN]
- commit: $commit_sha
- test_target '$test_target' green
- Next: $(pick_next_story || echo 'none')"

    log "story $story_id passed; commit=$commit_sha"
done

log "max iterations ($MAX) reached; exiting"
exit 0
