#!/usr/bin/env bash
# Extract the first ```diff ... ``` fenced block from a model response.
#
# Usage: extract_patch.sh < response.txt
#        extract_patch.sh < file.txt
# Output: absolute path to a temp file containing the patch.
#
# Exit codes:
#   0  Patch found, path written to stdout
#   5  No ```diff block found

set -euo pipefail

if [ "$#" -gt 0 ] && [ -f "$1" ]; then
    INPUT=$(cat "$1")
else
    INPUT=$(cat)
fi

PATCH_FILE="$(mktemp -t ralph-patch.XXXXXX.diff)"

# Extract the first ```diff ... ``` block. The awk range matches from the
# opening fence to the first closing fence.
echo "$INPUT" | awk '
    /^```diff[[:space:]]*$/ { capturing = 1; next }
    /^```[[:space:]]*$/ && capturing { capturing = 0; exit }
    capturing { print }
' > "$PATCH_FILE"

# An empty extracted file means we did not find a diff block.
if [ ! -s "$PATCH_FILE" ]; then
    rm -f "$PATCH_FILE"
    echo "ERROR: no \`\`\`diff fenced block found in response" >&2
    exit 5
fi

echo "$PATCH_FILE"
