#!/usr/bin/env bash
# Live verification of AI orchestrator against running stack (local/docker).
set -u

BASE="${BASE:-http://localhost:8094}"
USER="${USER:-admin}"
HDR=(-H "Content-Type: application/json" -H "X-User-Id: $USER")
PASS=0
FAIL=0
WARN=0

pass() { echo "PASS: $1"; PASS=$((PASS + 1)); }
fail() { echo "FAIL: $1"; FAIL=$((FAIL + 1)); }
warn() { echo "WARN: $1"; WARN=$((WARN + 1)); }

check_http() {
  local name="$1" url="$2" expect="${3:-200}"
  local code
  code=$(curl -sS -o /tmp/orch-body.json -w "%{http_code}" "$url" "${HDR[@]}" 2>/dev/null || echo "000")
  if [[ "$code" == "$expect" ]]; then
    pass "$name (HTTP $code)"
  else
    fail "$name (HTTP $code, expected $expect)"
    head -c 300 /tmp/orch-body.json 2>/dev/null; echo
  fi
}

echo "=============================================="
echo " AI Orchestrator verification — $BASE"
echo "=============================================="

echo
echo "--- Infrastructure ---"
check_http "Actuator health" "$BASE/actuator/health"
check_http "Agent card" "$BASE/.well-known/agent-card.json"
check_http "A2A skills registry" "$BASE/a2a/v1/skills"

