# M51: Production Hardening and Test Coverage

**Goal**: Improve production readiness through targeted health checks, increased test coverage in weak modules, and configuration hardening.

**Context**:
- M50 achieved zero test failures (495 tests, 100% pass rate).
- Overall instruction coverage is ~70% — strong core module coverage but several modules (evidence 18%, documents 8%, ingestion 24%, retrieval 30%) have weak coverage.
- Spring AI 2.0.0-M8, Spring Boot 4.0.2, Spring Modulith 2.0.2 are stable.
- 0 TODOs, 0 FIXMEs, 0 @Disabled tests in codebase.
- 529 main Java files across 18 modules.

## Steps

### 1. Add missing health indicators

**Current state**: `ComprehensiveHealthIndicator` aggregates health. Some subsystems lack dedicated indicators.

**Add**:
- `EvidenceHealthIndicator` — checks PubMed API connectivity and response time
- `PgVectorHealthIndicator` — validates PgVector extension and index health
- `AgeGraphHealthIndicator` — validates Apache AGE graph connectivity
- `EmbeddingPoolHealthIndicator` — checks multi-endpoint embedding pool availability

**File**: `system/health/` package. Each follows the existing `RerankingHealthIndicator` pattern.

### 2. Improve evidence module test coverage (18% → 60%+)

**Current**: Evidence module has `PubMedEvidenceService` with low coverage.

**Add**:
- `PubMedEvidenceServiceIT` — integration test with mocked HTTP client covering:
  - Successful PubMed query with term parsing
  - Empty result set handling
  - Rate limit response handling
  - Malformed XML response handling
  - Timeout recovery

### 3. Improve documents module test coverage (8% → 50%+)

**Current**: Document parsing and search have minimal coverage.

**Add**:
- `DocumentParserIT` — integration test for PDF and JSONL parsing:
  - PDF text extraction with embedded content
  - JSONL batch parsing with metadata
  - Empty/truncated file handling
  - SHA-256 deduplication verification
- `DocumentSearchServiceIT` — integration test for semantic search:
  - Document chunk embedding and search
  - Ranking verification
  - Empty corpus handling

### 4. Improve retrieval module test coverage (30% → 55%+)

**Add**:
- `RetrievalScoringIT` — integration test for hybrid scoring:
  - Vector similarity scoring edge cases
  - Graph traversal scoring with empty graph
  - Historical scoring with no prior data
  - Configurable fusion weight verification

### 5. Application configuration hardening

**Current**: `application.yml` uses `localhost:5433` as default DB URL. Some properties lack documentation.

**Add**:
- Verify all `@ConfigurationProperties` classes have `@Validated` annotations
- Add JSR-303 validation constraints on all configuration properties
- Add `application-prod.yml` profile with production-appropriate defaults:
  - Connection pool sizing tuned for production
  - Timeout values appropriate for cloud environments
  - Logging level `WARN` for production
- Document all environment variables in `application.yml` comments

### 6. Graceful shutdown hardening

**Current**: 30s graceful shutdown configured. No in-flight request tracking.

**Add**:
- `RequestInFlightCounter` — tracks active API requests via `Filter` + `AtomicInteger`
- `GracefulShutdownListener` — logs in-flight count during shutdown, waits for requests to drain
- Add `AvailabilityChangeEvent` for readiness probes

### 7. Update CHANGELOG and documentation

- Add M51 entry to CHANGELOG.md
- Update test count in milestone table
- Verify AGENTS.md references are current

## Success Criteria

- [ ] 4 new health indicators added and passing
- [ ] Evidence module coverage ≥ 60%
- [ ] Documents module coverage ≥ 50%
- [ ] Retrieval module coverage ≥ 55%
- [ ] `application-prod.yml` created with production defaults
- [ ] Graceful shutdown with in-flight request tracking
- [ ] All `@ConfigurationProperties` classes have validation constraints
- [ ] `mvn verify` passes with zero failures
- [ ] Overall instruction coverage ≥ 72%
- [ ] CHANGELOG updated
