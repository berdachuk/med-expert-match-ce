# Skill: token-efficient-format

## Purpose

Choose the **cheapest format that still supports safe parsing** for the current call, and shape both prompt and schema to minimize tokens (especially in structured-output flows).

## Inputs considered

For each call, evaluate:

- `needs_schema_validation`: strict typed mapping in Spring AI / JSON schema required?
- `structure_shape`: flat list, table, simple key–value map, or deep nested object graph?
- `downstream_parsers`: Java DTO + Jackson / BeanOutputConverter, or simple string split?
- `volume`: expected object count per response and call frequency (high vs low).
- `provider_capabilities`: JSON mode / structured output available or not.

## Decision rules

### 1. When to use full JSON (standard structured output)

Use **standard JSON structured output** when all are true:

- The result is consumed by Java DTOs, BeanOutputConverter, or JSON schema validation.
- You care about correctness and reusability more than absolute minimal tokens.
- Structure is moderately or deeply nested (objects with arrays, nested records).
- You can use provider-native JSON / structured output modes (`response_format=json_object` or JSON schema).

**Format contract**

- Enforce "JSON-only" veto in the system/instructions: no prose, no markdown.
- Use **short keys** (`id`, `sc`, `msg`, `cats`) instead of long ones (`identifier`, `score`, `message`, `categories`).
- Omit optional fields unless needed; avoid long descriptions in the schema.
- Prefer enums and simple codes over open-ended text.

Example (LLM output):

```json
{
  "items": [
    {"id": "u1", "sc": 0.91, "cat": "OK"},
    {"id": "u2", "sc": 0.41, "cat": "WARN"}
  ]
}
```

In Spring AI keep internal DTOs verbose and have a "wire DTO" with compact keys for the LLM.

### 2. When to use TOON format

> **STATUS: UNIMPLEMENTED — DO NOT SELECT.**
> No TOON→JSON adapter exists in this codebase; the M127 plan explicitly deferred it.
> Selecting TOON produces output no Java parser can read. For nested structure without a
> schema, use **ultra-compact JSON** (§3) instead. TOON is retained here as a future option
> pending the adapter; it must not be chosen until that adapter lands.

Use **TOON (Token-Oriented Object Notation)** when (all of the below, **plus** the TOON→JSON adapter exists):

- You need structured output with hierarchy (nested objects, arrays).
- You do **not** need strict JSON schema validation or BeanOutputConverter.
- You want significant token savings (~60% vs JSON) while preserving structure.
- You can parse indentation-based format on the Java side (similar to YAML parsing).

TOON eliminates JSON's redundant syntax (braces, brackets, quotes, commas) and uses indentation for hierarchy. Uniform arrays become tabular blocks.

**Format contract**

- Indentation (2 spaces) represents nesting depth.
- Keys and values separated by `: ` (colon + space).
- Uniform arrays rendered as tabular blocks with a header row.
- No quotes around string values unless they contain `:` or leading/trailing whitespace.
- No trailing commas, no braces, no brackets.

Example (LLM output):

```text
items:
  id: u1
  sc: 0.91
  cat: OK
  id: u2
  sc: 0.41
  cat: WARN
```

For nested objects:

```text
patient:
  id: P-001
  name: Jane Doe
  conditions:
    code: I10
    label: Essential hypertension
    onset: 2023-01
    code: E11.9
    label: Type 2 diabetes
    onset: 2022-06
```

**Java-side parsing**: write a lightweight TOON parser (indentation-stack + line-split) or convert TOON → JSON via a pre-processing step before Jackson deserialization. TOON is not a Jackson format, so it requires a thin adapter layer. **This adapter does not exist yet** — see the STATUS banner above.

### 3. When to use ultra-compact JSON

Use **ultra-compact JSON** (no whitespace, very short keys) when:

- You still need JSON parsing/validation (Jackson, BeanOutputConverter).
- Responses are large (e.g., arrays of many items).
- You can control both sides of the contract (no human readability requirement).

Guidelines:

- No pretty printing or line breaks (model usually follows if you show a single-line example).
- Keys ≤ 3–4 chars where feasible.
- Represent booleans and enums as single characters or digits where safe (`"s":"H"` instead of `"severity":"HIGH"`).
- This is the **recommended** format for nested structure consumed by `Map.class` parsing (no BeanOutputConverter / JSON schema), since TOON has no adapter.

**Sanitizer coupling (critical):** if you change the keys of a JSON prompt to short keys, you **must** update `LlmResponseSanitizer.FIELD_LABELS` and `LlmResponseSanitizer.JSON_BLOCK_PATTERN` (currently at `core/util/LlmResponseSanitizer.java:34,62`) in the same change — both hardcode the long key names (`requiredSpecialty`, `urgencyLevel`, `clinicalFindings`, `icd10Codes`, `caseSummary`). A short-key prompt without a matching sanitizer update silently breaks JSON-block → prose rendering. Any non-standard (non-JSON) format must also round-trip through `LlmResponseSanitizer.extractJson` so sanitization, PHI stripping, and JSON-block rendering stay consistent.

