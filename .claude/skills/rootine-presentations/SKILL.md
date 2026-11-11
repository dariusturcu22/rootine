---
name: rootine-presentations
description: Use when building or updating any Rootine presentation or slide deck (demo videos, sprint reviews, pitches). Gives the design language (flat colour fields, Nunito Black, the logo leaf), the slide types, plain-Romanian content rules, accessibility rules, a copy-and-fill template with fonts, and a PNG export script.
---

# Rootine presentations

Decks are plain HTML, CSS and JS. No framework, no build step. They open by double-click from `file://` and work offline. A fixed 1920x1080 stage is scaled to the window.

Everything lives in `template/`. A new deck is a copy of that folder with the slides edited.

## Build a new deck

1. Copy `template/` to `docs/presentations/<deck-name>/`.
2. Edit `index.html`: keep one `<section class="slide ...">` per slide, delete the example types you do not need, and set the `<title>`.
3. Put real screenshots and photos in `assets/` (JPEG, quality about 85, under 300 KB each).
4. Open `index.html` in a browser. Press `N` for notes and `T` for the seconds badge.
5. Write `FACTS.md` next to the deck: one row per claim (slide, claim, evidence, date, status).
6. Export with the script below and look at every PNG.

Do not change `deck.js` or `deck.css` for one deck. If a new slide type or rule is needed, change the template and say so in the PR.

## Keys and runtime

Right, Down, Space: next. Left, Up: previous. `Home` and `End`: first and last. `F`: fullscreen. `N`: speaker notes. `T`: seconds badge (from `data-seconds`). The URL hash opens a slide (`index.html#3`). `?export` hides the counter. Notes and the badge are off by default.

## Design language

Flat colour fields taken from the app, one heavy typeface, and the logo leaf used as a big shape. Each slide sits on one field: `on-green`, `on-pale` or `on-bark` (alternate them). Never purple, never gradients, no emoji.

| Token | Value | Use |
|---|---|---|
| `--green` | `#2e7d32` | brand green: green slides, big figures |
| `--pale` | `#e9eee4` | pale slides (the app background) |
| `--bark` | `#7a5c51` | brown slides and the screenshot band (darkened from the app's `#8d6e63` for contrast with white text) |
| `--mint` | `#c5e1a5` | highlight (`.mark`), result boxes, list bullets |
| `--ink` | `#1f2b22` | text on pale and on white |
| `--ok`, `--warn`, `--bad` | `#1f5f25`, `#7a4f00`, `#9b2c20` | status text, always with an icon and a word (`.chip`) |

Type: Nunito only (weights 600 to 900), sentence case, tight letter spacing on headings. Headings 78 to 150 px, body 36 to 46 px, nothing under 28 px.

Leaf: the logo is the inline SVG `#leaf` (copied from `frontend/src/layouts/MainLayout.tsx`) next to the word "Rootine" in Nunito 900. Do not redraw it. `#leafshape` is the same leaf as one flat shape, placed big and faint as `.leafbg` (`br`, `tl` or `r`), one per slide.

Do not add: small kicker labels above headings, footers or "source" lines, numbered markers, hairline rules, serif or italic emphasis, more than one `.mark` per slide. Credits that the licence requires (dataset, map tiles) go in the slide's text, in a normal sentence at 30 px.

## Slide types

Seven types. Each example is in `template/index.html`.

| Class | Use it for | Rule |
|---|---|---|
| `s-title` | first slide | logo, headline in two lines, one plain sentence |
| `s-statement` | one idea | one sentence, optional one-line sub text |
| `s-feature` | a feature with a visual | heading, one line, one to three real screenshots on the brown band (`shot solo` or `t1`, `t2`, `t3`) |
| `s-numbers` | figures | 2 or 3 rows (`.row`: figure plus one sentence), each figure sourced |
| `s-flow` | process or architecture | a `.stack` of three boxes, or an inline `svg.diagram` (see the backend slide of the Demo 1 deck) |
| `s-divider` | cut to a screen recording | one word, about 2 seconds |
| `s-close` | end or next steps | heading and at most 5 items; the leaf is the bullet |

To add a slide: copy the closest example `<section>`, change its text, set `data-seconds` and fill the `<aside class="notes">`. Slide order is document order.

## Content rules

- One idea per slide, a few words, big visuals. A voice-over of 15 to 30 seconds per slide at most.
- Write the way a person talks: short, plain sentences, no slogans, no English calques. Romanian with correct diacritics (ă â î ș ț, comma-below). "de" before nouns after numerals above 19 (`3.064 de imagini`). Hyphen only where the Romanian spelling needs it (`PR-ul`, `e-mail`, but `endpointuri`). Keep technical terms in English (backend, CI, Spring Boot, ONNX). Never use em dashes.
- Have the Romanian text read by a second reader before delivery.
- Real facts only. Every number, feature or claim must be in the repo, a PR, a Release, a command output or a document the team named. Put each in `FACTS.md`. If it cannot be verified, leave it out.
- Do not present as done what is only in an open PR.
- No lorem ipsum, no "Disease 1" or "Parcel 1" placeholders, no emoji, no stock images. Screenshots must be real, from the running app, and must not show tokens, secrets or personal data. Prefer the full stack (`docker compose`) over the mock API.
- Photos from datasets: small JPEGs (under 100 KB), credit in the slide text. Never commit a dataset.
- Speaker notes list ideas for the voice-over, not a script.
- Drafts (for example plans for the next demo) are marked "ciornă" in `FACTS.md` and in an HTML comment, not on the slide, and kept short and easy to edit.

## Motion and accessibility

- Motion is subtle and ends fast (about 0.5 s): the `anim` and `d1`..`d4` classes. The deck is screen-recorded.
- `prefers-reduced-motion` turns animation off (already in `deck.css`).
- Text contrast at least WCAG AA. Use only the tokens above.
- Risk or status is never colour alone: `<span class="chip ok|warn|bad">` adds an icon, and the word is always written.
- Every `<img>` has a Romanian `alt`. Slides have `aria-label`. The font is self-hosted woff2 (Nunito, SIL OFL), never from a CDN.

## Export to PNG

Needs Playwright and a Chromium build (`npm i -D playwright`, then `npx playwright install chromium`, or set `CHROME_PATH` to an installed Chrome or Chromium).

```bash
node .claude/skills/rootine-presentations/scripts/export-slides.mjs docs/presentations/<deck-name> <output-folder>
```

It writes `slide-01.png`, `slide-02.png`, ... at 1920x1080. Keep the output folder outside the repo unless the team asks for it.

## Checklist before delivering

- [ ] Every slide is one idea, with few words and a big visual.
- [ ] No kicker labels, footers, source lines, numbered markers, serif or em dashes.
- [ ] Every number and claim is in `FACTS.md` with evidence and date; nothing from an open PR is shown as done.
- [ ] Romanian read by a second person: plain wording, diacritics, "de" after numerals.
- [ ] No placeholder text, no emoji, no secrets or personal data in screenshots.
- [ ] Credits shown where the licence needs them (dataset, map tiles).
- [ ] Risk or status has an icon and a word.
- [ ] `N` and `T` work and are off by default; every slide has `data-seconds` and notes.
- [ ] Opens from `file://` with no network, no console errors, the font loads.
- [ ] Exported PNGs looked at one by one: no overflow, no cut text, no orphan words.
- [ ] No attribution lines anywhere (commits, PR, files). Conventional Commits with the folder as scope, for example `docs(presentations): ...`.
