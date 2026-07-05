# Development pipeline

The `docs/pipeline/` directory holds the canonical, numbered engineering documents that form the source of truth for what is built, how it is designed, how it is tested, and how it is deployed. Each doc follows a recognized standard and links to its predecessor and successor.

| Step | Document | Standard | Use for |
|---|---|---|---|
| — | **[01-requirements.md](01-requirements.md)** | SRS | **Source of truth** — what to build: dataset, MCP surface, NFRs, milestones |
| 1 | **[02-architecture.md](02-architecture.md)** | SAD | System context, Modulith modules, stack, design decisions |
| 2 | **[03-design.md](03-design.md)** | SDD | Schema, domain records, service/repository APIs, MCP class sketches |
| 3 | **[04-testing.md](04-testing.md)** | Test plan | Unit/integration/quality tests, CSV split discipline, CI gates |
| 4 | **[05-deployment.md](05-deployment.md)** | Ops guide | `application.yml`, env vars, Docker, MCP client config |

## Pipeline rules

- **Numbering is stable**: `01-` through `05-` are reserved. Additional docs use `06-`, `07-`, etc. Never renumber existing docs.
- **Each doc links to its predecessor and successor**: `02-architecture.md` references `01-requirements.md` (which requirements it satisfies) and `03-design.md` (which design realizes it).
- **Memory-bank summarizes, docs contain the full spec**: the memory-bank files link to these pipeline docs as the deep reference and never duplicate them.

---

*Last updated: 2026-06-30*