#!/usr/bin/env bash
set -u
BASE=http://localhost:8094
CASE=6a1c79a862d83900018ecef3
HDR=(-H "Content-Type: application/json" -H "X-User-Id: admin")
PASS=0
FAIL=0

pass() { echo "PASS: $1"; PASS=$((PASS+1)); }
fail() { echo "FAIL: $1"; FAIL=$((FAIL+1)); }

echo "=== 1. Direct match-sync (baseline) ==="
curl -sS --max-time 120 -X POST "$BASE/api/v1/agent/match-sync/$CASE" "${HDR[@]}" -d '{}' > /tmp/v-match.json
python3 - <<'PY'
import json
d=json.load(open("/tmp/v-match.json"))
mc=d.get("metadata",{}).get("matchCount",-1)
r=d.get("response","")
print("  matchCount:", mc)
print("  response preview:", r[:250].replace("\n"," "))
import sys
sys.exit(0 if mc and mc >= 1 else 1)
PY
[[ $? -eq 0 ]] && pass "match-sync returns doctors (matchCount>=1)" || fail "match-sync returned no doctors"

echo
echo "=== 2. DB consultation_matches for case ==="
docker exec medexpertmatch-postgres psql -U medexpertmatch -d medexpertmatch -t -c \
  "SELECT COUNT(*) FROM medexpertmatch.consultation_matches WHERE case_id='$CASE';" | tr -d ' ' | read -r cnt
cnt=$(docker exec medexpertmatch-postgres psql -U medexpertmatch -d medexpertmatch -t -A -c \
  "SELECT COUNT(*) FROM medexpertmatch.consultation_matches WHERE case_id='$CASE';")
echo "  stored matches: $cnt"
[[ "$cnt" -ge 1 ]] && pass "DB has consultation_matches" || fail "DB has no consultation_matches"

echo
echo "=== 3. Chat specialist-matcher SSE ==="
CHAT=$(curl -sf -X POST "$BASE/api/v1/chats" "${HDR[@]}" \
  -d '{"name":"verify-sm","agentId":"specialist-matcher"}')
CID=$(echo "$CHAT" | python3 -c "import json,sys; print(json.load(sys.stdin)['id'])")
MSG="Find specialist for case $CASE"
OUT=$(mktemp)
curl -sS -N --max-time 120 -o "$OUT" -X POST "$BASE/api/v1/chats/$CID/messages/stream" "${HDR[@]}" \
  -d "$(python3 -c "import json; print(json.dumps({'content': '''$MSG''', 'agentId': 'specialist-matcher'}))")" >/dev/null 2>&1 || true
python3 - <<PY
import json,re,pathlib
text=pathlib.Path("$OUT").read_text(errors="replace")
done=re.search(r"event:\s*done\s*\ndata:\s*(\{.*\})", text, re.S)
content=""
if done:
    content=json.loads(done.group(1)).get("content","")
print("  reply len:", len(content))
print("  preview:", content[:280].replace("\n"," "))
bad=any(x in content.lower() for x in ["no specialist found","encountered an error","could not generate"])
has_doctor=any(x in content.lower() for x in ["doctor","surgery","match","specialist","score","dr."])
import sys
if bad and not has_doctor:
    sys.exit(1)
if len(content.strip())<30:
    sys.exit(2)
if has_doctor or "match" in content.lower():
    sys.exit(0)
sys.exit(2)
PY
rc=$?
[[ $rc -eq 0 ]] && pass "specialist-matcher chat mentions matches/doctors" \
  || { [[ $rc -eq 1 ]] && fail "specialist-matcher chat negative/empty reply" || fail "specialist-matcher chat too short or unclear"; }
rm -f "$OUT"

echo
echo "=== 4. Chat auto mode (Find Specialist paste) — expect known LLM issues ==="
CHAT2=$(curl -sf -X POST "$BASE/api/v1/chats" "${HDR[@]}" -d '{"name":"verify-auto","agentId":"auto"}')
CID2=$(echo "$CHAT2" | python3 -c "import json,sys; print(json.load(sys.stdin)['id'])")
OUT2=$(mktemp)
BODY=$(python3 -c "import json; print(json.dumps({'content':'Find specialist for case $CASE','agentId':'auto'}))")
curl -sS -N --max-time 120 -o "$OUT2" -X POST "$BASE/api/v1/chats/$CID2/messages/stream" "${HDR[@]}" -d "$BODY" >/dev/null 2>&1 || true
python3 - <<PY
import json,re,pathlib
text=pathlib.Path("$OUT2").read_text(errors="replace")
done=re.search(r"event:\s*done\s*\ndata:\s*(\{.*\})", text, re.S)
content=json.loads(done.group(1)).get("content","") if done else ""
print("  reply len:", len(content))
print("  preview:", content[:280].replace("\n"," "))
PY
docker logs medexpertmatch-app 2>&1 | grep "$CASE" | grep match_doctors_to_case | tail -n 3
rm -f "$OUT2"

echo
echo "=== SUMMARY: PASS=$PASS FAIL=$FAIL ==="
[[ $FAIL -eq 0 ]]
