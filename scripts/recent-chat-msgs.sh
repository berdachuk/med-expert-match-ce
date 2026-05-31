#!/usr/bin/env bash
docker exec medexpertmatch-postgres psql -U medexpertmatch -d medexpertmatch -c \
  "SELECT c.agent_id, m.role, left(m.content, 180) AS content FROM medexpertmatch.chat_message m JOIN medexpertmatch.chat c ON c.id=m.chat_id ORDER BY m.created_at DESC LIMIT 8;"
