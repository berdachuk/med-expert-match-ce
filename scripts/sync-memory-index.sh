#!/usr/bin/env bash
#
# sync-memory-index.sh — regenerate memory-bank Markdown index files from
# append-only JSONL registries + record files.
#
# Why: two agents editing the same index file (decisions.md, activeContext.md,
# progress.md, plans/00-index.md, productContext traceability table) is the
# main source of merge conflicts. By making the indexes GENERATED (not hand
# edited), regeneration is deterministic — two agents produce identical output
# and no merge conflict is possible.
#
# Usage:
#   ./scripts/sync-memory-index.sh            # regenerate all indexes
#   ./scripts/sync-memory-index.sh --check     # exit 1 if indexes are stale (CI)
#
# Requires: bash 4+, jq (https://stedolan.github.io/jq/)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
MB="$ROOT/.agents/memory-bank"
REG="$MB/registry"
PLANS="$ROOT/.agents/plans"

CHECK_ONLY=0
[[ "${1:-}" == "--check" ]] && CHECK_ONLY=1

command -v jq >/dev/null 2>&1 || { echo "ERROR: jq is required (apt-get install jq)" >&2; exit 2; }

GENERATED="$MB/tmp/generated"
mkdir -p "$GENERATED"
cleanup() { rm -rf "$MB/tmp"; }
trap cleanup EXIT

# Sort glob results safely descending (filenames are M###.md, no spaces).
sorted_glob_desc() { # glob
  local f files=()
  for f in "$1"; do [[ -e "$f" ]] && files+=("$f"); done
  printf '%s\n' "${files[@]}" | sort -r
}

# ---------------------------------------------------------------------------
# 1. decisions.md  (generated from dec.jsonl)
# ---------------------------------------------------------------------------
gen_decisions() {
  local out="$GENERATED/decisions.md"
  {
    echo "# Decisions"
    echo
    echo "ADR-style decision log. Each entry: status, date, title, rationale, affected modules."
    echo
    echo "> **GENERATED** from \`registry/dec.jsonl\` by \`scripts/sync-memory-index.sh\`. Do not hand-edit. To add a decision, append a line to \`registry/dec.jsonl\` and create \`records/decisions/DEC-###.md\`."
    echo
    echo "## ID Convention"
    echo
    echo "- All entries use the project-wide \`DEC-###\` prefix defined in \`.agents/skills/bdd-traceability/SKILL.md\`."
    echo "- Historical \`D-###\` references (D-001 … D-013) are **immutable aliases** — they keep the same numeric suffix under the new prefix. They are not separate decisions."
    echo
    echo "## Active decisions"
    echo
    jq -r 'select(.status != "Superseded") | "### \(.id): \(.title)\n- **Status:** \(.status)\n- **Date:** \(.date)\n- **Module:** \(.module)\n- **Affects:** \(.affects | join(", "))\n- **Rationale:** \(.rationale)\n- **Reference:** \(.ref)\n"' "$REG/dec.jsonl"
  } > "$out"
}

# ---------------------------------------------------------------------------
# 2. progress.md  (timestamped log from records/progress/*.md, sorted desc)
# ---------------------------------------------------------------------------
gen_progress() {
  local out="$GENERATED/progress.md"
  {
    echo "# Progress"
    echo
    echo "Timestamped log of completed work. **GENERATED** from \`records/progress/*.md\` by \`scripts/sync-memory-index.sh\`. Do not hand-edit."
    echo
    echo "Each completed milestone has a record file under \`records/progress/M{NN}.md\`. To log a completed milestone, create that file and re-run this script."
    echo
    echo "## Milestones (most recent first)"
    echo
    if compgen -G "$MB/records/progress/M*.md" >/dev/null; then
      while IFS= read -r f; do
        local mnum
        mnum="$(basename "$f" .md)"
        echo "### $mnum"
        echo
        echo "- [full record](records/progress/$mnum.md)"
        echo
        grep -m1 -vE '^\s*(#|\s*$)' "$f" 2>/dev/null | sed 's/^/- Summary: /' || true
        echo
      done < <(sorted_glob_desc "$MB/records/progress/M"*.md)
    else
      echo "_No milestone records yet._"
      echo
    fi
    echo "## Historical (M01–M83)"
    echo
    echo "All milestones M01–M83 are complete and archived. See \`.agents/plans/archive/\` for full history and \`.agents/plans/progress.txt\` for the detailed per-story iteration log."
  } > "$out"
}

