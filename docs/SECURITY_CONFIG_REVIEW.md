# Security Configuration Review

This document reviews configuration files for security issues and lists recommended mitigations. It covers
`docker-compose.yml`, `docker-compose.dev.yml`, `application.yml`, and `application-local.yml`, plus Spring security and
management settings.

## Demonstrate security locally (secure-demo profile)

To run the application with security hardening enabled and demonstrate that config and code pass common security checks,
use the **secure-demo** profile. Minimal change: add the profile to your run.

**Run with security hardening:**

```bash
# With local DB (start Postgres first via docker-compose.dev.yml)
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local,secure-demo"
```

Or with only the secure-demo profile (uses default DB URL from application.yml):

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=secure-demo"
```

**What secure-demo does:**

- **Actuator and /health** require HTTP Basic auth (default user `demo`, password `demo`; override with
  `MEDEXPERTMATCH_DEMO_USER` and `MEDEXPERTMATCH_DEMO_PASSWORD`). Unauthenticated calls get 401.
- **Error and binding messages** are not sent to the client (`include-message: never`, `include-binding-errors: never`).
- **Health details** are shown only when authorized (`show-details: when_authorized`); actuator exposes only `health` (
  no info/metrics).
- **Swagger UI and API docs** are disabled.
- **Logging** is INFO (no DEBUG).

**Verify:**

```bash
# Unauthenticated: 401
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health
# 401

# Authenticated: 200 and health details
curl -s -u demo:demo http://localhost:8080/actuator/health

