# M12: API Versioning, Advanced Search & Production Release — ✅ Archived

M11 delivered E2E acceptance tests, feature flags, i18n (EN+RU), accessibility improvements, body size limits, and graceful shutdown.

## Completed

| # | Improvement | Status |
|---|---|---|
| 1 | API v2 routing + `ApiVersionFilter` | ✅ |
| 2 | Faceted document search + `DocumentSearchFacetedIT` | ✅ |
| 3 | GZip response compression | ✅ |
| 4 | API usage analytics (`ApiUsageInterceptor`) | ✅ |
| 5 | Flyway V2 + `ApiSessionToken`/`AuditLog` repos + `SessionAuditRepositoryIT` | ✅ |
| 6 | `RELEASE_CHECKLIST.md` + `CHANGELOG.md` | ✅ |

**Verification:** `mvn verify` — Flyway V1+V2, faceted search IT, session/audit IT passing.