# ---------------------------------------------------------------------------
# 3. activeContext.md  (Current Focus from records/active/, Risks from risk.jsonl)
# ---------------------------------------------------------------------------
gen_active_context() {
  local out="$GENERATED/activeContext.md"
  {
    echo "# Active Context"
    echo
    echo "> **GENERATED** from \`records/active/*.md\` + \`registry/risk.jsonl\`. Do not hand-edit. To start a milestone, create \`records/active/M{NN}.md\`; to raise a risk, append to \`registry/risk.jsonl\`."
    echo
    echo "## Current Focus"
    echo
    if compgen -G "$MB/records/active/M*.md" >/dev/null; then
      for f in "$MB/records/active/"M*.md; do
        local mnum
        mnum="$(basename "$f" .md)"
        echo "### $mnum"
        echo
        cat "$f"
        echo
      done
    else
      echo "_No active milestones._"
      echo
    fi
    echo "## Open Questions"
    echo
    echo "_Captured per-milestone in \`records/active/M{NN}.md\`._"
    echo
    echo "## Risks"
    echo
    if [[ -f "$REG/risk.jsonl" ]] && [[ -s "$REG/risk.jsonl" ]]; then
      jq -r 'select(.status != "closed") | "- **\(.id)** — \(.title) (\(.status), module: \(.module)) — mitigation: \(.mitigation // "none")"' "$REG/risk.jsonl" 2>/dev/null
      # If jq produced no output (all closed), show none
      if ! jq -e 'any(.status != "closed")' "$REG/risk.jsonl" >/dev/null 2>&1; then
        echo "_None active._"
      fi
    else
      echo "_None active._"
    fi
    echo
    echo "## Traceability Gaps"
    echo
    local gaps
    gaps="$(jq -r 'select(.status != "verified") | .id' "$REG/scn.jsonl" 2>/dev/null || true)"
    if [[ -n "$gaps" ]]; then
      echo "$gaps" | sed 's/^/- /'
    else
      echo "No remaining traceability gaps."
    fi
  } > "$out"
}

