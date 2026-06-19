# M126: Next Implementation Phase

**Status:** Active  
**Date:** 2026-06-19  
**IDs:** REQ-126

---

## 1. Goal

Consolidate platform stability and address deferred M124 tasks (performance, monitoring, ops docs). No new feature development.

---

## 2. Tasks

### 2.1 GraphRAG query profiling
- Profile and document Cypher query latency in `GraphQueryServiceImpl`
- Add comment annotations for expected index coverage
- Benchmark hybrid retrieval latency

### 2.2 Monitoring enhancements
- Verify Grafana dashboard panels for GraphRAG query latency
- Ensure Prometheus metrics instrumentation for retrieval pipeline stages (add Micrometer `@Timed` annotations where missing)

### 2.3 Operations documentation
- Create `docs/DEPLOYMENT_RUNBOOK.md` covering docker-compose and k8s deployment
- Document PostgreSQL + AGE backup/restore procedures in `docs/OPS_GUIDE.md`

### 2.4 Update `00-index.md` — register M126

---

## 3. Acceptance Criteria

- [ ] GraphRAG query characteristics documented
- [ ] Monitoring panels verified for retrieval pipeline
- [ ] Deployment runbook covers docker-compose and k8s
- [ ] Backup/restore procedures documented
- [ ] `mvn verify` passes
- [ ] No regressions

---

## 4. Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Profiling reveals no bottleneck | Low — still provides documentation value | Accept documentation output |
| Monitoring dashboards already complete | Low — skip or note as verified | Mark task as verified |

---

## 5. Files Changed

| File | Change |
|------|--------|
| `docs/DEPLOYMENT_RUNBOOK.md` | Create |
| `docs/OPS_GUIDE.md` | Create |
| `retrieval/service/impl/SemanticGraphRetrievalServiceImpl.java` | Add @Timed annotations |
| `.agents/plans/00-index.md` | Register M126 |