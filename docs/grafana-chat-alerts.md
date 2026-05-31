# Grafana Chat Alert Hints (M21)

Suggested Prometheus alert rules for chat operations. Tune thresholds per environment.

## Chat rate limiting

```yaml
- alert: ChatRateLimitSpike
  expr: rate(chat_rate_limited_total[5m]) > 0.5
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: Elevated chat SSE rate limiting
    description: More than 0.5 chat turns/sec are being rejected (429). Check abuse or lower tier limits.
```

## Chat stream errors

```yaml
- alert: ChatStreamErrors
  expr: rate(chat_stream_errors_total[5m]) > 0.1
  for: 15m
  labels:
    severity: warning
  annotations:
    summary: Chat SSE stream errors elevated
```

## Chat exports

```yaml
- alert: ChatExportBurst
  expr: rate(chat_export_count_total[5m]) > 1
  for: 5m
  labels:
    severity: info
  annotations:
    summary: Unusual chat export volume
    description: Review audit_log CHAT_EXPORT entries (hashed ids only).
```

Dashboard panels live in `grafana/dashboard.json`.
