#!/usr/bin/env bash
# Apply a unified-diff patch to the working tree.
#
# Usage: apply_patch.sh <PATCH_FILE>
# Runs `git apply --check` first, then `git apply` from the repo root.
#
# Exit codes:
#   0  Patch applied cleanly
#   6  Patch did not pass `git apply --check` (does not apply cleanly)
#   7  Patch file missing

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

if [ $# -lt 1 ] || [ ! -f "$1" ]; then
    echo "ERROR: patch file not provided or missing" >&2
    exit 7
fi

PATCH_FILE="$1"

(cd "$REPO_ROOT" && git apply --check "$PATCH_FILE") || {
    echo "ERROR: patch did not pass 'git apply --check'" >&2
    echo "--- patch contents ---" >&2
    cat "$PATCH_FILE" >&2
    exit 6
}

(cd "$REPO_ROOT" && git apply "$PATCH_FILE")
echo "patch applied: $PATCH_FILE"
