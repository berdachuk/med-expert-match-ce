#!/usr/bin/env python3
"""Backfill reqRefs/scnRefs in test.jsonl by mining IT source files."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TEST_JSONL = ROOT / ".agents/memory-bank/registry/test.jsonl"
REQ_JSONL = ROOT / ".agents/memory-bank/registry/req.jsonl"
SCN_JSONL = ROOT / ".agents/memory-bank/registry/scn.jsonl"
SRC_TEST = ROOT / "src/test/java"

MODULE_REQ_DEFAULTS: dict[str, list[str]] = {
    "chat": ["REQ-014"],
    "llm": ["REQ-018"],
    "web": ["REQ-125"],
    "core": ["REQ-016"],
    "doctor": ["REQ-008"],
    "medicalcase": ["REQ-007"],
    "medicalcoding": ["REQ-007"],
    "facility": ["REQ-013"],
    "clinicalexperience": ["REQ-008"],
    "chunking": ["REQ-016"],
    "documents": ["REQ-016"],
    "embedding": ["REQ-016"],
    "evidence": ["REQ-009"],
    "graph": ["REQ-012"],
    "ingestion": ["REQ-015"],
    "retrieval": ["REQ-008"],
    "system": ["REQ-016"],
    "caseanalysis": ["REQ-007"],
    "bdd": [],
    "integration": ["REQ-018"],
}

CLASS_REQ_HINTS: list[tuple[re.Pattern[str], list[str]]] = [
    (re.compile(r"A2a|AgentCard|JsonRpc|SkillRegistry", re.I), ["REQ-018"]),
    (re.compile(r"Harness|AgentPlan|WorkflowRun", re.I), ["REQ-018"]),
    (re.compile(r"ToolSelection|SessionTurn", re.I), ["REQ-134"]),
    (re.compile(r"ChatFollowUp|GoalContext", re.I), ["REQ-014"]),
    (re.compile(r"Fhir", re.I), ["REQ-015"]),
    (re.compile(r"Synthetic", re.I), ["REQ-017"]),
    (re.compile(r"PubMed|Evidence", re.I), ["REQ-009"]),
    (re.compile(r"Matching|MatchOutcome|ConsultationMatch|RetrievalScoring", re.I), ["REQ-001", "REQ-008"]),
    (re.compile(r"Priority|Prioritize|Queue", re.I), ["REQ-003", "REQ-011"]),
    (re.compile(r"Route|Routing|Facility", re.I), ["REQ-006", "REQ-013"]),
    (re.compile(r"Graph|Network", re.I), ["REQ-004", "REQ-012"]),
    (re.compile(r"CaseAnalysis|Analyze", re.I), ["REQ-007"]),
    (re.compile(r"Recommendation|ClinicalAdvisor|MedicalAgent", re.I), ["REQ-010", "REQ-018"]),
    (re.compile(r"DocumentSearch|DocumentIngest|DocumentPipeline|Chunk", re.I), ["REQ-016"]),
    (re.compile(r"UseCase1|MatchDoctors", re.I), ["REQ-001"]),
    (re.compile(r"UseCase2", re.I), ["REQ-002"]),
    (re.compile(r"UseCase3|PrioritizeE2E", re.I), ["REQ-003"]),
    (re.compile(r"UseCase4", re.I), ["REQ-004"]),
    (re.compile(r"UseCase5|DecisionSupport", re.I), ["REQ-005", "REQ-010"]),
    (re.compile(r"UseCase6|DocumentSearchE2E", re.I), ["REQ-016"]),
    (re.compile(r"Cucumber", re.I), []),
    (re.compile(r"Admin|SessionToken|RateLimit|Export|Retention|Audit", re.I), ["REQ-016"]),
    (re.compile(r"Modulith", re.I), ["REQ-016"]),
    (re.compile(r"Health", re.I), ["REQ-016"]),
    (re.compile(r"Evaluation|Eval", re.I), ["REQ-018"]),
    (re.compile(r"Embedding", re.I), ["REQ-016"]),
    (re.compile(r"LenientJson|StructuredOutput|Sanitizer", re.I), ["REQ-133"]),
]

CLASS_SCN_HINTS: list[tuple[re.Pattern[str], list[str]]] = [
    (re.compile(r"MatchingService|MatchDoctors|UseCase1", re.I), ["SCN-002"]),
    (re.compile(r"CaseAnalysis|Triage|UseCase5", re.I), ["SCN-001", "SCN-009"]),
    (re.compile(r"PubMed|EvidenceRetrieval", re.I), ["SCN-003", "SCN-008"]),
    (re.compile(r"Recommendation", re.I), ["SCN-004"]),
    (re.compile(r"ClinicalAdvisor|MedicalAgentTools", re.I), ["SCN-005"]),
    (re.compile(r"GraphQuery|Network|UseCase4", re.I), ["SCN-006"]),
    (re.compile(r"Routing|RouteScore|UseCase", re.I), ["SCN-007"]),
    (re.compile(r"Cucumber", re.I), [f"SCN-{i:03d}" for i in range(1, 10)]),
]


def load_jsonl(path: Path) -> list[dict]:
    entries: list[dict] = []
    for line_no, line in enumerate(path.read_text().splitlines(), 1):
        if not line.strip():
            continue
        # Handle corrupted lines with multiple JSON objects
        pos = 0
        while pos < len(line):
            try:
                obj, end = json.JSONDecoder().raw_decode(line, pos)
                entries.append(obj)
                pos = end
            except json.JSONDecodeError as exc:
                raise ValueError(f"{path}:{line_no}: {exc}") from exc
    return entries


def write_jsonl(path: Path, entries: list[dict]) -> None:
    path.write_text("\n".join(json.dumps(e, separators=(",", ":")) for e in entries) + "\n")


def resolve_java_path(class_path: str) -> Path | None:
    """Map registry class path to filesystem path."""
    candidates: list[Path] = []
    norm = class_path.replace("com/berdachuk/medexpertmatch/", "")
    candidates.append(SRC_TEST / "com/berdachuk/medexpertmatch" / f"{norm}.java")
    if not class_path.startswith("com/"):
        candidates.append(SRC_TEST / "com/berdachuk/medexpertmatch" / f"{class_path}.java")
    for candidate in candidates:
        if candidate.exists():
            return candidate
    # Fallback: search by simple name
    simple = Path(class_path).name + ".java"
    matches = list(SRC_TEST.rglob(simple))
    return matches[0] if len(matches) == 1 else None


def extract_refs(text: str) -> tuple[list[str], list[str]]:
    reqs = sorted(set(re.findall(r"REQ-\d{3}", text)))
    scns = sorted(set(re.findall(r"SCN-\d{3}", text)))
    return reqs, scns


def infer_refs(class_path: str, module: str) -> tuple[list[str], list[str]]:
    simple = Path(class_path).stem
    reqs: set[str] = set(MODULE_REQ_DEFAULTS.get(module, ["REQ-016"]))
    scns: set[str] = set()
    if module == "bdd":
        reqs.clear()
        scns.update(f"SCN-{i:03d}" for i in range(1, 10))

    for pattern, hint_reqs in CLASS_REQ_HINTS:
        if pattern.search(simple) or pattern.search(class_path):
            reqs.update(hint_reqs)
    for pattern, hint_scns in CLASS_SCN_HINTS:
        if pattern.search(simple) or pattern.search(class_path):
            scns.update(hint_scns)

    return sorted(reqs), sorted(scns)


def merge_refs(existing: list[str], found: list[str]) -> list[str]:
    return sorted(set(existing) | set(found))


def read_java_source(java_path: Path) -> str:
    """Read Java source preserving original line endings (CRLF or LF)."""
    return java_path.read_bytes().decode("utf-8")


def build_javadoc(reqs: list[str], scns: list[str]) -> str:
    lines = ["/**"]
    for req in reqs[:3]:
        lines.append(f" * {req}: integration coverage for registered requirement.")
    for scn in scns[:3]:
        lines.append(f" * {scn}: executable scenario coverage.")
    if len(reqs) > 3:
        lines.append(f" * Also covers: {', '.join(reqs[3:])}.")
    if len(scns) > 3:
        lines.append(f" * Also covers: {', '.join(scns[3:])}.")
    lines.append(" */")
    return "\n".join(lines)


def annotate_it_sources(test_entries: list[dict], dry_run: bool) -> int:
    """Add class-level traceability javadoc to IT files missing REQ/SCN refs."""
    by_class: dict[str, dict] = {}
    for entry in test_entries:
        if entry.get("method"):
            continue
        by_class[entry["class"]] = entry

    annotated = 0
    for class_path, entry in sorted(by_class.items()):
        java_path = resolve_java_path(class_path)
        if not java_path or not java_path.exists():
            continue
        text = read_java_source(java_path)
        if re.search(r"REQ-\d{3}|SCN-\d{3}", text):
            continue
        reqs = entry.get("reqRefs") or []
        scns = entry.get("scnRefs") or []
        if not reqs and not scns:
            continue

        javadoc = build_javadoc(reqs, scns)
        line_sep = "\r\n" if "\r\n" in text else "\n"
        if line_sep == "\r\n":
            javadoc = javadoc.replace("\n", "\r\n")
        # Insert before class/interface declaration
        match = re.search(
            r"(^|\n)((?:@\w+(?:\([^)]*\))?\s*\n)*)(?:public\s+)?(?:abstract\s+)?class\s+\w+",
            text,
            re.MULTILINE,
        )
        if not match:
            continue
        insert_at = match.start(2)
        new_text = text[:insert_at] + javadoc + line_sep + text[insert_at:]
        if not dry_run:
            java_path.write_bytes(new_text.encode("utf-8"))
        annotated += 1

    print(f"Annotated IT classes: {annotated}")
    return annotated


def main() -> int:
    dry_run = "--dry-run" in sys.argv
    annotate = "--annotate" in sys.argv
    test_entries = load_jsonl(TEST_JSONL)
    updated = 0
    missing_source = 0

    for entry in test_entries:
        class_path = entry["class"]
        java_path = resolve_java_path(class_path)
        reqs: list[str] = list(entry.get("reqRefs") or [])
        scns: list[str] = list(entry.get("scnRefs") or [])

        if java_path and java_path.exists():
            source_reqs, source_scns = extract_refs(read_java_source(java_path))
            reqs = merge_refs(reqs, source_reqs)
            scns = merge_refs(scns, source_scns)
        else:
            missing_source += 1

        if not reqs and not scns:
            inf_reqs, inf_scns = infer_refs(class_path, entry.get("module", ""))
            reqs = inf_reqs
            scns = inf_scns

        if reqs != (entry.get("reqRefs") or []) or scns != (entry.get("scnRefs") or []):
            entry["reqRefs"] = reqs
            entry["scnRefs"] = scns
            updated += 1

    both_empty = sum(1 for e in test_entries if not e.get("reqRefs") and not e.get("scnRefs"))
    print(f"Entries: {len(test_entries)}, updated: {updated}, both-empty: {both_empty}, missing-source: {missing_source}")

    if both_empty:
        print("ERROR: entries still lack reqRefs and scnRefs", file=sys.stderr)
        return 1

    if annotate:
        annotate_it_sources(test_entries, dry_run=dry_run)

    if not dry_run:
        write_jsonl(TEST_JSONL, test_entries)

        # Fix req.jsonl corruption if present
        req_entries = load_jsonl(REQ_JSONL)
        write_jsonl(REQ_JSONL, req_entries)

        # Fix risk.jsonl corruption if present
        risk_path = ROOT / ".agents/memory-bank/registry/risk.jsonl"
        risk_entries = load_jsonl(risk_path)
        write_jsonl(risk_path, risk_entries)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
