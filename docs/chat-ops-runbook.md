# Chat & A2A Operator Runbook (M25)

Operational guide for chat SSE, A2A federation, exports, retention, and audit. All identifiers in logs and audit APIs are hashed — never raw user or chat ids.

## Rate limiting

| Scope | Endpoint(s) | Bucket key | Default tier |
|-------|-------------|------------|--------------|
| Chat SSE | `POST /api/v1/chats/{id}/messages/stream` | `userId:TIER:CHAT_SSE` | 10/min |
| A2A | `POST /a2a/v1/jsonrpc`, `POST /a2a/v1/stream` | `userId:TIER:A2A` | 10/min (separate bucket) |

- **429 response** includes `Retry-After: 60` (seconds).
- Tier from `X-API-Key` session token: `DEFAULT`, `HIGH`, or `UNLIMITED`.
- Metrics: `chat_rate_limited_total{tier=..., scope=CHAT_SSE|A2A}`, `chat_turn_duration_seconds{tier=...}`.
- Retention metrics: `chat_retention_runs_total`, `chat_retention_chats_purged_total`, `chat_retention_messages_purged_total`.

## Chat retention

- Config: `chat.retention.idle-days` (default `0` = disabled).
- Scheduler purges idle **non-default** chats past the cutoff.
- Default chat is kept; messages may be soft-deleted via user data delete.
- Metrics: `chat_retention_runs_total`, `chat_retention_chats_purged_total`, `chat_retention_messages_purged_total`, `chat_retention_failures_total`.
- Admin API: `GET /api/v1/admin/chat-retention` (requires `X-User-Id: admin`).

## Exports & data lifecycle

| Action | API | Audit action |
|--------|-----|--------------|
| Single chat export | `GET /api/v1/chats/{id}/export` | `CHAT_EXPORT` |
| User bundle export | `GET /api/v1/chats/export-bundle` | `CHAT_EXPORT_BUNDLE` |
| Delete all user data | `DELETE /api/v1/chats/data` | `CHAT_DATA_DELETE` |

- Responses include `auditReferenceHash` (SHA-256 of audit log id) for UI feedback.
- Web UI: `/chat` sidebar — **Export All** / **Delete All Data**.

## Admin tools

Access with `?user=admin` (sets `medexpertmatch-user-id=admin` cookie).

| Page | URL |
|------|-----|
| Admin hub | `/admin?user=admin` |
| Session tokens | `/admin/session-tokens?user=admin` |
| Export audit | `/admin/chat-exports?user=admin` |

Audit API: `GET /api/v1/admin/audit/chat-exports?action=CHAT_EXPORT_BUNDLE` (optional filter).

## Observability

- Dashboard: `grafana/dashboard.json`
- Alerts: `grafana/chat-alerts.yml`, hints in `docs/grafana-chat-alerts.md`
- Key panels: tier rate limits, turn duration by tier, export rate

## PHI safety

- Never log message content, patient identifiers, or raw chat/user ids.
- Exports run through `PhiGuard` redaction before persistence or download.
- Audit stores hashed resource and actor ids only.

## Local verification

```bash
mvn verify
mvn test -Pplaywright -Dplaywright.enabled=true   # optional browser smoke
```
