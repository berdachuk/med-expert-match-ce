# M98: Security Hardening and Auth Enablement

**Status:** Active (planned 2026-06-13)
**Created:** 2026-06-13
**Depends on:** M97 (archived)

## Problem Statement

1. **Auth is entirely disabled** — both `LocalSecurityConfig` and `DockerSecurityConfig` use `.anyRequest().permitAll()`. The `medexpertmatch.auth.enabled` flag defaults to `false`. No `@PreAuthorize` annotations exist anywhere.
2. **Document search endpoints expose PHI-potential content without access control** — `DocumentSearchController` serves document chunks; `DocumentSearchV2Controller` adds bulk operations. No auth guard on either.
3. **Admin endpoints partially guarded** — `AdminAccessGuard` exists but only some admin endpoints call `requireAdmin()`. Others (document admin, harness admin) have unprotected paths.
4. **Session token auth is present but untested** — `SessionTokenApiKeyAuthFilter` exists with `ApiKeyAuthFilter` and `AdminUserCookieFilter`, but `auth.enabled=false` bypasses them all.

## Goal

1. Enable `medexpertmatch.auth.enabled=true` as default (with secure-demo profile fallback)
2. Add `@PreAuthorize` annotations to all document search/ingest/admin endpoints
3. Add `AdminAccessGuard` check to `DocumentSearchController.backfillEmbeddings()`
4. Update `LocalSecurityConfig` to deny-by-default for `/api/v1/admin/**` and `/api/v1/documents/**`
5. Update `DockerSecurityConfig` with same posture
6. Verify no breaking changes to test context or local dev flow
7. `mvn verify` green

## Phases

| Phase | Task | Status |
|-------|------|--------|
| 1 | Add `adminAccessGuard` to `DocumentSearchController.backfillEmbeddings()` | Pending |
| 2 | Enable auth by default, add `secure-demo` profile bypass | Pending |
| 3 | Add `@PreAuthorize` on doc search endpoints | Pending |
| 4 | Review all admin endpoints for guard coverage | Pending |
| 5 | `mvn verify` green | Pending |
| 6 | Archive plan | Pending |

## References

- `src/main/java/.../core/config/LocalSecurityConfig.java`
- `src/main/java/.../core/config/DockerSecurityConfig.java`
- `src/main/java/.../documents/rest/DocumentSearchController.java`
- `src/main/java/.../documents/rest/DocumentSearchV2Controller.java`
- `src/main/java/.../core/security/AdminAccessGuard.java`
- `src/main/java/.../core/rest/AdminController.java`
- `src/main/resources/application.yml` (`medexpertmatch.auth.*`)

## Acceptance Criteria

- [ ] `POST /api/v1/documents/backfill-embeddings` requires admin
- [ ] All document search APIs require authentication when auth is enabled
- [ ] `LocalSecurityConfig` denies `/api/v1/admin/**` and `/api/v1/documents/**` by default
- [ ] `secure-demo` profile preserves current permit-all behavior for local dev
- [ ] Test config provides mock auth context for all affected controllers
- [ ] `mvn verify` exits 0
