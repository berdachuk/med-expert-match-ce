# Database Migrations

## Description
Flyway migration strategy, V1 consolidation pattern, and SQL conventions. Covers `core` and any module with DB tables.

## When to use
- Adding or modifying database tables, columns, or constraints
- Updating `V1__initial_schema.sql`
- Planning post-MVP migration strategy
- Answering: "How do I change the schema?"

## Instructions

### V1 Consolidation Rule (MVP)

- **Current phase (MVP)**: ALL schema changes must go into `V1__initial_schema.sql`
- **No incremental migrations**: Do NOT create V2, V3, V4, etc. during MVP
- **Post-MVP**: Switch to incremental migrations (V2, V3, etc.) for new features
- To add a new table/column, edit V1 directly and consolidate

### Migration File Location

- `src/main/resources/db/migration/V1__initial_schema.sql`

### SQL Conventions

- Table names: `snake_case`, plural where appropriate
- Column names: `snake_case` (e.g., `doctor_id`, `created_at`)
- Foreign key constraints: use `REFERENCES` with `ON DELETE` behavior
- Include `IF EXISTS` / `IF NOT EXISTS` for idempotent re-runs
- Indexes: add for foreign key columns and frequently-queried columns
- PostgreSQL-specific types: `UUID` for primary keys, `VECTOR(1536)` for embeddings

### Flyway Configuration

- Flyway MUST use ONLY the primary application `DataSource`
- External/read-only `DataSource` must NEVER be used by Flyway
- Flyway auto-migrates on application startup

### Adding a Table to V1

1. Add `CREATE TABLE IF NOT EXISTS {table_name} (...)` to V1
2. Add matching `INSERT`/`UPDATE` queries to `src/main/resources/sql/`
3. Create domain entity, repository interface + impl in the owning module
4. Update Testcontainers-based integration tests

### Repository SQL Files

- Store repository SQL in `src/main/resources/sql/` directory
- One file per repository or per entity
- Use named parameters (`:paramName`) matching Java-side parameter names

## Boundaries
- Do NOT create V2/V3 migrations during MVP phase
- Do NOT use Flyway on secondary/external DataSources
- Do NOT drop tables without human approval
- Do NOT modify production migration strategy without human approval
