#!/usr/bin/env bash
# Call an OpenAI-compatible chat completions endpoint and print the model's
# first message content to stdout.
#
# Usage: call_openai.sh < prompt.md
# Env:   OPENAI_API_KEY      (required)
#        OPENAI_BASE_URL     (required, e.g. https://api.openai.com/v1)
#        OPENAI_MODEL        (required, e.g. gpt-4o-mini)
#        OPENAI_TIMEOUT      (optional, default 600 seconds)
#        OPENAI_TEMPERATURE  (optional, default 0.0 for deterministic diffs)
#
# Exit codes:
#   0  Success (response printed to stdout)
#   4  Missing env, non-200 HTTP, or empty response

set -euo pipefail

if [ -z "${OPENAI_API_KEY:-}" ]; then
    echo "ERROR: OPENAI_API_KEY is not set" >&2
    exit 4
fi
if [ -z "${OPENAI_BASE_URL:-}" ]; then
    echo "ERROR: OPENAI_BASE_URL is not set" >&2
    exit 4
fi
if [ -z "${OPENAI_MODEL:-}" ]; then
    echo "ERROR: OPENAI_MODEL is not set" >&2
    exit 4
fi

PROMPT=$(cat)
TIMEOUT="${OPENAI_TIMEOUT:-600}"
TEMPERATURE="${OPENAI_TEMPERATURE:-0.0}"

# Build the JSON request body using jq. Escape the prompt into a JSON string.
REQUEST_BODY=$(jq -n \
    --arg model "$OPENAI_MODEL" \
    --arg prompt "$PROMPT" \
    --argjson temp "$TEMPERATURE" \
    '{
        model: $model,
        temperature: $temp,
        messages: [
            {role: "system", content: "You are an autonomous coding agent. You return a single ```diff fenced block in your final message. No other prose."},
            {role: "user", content: $prompt}
        ]
    }')

# Normalize the base URL (strip trailing slash).
BASE="${OPENAI_BASE_URL%/}"
URL="$BASE/chat/completions"

# Call the endpoint. Capture body + http code.
TMP_RESPONSE="$(mktemp)"
HTTP_CODE=$(curl -sS -m "$TIMEOUT" \
    -o "$TMP_RESPONSE" \
    -w "%{http_code}" \
    -H "Authorization: Bearer $OPENAI_API_KEY" \
    -H "Content-Type: application/json" \
    -X POST \
    --data-raw "$REQUEST_BODY" \
    "$URL" 2>/dev/null) || {
    rm -f "$TMP_RESPONSE"
    echo "ERROR: curl failed to reach $URL" >&2
    exit 4
}

if [ "$HTTP_CODE" -ne 200 ]; then
    echo "ERROR: HTTP $HTTP_CODE from $URL" >&2
    head -c 2000 "$TMP_RESPONSE" >&2
    rm -f "$TMP_RESPONSE"
    exit 4
fi

# Extract the first choice's message content. If the response is malformed,
# jq will fail (set -e).
CONTENT=$(jq -r '.choices[0].message.content // ""' "$TMP_RESPONSE")
rm -f "$TMP_RESPONSE"

if [ -z "$CONTENT" ]; then
    echo "ERROR: empty content in response" >&2
    exit 4
fi

printf '%s' "$CONTENT"
