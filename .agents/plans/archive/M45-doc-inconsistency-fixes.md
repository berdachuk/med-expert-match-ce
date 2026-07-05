# M45: Documentation Inconsistency Fixes

**Goal:** Fix version mismatches, dead links, outdated deadlines, duplicate content, and structural errors across project documentation.

**Created:** 2026-06-03
**Status:** Active

---

## Issues Found

### 1. Spring AI Version Mismatch (High)

| File | Current | Expected |
|------|---------|----------|
| `docs/01-requirements.md:515` | `Spring AI 2.0.0-M2` | `Spring AI 2.0.0-M6` |
| `docs/01-requirements.md:1393` | `Spring AI 2.0.0-M2` | `Spring AI 2.0.0-M6` |
| `docs/IMPLEMENTATION_PLAN.md:61` | `Spring AI 2.0.0-M2` | `Spring AI 2.0.0-M6` |

Other docs (`02-architecture.md:510`, `MedExpertMatch.md:93`, `index.md:141`) already use M6. The actual pom.xml uses M6.

**Fix:** Replace `2.0.0-M2` → `2.0.0-M6` in 01-requirements.md (2 occurrences) and IMPLEMENTATION_PLAN.md (1 occurrence).

---

### 2. FHIR Version Inconsistency (High)

| File | Current | Expected |
|------|---------|----------|
| `docs/02-architecture.md:831` | `FHIR R4` | `FHIR R5` |
| `docs/IMPLEMENTATION_PLAN.md:785` | `FHIR R4 structure` | `FHIR R5 structure` |

01-requirements.md, INTEGRATION_PLAN.md, SYNTHETIC_DATA_GENERATOR.md consistently reference FHIR R5 (25 occurrences). Only 2 occurrences are wrong.

**Fix:** Replace `FHIR R4` → `FHIR R5` in 02-architecture.md:831 and IMPLEMENTATION_PLAN.md:785.

---

### 3. Outdated Challenge Deadline (Medium)

`docs/index.md:50` states:
> **February 24, 2026** - ~6 weeks development timeline

The deadline is 3+ months in the past. The project status already says "MVP complete" elsewhere. A frozen deadline makes docs feel stale.

**Fix:** Reword to reflect that the challenge submission was completed. Example: "Challenge submission completed February 24, 2026 (6-week development timeline)."

Also in `docs/01-requirements.md:53` — same stale deadline. Apply same fix.

---

### 4. Section Numbering Error in 01-requirements.md (Medium)

`docs/01-requirements.md` section 8 is labeled incorrectly. After section **8. API Specifications** (with sub-section 8.1 "Agent API Endpoints"), the next sub-section is labeled **7.2 Test Data API Endpoints** instead of **8.2**.

**Fix:** Rename `7.2 Test Data API Endpoints` → `8.2 Test Data API Endpoints` in 01-requirements.md.

---

### 5. "Architecture Analysis" Link Mismatch (Low)

`docs/VISION.md:327` links:
```md
- [Architecture Analysis](02-architecture.md)
```

The target file is `02-architecture.md`, which has page title "MedExpertMatch Architecture" — not "Architecture Analysis." The FIX_PLAN.md:44 previously fixed a "missing architecture analysis file" entry from mkdocs.yml navigation (the old entry is already removed from the nav).

**Fix:** Change link text from "Architecture Analysis" to "Architecture" in VISION.md:327.

---

### 6. Outdated Tense in 01-requirements.md Implementation Phases (Low)

`docs/01-requirements.md` section 9 (lines 1309–1329) describes MVP delivery as future work:
> "Complete MVP for MedGemma Impact Challenge submission"
> "Timeline: Week 1-2, Week 3, ..."

The challenge was submitted months ago. The MVP is complete (as stated in the PRD header: "Status: MVP complete").

**Fix:** Add completion markers to each phase or rephrase as retrospective (past tense). At minimum add "✅" markers so the section is clearly historical/retrospective.

---

### 7. Duplicate Content: PRESENTATION_SALES_3MIN.md Listings (Low)

`docs/PRESENTATION_SALES_3MIN.md` exists in both places:
- `docs/index.md:66` → links it under "Overview"
- `mkdocs.yml:106` → lists it under `Overview` nav
- `mkdocs.yml:93` → also listed under `Presentations` nav as a separate file (`medexpertmatch-sales-3min.md`)

