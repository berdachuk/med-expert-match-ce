#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE:-http://localhost:8094}"
USER="${USER:-admin}"

echo "=== Create chat ==="
CHAT_JSON=$(curl -sf -X POST "$BASE/api/v1/chats" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER" \
  -d '{"name":"curl-test","agentId":"auto"}')
CHAT_ID=$(echo "$CHAT_JSON" | python3 -c "import json,sys; print(json.load(sys.stdin)['id'])")
echo "chatId=$CHAT_ID"

BODY='{"content":"Find a specialist for this case.\nCase ID: 6a1c79a862d83900018ecef3","agentId":"auto"}'

echo "=== Stream message (may take 30-120s) ==="
OUT=$(mktemp)
HTTP=$(curl -sS -N --max-time 120 -o "$OUT" -w "%{http_code}" -X POST \
  "$BASE/api/v1/chats/$CHAT_ID/messages/stream" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $USER" \
  -H "Accept: text/event-stream" \
  -d "$BODY")
echo "HTTP=$HTTP"
echo "--- SSE tail ---"
tail -n 50 "$OUT"
echo "--- done event ---"
grep -A1 "^event:[[:space:]]*done" "$OUT" || echo "(no done event)"
echo "--- token count ---"
grep -c "^event: token" "$OUT" || true
python3 - <<PY
import json, re, pathlib
text = pathlib.Path("$OUT").read_text(errors="replace")
for block in text.split("\n\n"):
    if "event: done" in block:
        for line in block.splitlines():
            if line.startswith("data: "):
                payload = json.loads(line[6:])
                content = payload.get("content", "")
                print("reply length:", len(content))
                print("reply preview:", content[:400].replace("\n", " "))
                if "could not generate" in content.lower():
                    print("RESULT: FAIL (empty reply message)")
                elif len(content.strip()) > 20:
                    print("RESULT: OK (non-empty assistant reply)")
                else:
                    print("RESULT: UNCERTAIN")
PY
rm -f "$OUT"

echo "=== Recent app logs (stream fallback) ==="
docker logs medexpertmatch-app 2>&1 | grep -E "Chat stream returned no text|Chat stream failed|retrying sync|Chat LLM turn" | tail -n 10 || echo "(no matching log lines)"