# ---------------------------------------------------------------------------
# 4. productContext.md traceability tables (generated from registries)
# ---------------------------------------------------------------------------
gen_product_context_tables() {
  local out="$GENERATED/productContext.tables.md"
  {
    echo "### Use cases"
    echo
    echo "| Use Case | REQ-### | Owning module | Primary domain models | Verified test artifact (TEST-###) | Status |"
    echo "|---|---|---|---|---|---|"
    jq -r --slurpfile scn "$REG/scn.jsonl" --slurpfile test "$REG/test.jsonl" '
      . as $r |
      ([ $scn[] | select(.reqRefs[]? | . == $r.id) | .testRefs[]? ] | unique) as $tids |
      ([ $test[] | select(.id as $tid | $tids | index($tid)) | "\(.class)\(.method // "" | if . == "" then "" else "#\(.)" end)" ]) as $tart |
      "| \($r.title) | \($r.id) | \($r.module) | \($r.domainModels | join(", ")) | \($tart | join("; ")) | \($r.status) |"
    ' "$REG/req.jsonl"
    echo
    echo "### Agent skills → scenarios"
    echo
    echo "Each skill gets a single seed \`SCN-###\` row. Status is **verified** if a test class carries a \`SCN-###\` javadoc/\`@DisplayName\` annotation; **provisional** otherwise."
    echo
    echo "| Skill | SCN-### | Owning module | Primary outcome (business language) | Status | Feature file |"
    echo "|---|---|---|---|---|---|---|"
    jq -r '"| \(.skill) | \(.id) | \(.module) | \(.title) | \(.status) | \(.featureFile // "—") |"' "$REG/scn.jsonl"
  } > "$out"
}

# ---------------------------------------------------------------------------
# 5. plans/00-index.md  (Active / Deferred / Archive from records dirs)
# ---------------------------------------------------------------------------
gen_plans_index() {
  local out="$GENERATED/00-index.md"
  {
    echo "# Milestone Plan Index"
    echo
    echo "> **GENERATED** from \`records/active/\`, \`records/deferred/\`, \`records/progress/\` by \`scripts/sync-memory-index.sh\`. Do not hand-edit."
    echo
    echo "## Active"
    echo
    if compgen -G "$MB/records/active/M*.md" >/dev/null; then
      echo "| # | Plan | Description |"
      echo "|---|------|-------------|"
      for f in "$MB/records/active/"M*.md; do
        local mnum desc
        mnum="$(basename "$f" .md)"
        desc="$(grep -m1 -vE '^\s*(#|\s*$)' "$f" 2>/dev/null | cut -c1-80 || true)"
        echo "| $mnum | [\`$mnum.md\`](../memory-bank/records/active/$mnum.md) | $desc |"
      done
    else
      echo "None — no active milestones."
    fi
    echo
    echo "## Deferred"
    echo
    if compgen -G "$MB/records/deferred/M*.md" >/dev/null; then
      echo "| # | Plan | Description |"
      echo "|---|------|-------------|"
      for f in "$MB/records/deferred/"M*.md; do
        local mnum desc
        mnum="$(basename "$f" .md)"
        desc="$(grep -m1 -vE '^\s*(#|\s*$)' "$f" 2>/dev/null | cut -c1-80 || true)"
        echo "| $mnum | [\`$mnum.md\`](../memory-bank/records/deferred/$mnum.md) | $desc |"
      done
    else
      echo "None."
    fi
    echo
    echo "## Archive"
    echo
    echo "| # | Plan | Description |"
    echo "|---|------|-------------|"
    if compgen -G "$MB/records/progress/M*.md" >/dev/null; then
      while IFS= read -r f; do
        local mnum desc
        mnum="$(basename "$f" .md)"
        desc="$(grep -m1 -vE '^\s*(#|\s*$)' "$f" 2>/dev/null | cut -c1-80 || true)"
        echo "| $mnum | [\`$mnum.md\`](../memory-bank/records/progress/$mnum.md) | $desc |"
      done < <(sorted_glob_desc "$MB/records/progress/M"*.md)
    fi
    echo
    echo "_Legacy plan files in \`.agents/plans/archive/\` are not yet migrated to \`records/progress/\`; see the existing \`00-index.md\` archive table for the full historical list until migration completes._"
  } > "$out"
}

# ---------------------------------------------------------------------------
# Run generators
# ---------------------------------------------------------------------------
gen_decisions
gen_progress
gen_active_context
gen_product_context_tables
gen_plans_index

# ---------------------------------------------------------------------------
# --check: compare generated vs on-disk; exit 1 if stale
# ---------------------------------------------------------------------------
if [[ "$CHECK_ONLY" -eq 1 ]]; then
  stale=0
  # pairs: "on-disk-absolute-path:generated-absolute-path:display-name"
  for pair in \
    "$MB/decisions.md:$GENERATED/decisions.md:decisions.md" \
    "$MB/progress.md:$GENERATED/progress.md:progress.md" \
    "$MB/activeContext.md:$GENERATED/activeContext.md:activeContext.md" \
    "$PLANS/00-index.md:$GENERATED/00-index.md:plans/00-index.md"
  do
    on_disk="${pair%%:*}"
    rest="${pair#*:}"
    gen="${rest%%:*}"
    name="${rest##*:}"
    if ! diff -q "$on_disk" "$gen" >/dev/null 2>&1; then
      echo "STALE: $name is out of sync with registries. Run ./scripts/sync-memory-index.sh" >&2
      stale=1
    fi
  done
  # productContext tables: the generated tables must match the tail of the file
  if ! diff <(sed -n '/^### Use cases/,$p' "$MB/productContext.md") "$GENERATED/productContext.tables.md" >/dev/null 2>&1; then
    echo "STALE: productContext.md traceability tables out of sync. Run ./scripts/sync-memory-index.sh" >&2
    stale=1
  fi
  exit "$stale"
fi

# ---------------------------------------------------------------------------
# Apply: overwrite generated index files
# ---------------------------------------------------------------------------
cp "$GENERATED/decisions.md"      "$MB/decisions.md"
cp "$GENERATED/progress.md"       "$MB/progress.md"
cp "$GENERATED/activeContext.md"  "$MB/activeContext.md"
cp "$GENERATED/00-index.md"       "$PLANS/00-index.md"

# productContext.md: keep the prose before "### Use cases", replace the rest.
if grep -q '^### Use cases' "$MB/productContext.md"; then
  {
    sed -n '1,/^### Use cases/p' "$MB/productContext.md" | sed '${/^### Use cases$/d}'
    cat "$GENERATED/productContext.tables.md"
  } > "$MB/productContext.md.new"
else
  # Fresh file with no tables yet: append a separator then the tables
  {
    cat "$MB/productContext.md"
    echo
    cat "$GENERATED/productContext.tables.md"
  } > "$MB/productContext.md.new"
fi
mv "$MB/productContext.md.new" "$MB/productContext.md"

echo "Memory-bank indexes regenerated:"
echo "  - decisions.md"
echo "  - progress.md"
echo "  - activeContext.md"
echo "  - productContext.md (traceability tables)"
echo "  - plans/00-index.md"