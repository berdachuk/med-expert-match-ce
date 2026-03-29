# Presentations (browser)

This section contains **Reveal.js** slide decks embedded in the MkDocs site. They open in the same documentation server as the rest of the project (`mkdocs serve`).

## How to view

1. Install docs dependencies: `pip install -r requirements-docs.txt`
2. Run: `mkdocs serve`
3. Open **Presentations** in the navigation and choose a deck.

## Controls (during presentation)

- **F** — fullscreen  
- **O** — slide overview  
- **S** — speaker view (with notes)  
- Arrow keys — navigate slides  

## Authoring

- Decks are Markdown files under `docs/presentations/`.
- Enable a deck with YAML front matter:

```yaml
---
revealjs:
  presentation: true
---
```

- Separate **horizontal** slides with a blank line, `---`, and a blank line (not the YAML delimiter at the top — that is only the first front matter block).
- See the [mkdocs-revealjs documentation](https://rod2ik.gitlab.io/mkdocs-revealjs/) for vertical slides, speaker notes, and themes.

## Decks

| Page | Description |
|------|-------------|
| [Video (1 slide)](medexpertmatch-video-one-slide.md) | Single slide with embedded YouTube: [watch?v=1KyndK-efKs](https://www.youtube.com/watch?v=1KyndK-efKs) |
| [Sales pitch (~3 min)](medexpertmatch-sales-3min.md) | Short Reveal deck aligned with [PRESENTATION_SALES_3MIN.md](../PRESENTATION_SALES_3MIN.md): problem, solution, GraphRAG, capabilities, live demo cue, CTA |
| [Full talk (45 min, English)](medexpertmatch-full-presentation.md) | Complete conference deck: stack motivation, problems, architecture (SGR), demo flow, conclusion |

Related speaker script and timing (Russian): [PRESENTATION_PLAN_RU.md](../PRESENTATION_PLAN_RU.md).