### 4. When to use CSV / TSV / minimal tabular format

Use **CSV/TSV/tabular text** when:

- Records are flat (same columns for each row).
- Downstream parsing can handle a simple split/tokenizer.
- You want **maximum token efficiency**; JSON overhead (quotes, braces, commas, keys) is a significant part of the cost.
- You do not need JSON schema validation on the output, only type-safe ingestion in your code.

Typical pattern:

- First line: header with short column identifiers.
- Following lines: values, one record per line.
- Separator: `,` for CSV, `\t` for TSV, or `|` if that's easier to parse robustly.

Example:

```text
id,sc,cat
u1,0.91,OK
u2,0.41,WARN
```

### 5. When to use key:value or line-based lists

Use **line-based key:value** or simple lists when:

- You only need 1D structures: lists of strings, numbers, ids, or simple pairs.
- The response will be post-processed by a very small, deterministic parser.

Patterns:

- Pure list: `item1|item2|item3` or each item on its own line.
- Simple mapping: `id=sc|cat` or `id:sc:cat` on each line.

Example:

```text
u1|0.91|OK
u2|0.41|WARN
```

### 6. When *not* to use structured format for output

Consider **unstructured or lightly-structured text** when:

- You need rich explanation or natural language commentary that cannot be easily captured in a strict schema.
- You are exploring or prototyping and cost is low relative to developer time.
- You plan to manually read the response more than you programmatically process it.

In those cases, focus token reduction on **input** (shorter instructions, less context, summary references) instead of forcing everything into a schema.

## Decision table (quick reference)

| Need                                          | Recommended format         | Why                                                       |
|-----------------------------------------------|----------------------------|-----------------------------------------------------------|
| DTO via BeanOutputConverter / provider schema | JSON (structured output)   | Provider-native support, robust parsing.                  |
| Map-parsed, no schema, nested structure       | Ultra-compact JSON         | Implemented; Jackson-safe; keys/whitespace minimized.     |
| Nested structure, no schema (TOON adapter)    | TOON (future)              | ~60% token reduction — **blocked: no adapter exists**.   |
| Large arrays, still need JSON                 | Ultra-compact JSON         | Keys and whitespace minimized.                            |
| Flat tables, high volume                       | CSV/TSV/tabular            | Minimal syntax overhead per row.                          |
| Simple lists or pairs                          | Line-based / delimited     | Cheapest possible overhead.                               |
| Human-readable analysis, low volume            | Semi-structured text       | Dev convenience > token savings.                          |

## Skill instructions (for AGENTS.md)

Embed this as a concise policy in root or module AGENTS:

- Before defining response format, classify the output as:
  - `type=object_deep`, `object_flat`, `table`, or `list`.
- If `type in {object_deep, object_flat}` and result is consumed by Spring AI structured output / JSON schema, choose JSON with short keys and no extra commentary.
- If `type in {object_deep, object_flat}` and no JSON schema validation is required, prefer **ultra-compact JSON** with short keys (it is Jackson-parseable via `Map.class`). Do NOT choose TOON — no TOON→JSON adapter exists.
- If `type=table` and many rows are expected, choose CSV/TSV with short headers.
- If `type=list` and items are atomic, choose `|`-delimited or line-delimited output.
- Always:
  - Remove greetings, explanations, and markdown wrappers from model output.
  - Prefer compact keys and values over verbose ones.
  - Keep the chosen format stable per endpoint so Java-side parsing is trivial.
  - When changing JSON prompt keys, update `LlmResponseSanitizer` field labels / patterns in lockstep (see §3 coupling note).

## 7. Input-side token reduction

Output format is only half the cost. Reduce **input** tokens for frequently called prompts:

- **Shared boilerplate**: the medical disclaimer is duplicated verbatim across ~10 `.st` files and the `CRITICAL OUTPUT LIMITS` block across 4 prose prompts. Extract these into a single shared fragment and include it via Spring AI resource composition, or hoist them into a once-per-conversation system prompt so per-call prompts shrink.
- **Shorten repeated limits**: prefer one compact line (`Output: 1 JSON object, ≤3000 chars, then stop.`) over the multi-bullet `CRITICAL OUTPUT LIMITS` block; the model obeys single-line constraints as well or better.
- **Summarize context**: when injecting case context / evidence, pass a compact summary reference instead of the full payload where the downstream step only needs key fields.
- **Trim verbose keys in input payloads too**: the same short-key discipline applies to any structured input you compose for the model.
