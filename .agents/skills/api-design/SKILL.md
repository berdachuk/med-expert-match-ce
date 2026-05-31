# API and UI edge (REST and Thymeleaf)

## Description

Conventions for **REST** controllers, **OpenAPI** maintenance, and **Thymeleaf** server-rendered UI under `web` and related REST packages in domain modules.

## When to use

- Adding or changing HTTP endpoints or DTOs exposed to clients.
- Updating `src/main/resources/api/openapi.yaml`.
- Adding or changing Thymeleaf templates or static assets.

## Instructions

- **REST**: keep controllers thin; validate input; use domain services; follow existing versioning paths (e.g. `/api/...` patterns in codebase).
- **OpenAPI**: update the spec when the public HTTP contract changes; regenerate or adjust client docs if used.
- **Thymeleaf**: templates under `src/main/resources/templates/`; fragments under `templates/fragments/`; static files under `src/main/resources/static/`.
- **Security**: respect global security configuration; do not expose PHI in error payloads or logs.
- **Errors**: use explicit, safe messages for clients; align with project exception types.

## Boundaries

- Do not embed business rules only in controllers that belong in domain services.
- Do not skip OpenAPI updates for user-visible API changes.