# API still works without auth (for local demo)
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/openapi.json
# 404 (API docs disabled in secure-demo)
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/health
# 401 (custom /health protected)
```

When `local` or `docker` is active together with `secure-demo`, the secure-demo security config takes precedence (
actuator and /health protected; API permitAll).

---

## 1. Secrets and Credentials

### Issues

- **docker-compose.yml**: Database password (`medexpertmatch`) and API keys (`ollama`) are in plain text. Acceptable for
  local/dev only; must not be used as-is in production.
- **application.yml**: Defaults (`MEDEXPERTMATCH_DB_PASSWORD:medexpertmatch`, `CHAT_API_KEY:ollama`) are weak
  placeholders. Env overrides are supported; production must set strong secrets via environment.
- **No `.env` in .gitignore**: If you use a `.env` file for overrides, it should be ignored so real keys are never
  committed.

### Recommendations

- Add `.env` to `.gitignore` and use `env_file: .env` in Compose only when present; document in README that production
  must use env vars or a secrets manager, not committed files.
- For production: set `SPRING_DATASOURCE_PASSWORD`, `CHAT_API_KEY`, `EMBEDDING_API_KEY`, etc. from a secrets manager or
  secure env (e.g. Docker secrets, Kubernetes secrets). Never commit production credentials.

---

## 2. Database Exposure

### Issues

- **docker-compose.yml** and **docker-compose.dev.yml**: PostgreSQL is published as `5433:5432`, so the port is
  reachable on all host interfaces (`0.0.0.0`). In a production deployment, the database should not be exposed to the
  host or should be bound to `127.0.0.1` if admin access is required.

### Recommendations

- For production Compose (or K8s), do not publish the Postgres port, or restrict to `127.0.0.1:5433:5432`. Keep current
  behavior for local/dev only.

---

## 3. Application Security (Spring)

### Issues

- **LocalSecurityConfig** (`@Profile("local")`) and **DockerSecurityConfig** (`@Profile("docker")`): CSRF is disabled
  and `anyRequest().permitAll()`. There is no authentication or authorization in these profiles. The design assumes the
  app runs behind a gateway that enforces auth in production.
- If the app is ever exposed directly (e.g. port 8094 on a public IP) with `local` or `docker` profile, all endpoints
  are unauthenticated.

### Recommendations

- Do not expose the app directly to the internet when using `local` or `docker`. Always put it behind a reverse
  proxy/gateway that enforces authentication and TLS.
- For a future production profile: add a dedicated security config (e.g. `@Profile("prod")`) that enables CSRF for
  state-changing endpoints, restricts actuator to authenticated users, and uses a proper authentication mechanism (e.g.
  OAuth2, JWT) or relies on gateway auth and allows only gateway IPs if applicable.

---

## 4. Management and Actuator Endpoints

### Issues

- **application.yml**:
    - `management.endpoints.web.exposure.include: health,info,metrics` — all are exposed.
    - `management.endpoint.health.show-details: always` and `show-components: always` — full health details (e.g. DB
      status, component names) are visible to anyone who can reach the app.
    - `info` and `metrics` can expose JVM and application internals.

### Recommendations

- For production: use a profile (e.g. `application-prod.yml`) that sets:
    - `show-details: when-authorized` (or `when_authorized`) and `show-components: when-authorized`, and secure actuator
      with the same auth as the rest of the app; or
    - Expose only `health` (with minimal details) on a separate port/path and keep `info`/`metrics` behind auth or
      internal network.

---

## 5. Error and Validation Information

### Issues

- **application.yml**: `server.error.include-message: always` and `include-binding-errors: always` send exception
  messages and binding errors to the client. In production this can leak internal details or validation logic.

### Recommendations

- For production profile: set `include-message: never` (or `on_param`) and `include-binding-errors: never` (or
  `on_param`), and log full details server-side only.

---

## 6. OpenAPI / Swagger

### Issues

- **application.yml**: `springdoc.api-docs.path: /api/v1/openapi.json` and `swagger-ui` are enabled with
  `try-it-out-enabled: true`. This is convenient for development but increases attack surface in production (exploration
  of APIs, possible abuse of “try it out”).

### Recommendations

- For production: disable Swagger UI and/or API docs, or serve them only on an internal URL and protect with auth.
  Alternatively use a profile to set `springdoc.api-docs.enabled: false` and `springdoc.swagger-ui.enabled: false`.

---

## 7. Logging

### Issues

- **docker-compose.yml**: Debug logging is enabled for several packages (`LOGGING_LEVEL_*: DEBUG`). Verbose logs in
  production can expose internal behavior and, if not carefully filtered, risk logging sensitive data (AGENTS.md already
  forbids logging PHI; ensure all paths comply).

### Recommendations

- In production, set logging to INFO or WARN. Use DEBUG only in non-production or for short-lived debugging. Ensure no
  PHI or credentials are ever logged.

---

## 8. application-local.yml

### Issues

- **server.address: 192.168.0.73** — machine-specific IP. The file is listed in `.gitignore`, so it should not be
  committed. If it were ever committed, it would reveal a local network address.

### Recommendations

- Keep `application-local.yml` in `.gitignore`. If you need to bind to a specific IP, document that this file is
  local-only and must not be committed. Consider using an env variable for `server.address` if needed across machines.

---

## 9. docker-compose.dev.yml

### Issues

- Same hardcoded DB credentials as in main Compose.
- Volume `~/data/medexpertmatch-postgres` — path outside the project; ensure this path is not world-readable and is
  backed up appropriately.

### Recommendations

- Treat as dev-only; do not use in production. For production, use the main Compose (or K8s) with secrets from env or a
  secrets manager and no published DB port (or 127.0.0.1 only).

---

## 10. Summary Checklist

| Area                 | Current state (local/docker)     | Production recommendation              |
|----------------------|----------------------------------|----------------------------------------|
| DB password          | Plain text in Compose / defaults | Env or secrets manager only            |
| API keys             | Plain text / placeholder         | Env or secrets manager only            |
| `.env`               | Not in .gitignore                | Add to .gitignore                      |
| Postgres port        | Published 0.0.0.0:5433           | Do not publish or bind 127.0.0.1       |
| App auth             | None (permitAll)                 | Gateway auth or app-level auth in prod |
| Health details       | Always visible                   | when-authorized or minimal on public   |
| Error messages       | Always included                  | never or on_param in prod              |
| Swagger / try-it-out | Enabled                          | Disable or protect in prod             |
| Logging              | DEBUG in Docker Compose          | INFO/WARN in prod                      |

**secure-demo profile:** Use `--spring.profiles.active=local,secure-demo` (or `secure-demo`) to run with the above
production-style settings locally and demonstrate that the application passes security checks. See "Demonstrate security
locally" at the top of this document.

---

*Last updated: 2026-02-08*
