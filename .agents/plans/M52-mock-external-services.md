# M52: Mock External Services in Integration Tests

**Goal**: Replace live external HTTP calls in integration tests with WireMock stubs. Establish a project-wide rule that all ITs use recorded/mocked responses for services outside project control (PubMed, NCBI, etc.).

**Context**:
- M51 added live PubMedServiceIT tests that flake when the shared IP hits NCBI rate limits (429).
- `PubMedServiceImpl` creates its own `RestTemplate` internally — not injectable, making HTTP mocking impossible from tests.
- No WireMock/MockWebServer dependency exists in the project.
- `BaseIntegrationTest` already excludes Spring HTTP client auto-configs, which is fine for WireMock usage.

## Steps

### 1. Add WireMock test dependency

Add `org.wiremock:wiremock-standalone:3.9.2` as a test-scoped dependency in pom.xml.

### 2. Refactor PubMedServiceImpl for testability

- Change constructor to accept an optional `RestTemplate` parameter (Spring constructor injection).
- Add a Spring `@Bean` for PubMed `RestTemplate` with user-agent interceptor.
- `PubMedServiceIT` can then inject a RestTemplate pointing at WireMock's base URL.

### 3. Record real PubMed API responses

- Capture actual XML/JSON responses from NCBI E-utilities for a representative query.
- Store as test resource fixtures in `src/test/resources/evidence/`.

### 4. Rewrite PubMedServiceIT with WireMock

- Set up a WireMock server in the test (random port, dynamic).
- Stub `esearch.fcgi` and `efetch.fcgi` endpoints with recorded responses.
- Wire the injected `RestTemplate` to use WireMock's base URL.
- Test: normal results, empty results, XML parse edge cases — all deterministic.

### 5. Add project rule: mock all external services in ITs

- Append to `AGENTS.md` a rule requiring ITs to mock external HTTP APIs via WireMock.
- Append to `.agents/skills/testing/SKILL.md` guidance on WireMock usage pattern.
- Include the rule: "Record real responses once, store as fixtures, mock in all ITs."

## Success Criteria

- [ ] WireMock dependency added to pom.xml
- [ ] `PubMedServiceImpl` accepts injected `RestTemplate`
- [ ] WireMock-based `PubMedServiceIT` replaces live-API test
- [ ] PubMed response fixtures stored in test resources
- [ ] Mock-external-services rule added to AGENTS.md + testing skill
- [ ] `mvn verify` passes with zero failures (no flaky PubMed 429 errors)
- [ ] 516+ tests, zero failures
