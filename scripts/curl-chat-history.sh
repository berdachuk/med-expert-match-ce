#!/usr/bin/env bash
CHAT_ID="${1:?chat id required}"
curl -sf "http://localhost:8094/api/v1/chats/$CHAT_ID/history" -H "X-User-Id: admin" | python3 -c "
import json, sys
for m in json.load(sys.stdin):
    print(m['role'] + ':', m['content'][:300].replace(chr(10), ' '))
"
