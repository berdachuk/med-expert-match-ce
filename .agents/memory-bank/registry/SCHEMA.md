# Registry Schemas

Append-only JSONL registries are the single source of truth for stable IDs
(`REQ-###`, `DEC-###`, `SCN-###`, `TEST-###`, `RISK-###`, `TASK-###`).
One JSON object per line. The Markdown index files
(`decisions.md`, `productContext.md` traceability table, `activeContext.md`,
`progress.md`, `plans/00-index.md`) are **generated** from these registries —
never hand-edit them.

## Allocation rule (multi-agent safe)

To mint a new ID:

1. Read the registry file (`registry/<kind>.jsonl`).
2. Compute `nextId = max(existing.<kindPrefix>) + 1`, zero-padded to 3 digits.
3. Append exactly **one** new line. Do not edit existing lines.
4. If `git pull` / merge reports a conflict on the file, the losing side must
   re-read, recompute `nextId`, and re-append. Because each entry is a single
   line, conflicts collapse to "who owns the last line" and are trivially
   resolvable — no semantic ID collision is possible.

## File layout

```
registry/
├── SCHEMA.md       # this file
├── req.jsonl       # REQ-###  functional requirements
├── nfr.jsonl       # NFR-###  non-functional requirements
├── scn.jsonl       # SCN-###  executable behavior scenarios
├── test.jsonl      # TEST-### test artifacts (class#method)
├── dec.jsonl        # DEC-###  decisions (legacy D-### are immutable aliases)
├── risk.jsonl       # RISK-### known risks
└── task.jsonl      # TASK-### plan tasks
```

## Schemas

### req.jsonl / nfr.jsonl

```json
{"id":"REQ-001","title":"Specialist Matching","module":"retrieval","status":"active",
 "domainModels":["MedicalCase","Doctor","DoctorMatch","ScoreResult"],
 "created":"2026-06-13","supersededBy":null}
```

Required: `id`, `title`, `module`, `status` (`active`|`superseded`|`dropped`),
`created`.

### scn.jsonl

```json
{"id":"SCN-001","title":"case-analyzer extracts entities and urgency",
 "skill":"case-analyzer","module":"caseanalysis","reqRefs":["REQ-005"],
 "testRefs":["TEST-005"],"status":"verified","featureFile":"features/case-analyzer.feature"}
```

Required: `id`, `title`, `module`, `reqRefs`, `status`
(`verified`|`provisional`|`dropped`).

### test.jsonl

```json
{"id":"TEST-001","class":"retrieval/service/MatchingServiceIT","method":null,
 "module":"retrieval","scnRefs":["SCN-002"],"reqRefs":["REQ-001"],"added":"2026-06-13"}
```

`method` may be `null` (whole class) or a method name.

### dec.jsonl

```json
{"id":"DEC-015","title":"Multi-agent memory partitioning","status":"Accepted",
 "date":"2026-06-21","module":"core","rationale":"...",
 "affects":["core",".agents/memory-bank"],
 "ref":"records/decisions/DEC-015.md","legacyAlias":null}
```

Required: `id`, `title`, `status`
(`Accepted`|`Proposed`|`Deprecated`|`Superseded`), `date`, `module`.
`legacyAlias` carries the old `D-###` form for DEC-001..013 only.
`ref` points to the long-form record under `records/decisions/`.

### risk.jsonl

```json
{"id":"RISK-132","title":"Short-key/long-key drift in LlmResponseSanitizer",
 "status":"mitigated","module":"core","mitigation":"dual-key fallback + parity tests",
 "created":"2026-06-21","decRefs":["DEC-015"]}
```

Required: `id`, `title`, `status`
(`open`|`mitigated`|`accepted`|`closed`), `module`, `created`.

### task.jsonl

```json
{"id":"TASK-001","milestone":"M133","title":"migrate decisions.md to dec.jsonl",
 "module":"core","status":"done","plan":"records/active/M133.md","assignedTo":null}
```

`assignedTo` is a branch name or agent session id; `null` = unassigned.

## Generation

`scripts/sync-memory-index.sh` walks these files and regenerates:

- `decisions.md` — "Active decisions" section from `dec.jsonl` (status != Superseded).
- `productContext.md` — traceability tables from `req.jsonl` + `scn.jsonl` + `test.jsonl`.
- `activeContext.md` — "Current Focus" from `records/active/*.md`; "Risks" from `risk.jsonl`.
- `progress.md` — timestamped log from `records/progress/*.md` (sorted by milestone number).
- `plans/00-index.md` — Active/Deferred/Archive tables from `records/active/`,
  `records/deferred/`, `records/progress/`.

If two agents regenerate the index, the output is deterministic and identical,
so regeneration never produces a merge conflict.