#!/usr/bin/env bash
set -euo pipefail
BASE=http://localhost:8094
curl -sf "$BASE/a2a/v1/skills" -H "X-User-Id: admin" | python3 -c "import json,sys; s=json.load(sys.stdin); print('A2A skills:', [x.get('id') for x in s])"
curl -sS --max-time 180 -X POST "$BASE/a2a/v1/jsonrpc" \
  -H "Content-Type: application/json" -H "X-User-Id: admin" \
  --data-binary @/mnt/c/Users/Siarhei_Berdachuk/projects-med-expert/med-expert-match-ce/scripts/a2a-doctor-match.json \
  > /tmp/a2a-result.json
python3 - <<'PY'
import json
d=json.load(open("/tmp/a2a-result.json"))
if "error" in d:
    print("A2A FAIL:", d["error"])
    raise SystemExit(1)
r=d["result"]
print("A2A status:", r.get("status"), "skill:", r.get("skill"))
msg=r.get("result",{}).get("message","")
print("message length:", len(msg))
print("preview:", msg[:300].replace("\n"," "))
if len(msg) > 30:
    print("A2A PASS")
else:
    print("A2A WARN thin response")
PY
