# MkDocs Setup for MedExpertMatch

**Last Updated:** 2026-01-27

## Overview

MedExpertMatch uses MkDocs Material for documentation.

## Installation

```bash
# Install Python dependencies
pip install -r requirements-docs.txt
```

## Running Documentation Locally

```bash
# Serve documentation locally (http://localhost:8000)
mkdocs serve

# Build documentation
mkdocs build
```

## Configuration

Configuration is in `mkdocs.yml`. Key features:

- Material theme with teal color scheme
- PlantUML support for diagrams
- Mermaid diagram support
- Full-width content layout

## Documentation Structure

See `mkdocs.yml` for navigation structure. Main sections:

- Home
- **Presentations** — Reveal.js slide decks in the browser (`docs/presentations/`, `mkdocs-revealjs` plugin)
- Overview
- Architecture
- Development
- Configuration

### Browser presentations (`docs/presentations/`)

Markdown files can embed a full-screen Reveal.js deck when front matter sets `revealjs.presentation: true`. See
[presentations/index.md](presentations/index.md) for authoring notes and keyboard shortcuts (**F** / **O** / **S**).

## Related Documentation

- [Development Guide](DEVELOPMENT_GUIDE.md)
- [README](../README.md)

---

*Last updated: 2026-01-27*