The `index.md` and `mkdocs.yml` nav both list it under Overview, while the presentations subdirectory has its own version. The Presentation nav entry links to a different file (`medexpertmatch-sales-3min.md` which is a Reveal.js slideshow) than the standalone `PRESENTATION_SALES_3MIN.md` (a Markdown file with more text). These are actually two different files — the Reveal presentation and a standalone script. Both are valid but the Overview nav listing is redundant since the Presentations nav already covers sales content.

**Fix:** Remove `PRESENTATION_SALES_3MIN.md` from the Overview section in `mkdocs.yml` nav (keep it accessible from `index.md` quick links since it's there for convenience).

---

### 8. VISION.md Largely Duplicates 01-requirements.md (Low)

`docs/VISION.md` (332 lines) and `docs/01-requirements.md` (1430 lines) have nearly identical content for:
- MedGemma Impact Challenge context (same paragraph)
- Core Value Propositions (same 4 items, same wording)
- Core Use Cases section (same 6 use cases)
- Success Metrics (same 3 categories)
- Implementation Phases (same phases)

VISION.md should be a concise vision statement, not a mini-PRD. The PRD is the canonical source.

**Fix:** Strip VISION.md down to ~60 lines: keep the vision statement, purpose, MedGemma challenge alignment, differentiators, and impact statement. Remove duplicated sections and add cross-references to 01-requirements.md.

---

### 9. MedExpertMatch.md Overlaps with index.md (Low)

`docs/MedExpertMatch.md` (140 lines) and `docs/index.md` (204 lines) share:
- Same "About MedExpertMatch" description
- Same "MedGemma Impact Challenge" section
- Same "Key Features" list
- Same "Architecture Overview" / "Technology Stack"

The MedExpertMatch page is the canonical overview page per mkdocs.yml nav structure.

**Fix:** Reduce duplication between the two. Keep `index.md` as navigation hub (links, disclaimers, quick reference) and let `MedExpertMatch.md` be the comprehensive overview. Remove duplicated sections from index.md.

---

### 10. PROJECT_DESCRIPTION.md Redundant with 01-requirements.md (Low)

`docs/PROJECT_DESCRIPTION.md` (82 lines) duplicates the "Problems Solved" content also present in `PROBLEMS_SOLVED.md` and the value propositions in 01-requirements.md. It is not listed in mkdocs.yml navigation so it's a hidden/standalone page.

**Fix:** Either remove it or add a clear cross-reference header pointing to 01-requirements.md and PROBLEMS_SOLVED.md as the canonical sources. If the file serves a specific purpose (brief summary for external sharing), add a note explaining its role.

---

## Checklist

- [ ] **High:** Fix Spring AI version M2→M6 in 01-requirements.md (2 occurrences)
- [ ] **High:** Fix Spring AI version M2→M6 in IMPLEMENTATION_PLAN.md (1 occurrence)
- [ ] **High:** Fix FHIR R4→R5 in 02-architecture.md:831
- [ ] **High:** Fix FHIR R4→R5 in IMPLEMENTATION_PLAN.md:785
- [ ] **Medium:** Update stale challenge deadline in index.md:50 and 01-requirements.md:53
- [ ] **Medium:** Fix section numbering 7.2→8.2 in 01-requirements.md
- [ ] **Low:** Fix "Architecture Analysis" link text in VISION.md:327
- [ ] **Low:** Update tense in 01-requirements.md section 9 (Implementation Phases)
- [ ] **Low:** Remove duplicate PRESENTATION_SALES_3MIN.md from mkdocs.yml Overview nav
- [ ] **Low:** Strip VISION.md to ~60 lines, remove PRD duplication
- [ ] **Low:** Reduce index.md / MedExpertMatch.md overlap
- [ ] **Low:** Clean up PROJECT_DESCRIPTION.md (cross-reference or remove)

## Verification

After fixes, run:
```bash
# Validate MkDocs builds without broken links
mvn clean install -DskipTests  # or: mkdocs build --strict
# Spot-check: grep for remaining version mismatches
rg "Spring AI 2\.0\.0-M2" docs/
rg "FHIR R4" docs/02-architecture.md docs/IMPLEMENTATION_PLAN.md
```

## Related

- [FIX_PLAN.md](docs/FIX_PLAN.md) — existing tracked fixes (Phase 1 covers some alignment already)
- [02-architecture.md](docs/pipeline/02-architecture.md)
- [01-requirements.md](docs/pipeline/01-requirements.md)