echo
echo "--- Data ---"
CASES=$(curl -sf "$BASE/api/cases/list?limit=5" "${HDR[@]}" 2>/dev/null || echo "[]")
CASE_ID=$(echo "$CASES" | python3 -c "
import json,sys
cases=json.load(sys.stdin)
print(cases[0]['id'] if cases else '')
" 2>/dev/null || echo "")
if [[ -n "$CASE_ID" ]]; then
  pass "Cases available (sample: $CASE_ID)"
else
  fail "No cases in database — generate test data first"
fi

echo
echo "--- Workflow orchestrator (MedicalAgentService sync APIs) ---"
if [[ -n "$CASE_ID" ]]; then
  echo "  match-sync (may take 60-120s)..."
  MATCH_CODE=$(curl -sS --max-time 180 -o /tmp/match.json -w "%{http_code}" -X POST \
    "$BASE/api/v1/agent/match-sync/$CASE_ID" "${HDR[@]}" -d '{"sessionId":"orch-verify-match"}' || echo "000")
  if [[ "$MATCH_CODE" == "200" ]]; then
    python3 - <<'PY'
import json
d=json.load(open("/tmp/match.json"))
r=d.get("response") or ""
print("  response length:", len(r))
if len(r) > 50 and "error" not in r.lower()[:100]:
    print("  preview:", r[:200].replace("\n"," "))
    exit(0)
exit(1)
PY
    if [[ $? -eq 0 ]]; then pass "match-sync returned substantive response"; else warn "match-sync HTTP 200 but thin/error response"; fi
  else
    fail "match-sync (HTTP $MATCH_CODE)"
    head -c 400 /tmp/match.json 2>/dev/null; echo
  fi

  echo "  analyze-case-sync..."
  ANALYZE_CODE=$(curl -sS --max-time 120 -o /tmp/analyze.json -w "%{http_code}" -X POST \
    "$BASE/api/v1/agent/analyze-case-sync/$CASE_ID" "${HDR[@]}" -d '{}' || echo "000")
  [[ "$ANALYZE_CODE" == "200" ]] && pass "analyze-case-sync" || fail "analyze-case-sync (HTTP $ANALYZE_CODE)"

  echo "  route-case-sync..."
  ROUTE_CODE=$(curl -sS --max-time 120 -o /tmp/route.json -w "%{http_code}" -X POST \
    "$BASE/api/v1/agent/route-case-sync/$CASE_ID" "${HDR[@]}" -d '{}' || echo "000")
  [[ "$ROUTE_CODE" == "200" ]] && pass "route-case-sync" || fail "route-case-sync (HTTP $ROUTE_CODE)"
fi

echo "  match-from-text-sync..."
MFT_CODE=$(curl -sS --max-time 180 -o /tmp/mft.json -w "%{http_code}" -X POST \
  "$BASE/api/v1/agent/match-from-text-sync" "${HDR[@]}" \
  -d '{"caseText":"45-year-old with chest pain and dyspnea, needs cardiology consult","sessionId":"orch-verify-mft"}' || echo "000")
if [[ "$MFT_CODE" == "200" ]]; then
  python3 -c "import json;d=json.load(open('/tmp/mft.json'));r=d.get('response','');print('  len',len(r));exit(0 if len(r)>30 else 1)" \
    && pass "match-from-text-sync" || warn "match-from-text-sync thin response"
else
  fail "match-from-text-sync (HTTP $MFT_CODE)"
fi

echo
echo "--- Chat orchestrator (SSE) ---"
CHAT_JSON=$(curl -sf -X POST "$BASE/api/v1/chats" "${HDR[@]}" \
  -d '{"name":"orch-verify","agentId":"auto"}' 2>/dev/null || echo "")
CHAT_ID=$(echo "$CHAT_JSON" | python3 -c "import json,sys; print(json.load(sys.stdin).get('id',''))" 2>/dev/null || echo "")
if [[ -z "$CHAT_ID" ]]; then
  fail "Could not create chat for orchestrator test"
else
  pass "Chat session created ($CHAT_ID)"
  OUT=$(mktemp)
  HTTP=$(curl -sS -N --max-time 120 -o "$OUT" -w "%{http_code}" -X POST \
    "$BASE/api/v1/chats/$CHAT_ID/messages/stream" "${HDR[@]}" \
    -H "Accept: text/event-stream" \
    -d "{\"content\":\"Find specialist for case $CASE_ID\",\"agentId\":\"auto\"}" 2>/dev/null || echo "000")
  python3 - <<PY
import json, pathlib, re, sys
text = pathlib.Path("$OUT").read_text(errors="replace")
done = re.search(r"event:\s*done\s*\ndata:\s*(\{.*\})", text, re.S)
content = ""
if done:
    content = json.loads(done.group(1)).get("content","")
err_phrases = ["could not generate", "encountered an error", "cannot assist", "cannot access"]
lower = content.lower()
if "$HTTP" != "200":
    print("FAIL chat auto orchestrator HTTP $HTTP")
    sys.exit(1)
if any(p in lower for p in err_phrases):
    print("WARN chat auto orchestrator reply:", content[:180].replace(chr(10)," "))
    sys.exit(2)
if len(content.strip()) > 40:
    print("PASS chat auto orchestrator:", content[:180].replace(chr(10)," "))
    sys.exit(0)
print("WARN chat auto orchestrator short reply:", content[:120])
sys.exit(2)
PY
  rc=$?
  [[ $rc -eq 0 ]] && pass "Chat auto orchestrator meaningful reply" \
    || { [[ $rc -eq 2 ]] && warn "Chat auto orchestrator degraded (see above)" || fail "Chat auto orchestrator stream failed"; }
  rm -f "$OUT"
fi

echo
echo "--- Chat specialist-matcher (direct skill, no Task delegation) ---"
if [[ -n "$CASE_ID" ]]; then
  CHAT2=$(curl -sf -X POST "$BASE/api/v1/chats" "${HDR[@]}" \
    -d '{"name":"orch-sm","agentId":"specialist-matcher"}' 2>/dev/null || echo "")
  CID2=$(echo "$CHAT2" | python3 -c "import json,sys; print(json.load(sys.stdin).get('id',''))" 2>/dev/null || echo "")
  OUT2=$(mktemp)
  curl -sS -N --max-time 120 -o "$OUT2" -X POST \
    "$BASE/api/v1/chats/$CID2/messages/stream" "${HDR[@]}" \
    -d "{\"content\":\"Match doctors for case $CASE_ID\",\"agentId\":\"specialist-matcher\"}" >/dev/null 2>&1 || true
  python3 - <<PY
import json, pathlib, re, sys
text = pathlib.Path("$OUT2").read_text(errors="replace")
done = re.search(r"event:\s*done\s*\ndata:\s*(\{.*\})", text, re.S)
content = json.loads(done.group(1)).get("content","") if done else ""
bad = any(x in content.lower() for x in ["encountered an error", "could not generate", "toolcallback"])
if bad:
    print("WARN specialist-matcher:", content[:160].replace(chr(10)," "))
    sys.exit(2)
if len(content.strip()) > 40:
    print("PASS specialist-matcher:", content[:160].replace(chr(10)," "))
    sys.exit(0)
print("WARN specialist-matcher short:", content[:100])
sys.exit(2)
PY
  rc2=$?
  [[ $rc2 -eq 0 ]] && pass "Chat specialist-matcher" || warn "Chat specialist-matcher degraded"
  rm -f "$OUT2"
fi

echo
echo "--- Recent orchestrator log signals ---"
docker logs medexpertmatch-app 2>&1 | grep -E "matchDoctors\(\)|match-sync|ToolCallback|stream returned no text|Chat stream failed|Step 1: LLM|match_doctors_to_case\(\)" | tail -n 12 || true

echo
echo "=============================================="
echo " SUMMARY: PASS=$PASS  WARN=$WARN  FAIL=$FAIL"
echo "=============================================="
[[ $FAIL -eq 0 ]]
