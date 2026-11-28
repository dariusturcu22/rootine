# Presentations

Slide decks for Rootine are plain HTML. They open by double-click, work offline and are built from one template.

| Deck | Folder | Purpose |
|---|---|---|
| Demo 1 | [demo-1](demo-1/) | Slides with voice-over for the Demo 1 video (13 November 2026) |

## How to make a deck with the skill

The skill lives in `.claude/skills/rootine-presentations/`. Steps:

1. Copy `.claude/skills/rootine-presentations/template/` to `docs/presentations/<deck-name>/`.
2. Edit `index.html`: one `<section class="slide ...">` per slide. Slide types: title, statement, feature, numbers, flow, divider, closing.
3. Add real screenshots to `assets/`. Write only facts you can prove, and list each one in a `FACTS.md` next to the deck.
4. Open `index.html`. Keys: arrows or space to move, `F` fullscreen, `N` speaker notes, `T` seconds per slide.
5. Export PNGs (needs Playwright and Chromium):

```bash
node .claude/skills/rootine-presentations/scripts/export-slides.mjs docs/presentations/<deck-name> <output-folder>
```

Rules, design tokens and the delivery checklist are in `.claude/skills/rootine-presentations/SKILL.md`.
