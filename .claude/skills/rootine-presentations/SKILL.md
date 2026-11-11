---
name: rootine-presentations
description: Use when building or updating any Rootine presentation or slide deck (demo videos, sprint reviews, pitches). Gives the design language (warm editorial), the slide types, content and accessibility rules, a copy-and-fill template with fonts, and a PNG export script.
---

# Rootine presentations

Decks are plain HTML, CSS and JS. No framework, no build step. They open by double-click from `file://` and work offline. A fixed 1920x1080 stage is scaled to the window.

Everything lives in `template/`. A new deck is a copy of that folder with the slides edited.

## Build a new deck

1. Copy `template/` to `docs/presentations/<deck-name>/`.
2. Edit `index.html`: keep one `<section class="slide ...">` per slide, delete the example types you do not need, and set the `<title>`.
3. Put real screenshots and photos in `assets/`. Keep each image under 200 KB (JPEG, quality about 85).
4. Open `index.html` in a browser. Press `N` for notes and `T` for the seconds badge.
5. Write `FACTS.md` next to the deck: one row per claim (slide, claim, evidence, date, status).
6. Export with the script below and look at every PNG.

Do not change `deck.js` or `deck.css` for one deck. If a new slide type or rule is needed, change the template and say so in the PR.

## Keys and runtime

Right, Down, Space: next. Left, Up: previous. `Home` and `End`: first and last. `F`: fullscreen. `N`: speaker notes. `T`: seconds badge (from `data-seconds`). The URL hash opens a slide (`index.html#3`). `?export` hides the counter. Notes and the badge are off by default.

## Design language

Mood: warm editorial. Paper background, deep forest green, strong serif display type, thin rules, generous space, asymmetric layouts. Never purple, never gradients, no emoji icons, no stock-template look.

| Token | Value | Use |
|---|---|---|
| `--paper` | `#f4efe4` | background |
| `--ink` | `#1d2a1b` | body text, rules |
| `--forest` | `#1f4a2a` | titles, dark panels |
| `--green` | `#2e7d32` | brand green, emphasis, numbers |
| `--bark` | `#6a4e44` | kickers, captions, sources |
| `--leaf` | `#c5e1a5` | highlights, shadows of screenshots |
| `--warn`, `--bad`, `--ok` | `#8a5a00`, `#9b2c20`, `#1f4a2a` | risk or status text (always with an icon or word) |

`--green` on `--paper` is about 4.4:1: use it only for large text (24 px and up) and for fills, never for small text.

Brand source: Version 2 (Nunito, brand green `#2e7d32`, `#c5e1a5`, bark `#8d6e63` darkened to `#6a4e44` for text contrast).

Type: Fraunces for titles (`h1` 150 px, statement 136 px, `h2` 78 to 104 px), Nunito for everything else (body 32 to 38 px, kicker 22 px caps with letter spacing, captions 22 px). Italic Fraunces in green is the one emphasis device: `<em>`, at most one per slide. Nothing under 22 px on a slide.

Spacing: 8 px base, page margins 120 px horizontally and 96 px vertically (`--mx`, `--my`). Leave the bottom 100 px free for the source line.

Logo: the leaf mark is inline SVG (`<symbol id="leaf">`, copied from `frontend/src/layouts/MainLayout.tsx`) next to the word "Rootine" set in Nunito 800. Do not redraw it. Use `--leaf-outline` to switch the outline colour on dark panels.

## Slide types

Use these seven types. Each example is in `template/index.html`.

| Class | Use it for | Rule |
|---|---|---|
| `s-title` | first slide | name, one promise line, team |
| `s-statement` | one idea | one sentence, optional one-line sub text |
| `s-feature` | a feature with a visual | heading, one line of text, one or two real screenshots (`shot a/b` or `shot solo`) |
| `s-numbers` | big figures | 2 to 4 numbers (`cols-2`, `cols-3`), each sourced |
| `s-flow` | process or architecture | 3 to 5 steps (`steps-3`, `steps-5`), last step has class `end` |
| `s-divider` | cut to a screen recording | one word, about 2 seconds |
| `s-close` | end or next steps | thank-you line, or `ol.todo` with at most 5 items |

To add a slide: copy the closest example `<section>`, change its text, set `data-seconds` and fill the `<aside class="notes">`. Slide order is document order.

## Content rules

- One idea per slide. A few words, big visuals. A voice-over of 15 to 30 seconds per slide at most.
- Romanian, with correct diacritics (ă â î ș ț, comma-below). Keep technical terms in English (backend, endpoint, pipeline, Spring Boot, ONNX).
- Real facts only. Every number, feature or claim must be in the repo, a PR, a Release, a command output or a document the team named. Put each in `FACTS.md`. A slide with a number carries a `.source` line. If it cannot be verified, leave it out.
- Do not present as done what is only in an open PR; show it as "în lucru" or leave it out.
- No lorem ipsum, no "Disease 1" or "Parcel 1" placeholders, no emoji, no stock images. Screenshots must be real and must not show tokens, secrets or personal data.
- Photos from datasets: small JPEGs (under 100 KB), with the credit on the slide. Never commit a dataset.
- Speaker notes list topics for the voice-over, not a script.
- Drafts (for example plans for the next demo) are marked "ciornă" in `FACTS.md`, and kept short and easy to edit.

## Motion and accessibility

- Motion is subtle and ends fast (about 0.5 s): the `anim` and `d1`..`d4` classes. The deck is screen-recorded.
- `prefers-reduced-motion` turns animation off (already in `deck.css`).
- Text contrast at least WCAG AA. Use only the tokens above.
- Risk or status is never colour alone: use `<span class="status ok|warn|bad">` which adds an icon, and always write the word.
- Every `<img>` has a Romanian `alt`. Slides have `aria-label`. Fonts are self-hosted woff2 (Nunito, Fraunces, open licence SIL OFL), never from a CDN.

## Export to PNG

Needs Playwright and a Chromium build (`npm i -D playwright`, then `npx playwright install chromium`, or set `CHROME_PATH` to an installed Chrome or Chromium).

```bash
node .claude/skills/rootine-presentations/scripts/export-slides.mjs docs/presentations/<deck-name> <output-folder>
```

It writes `slide-01.png`, `slide-02.png`, ... at 1920x1080. Keep the output folder outside the repo unless the team asks for it.

## Checklist before delivering

- [ ] Every slide is one idea, with few words and a big visual.
- [ ] Every number and claim is in `FACTS.md` with evidence and date; nothing from an open PR is shown as done.
- [ ] Romanian text checked for diacritics (no `s,` or `ş` with cedilla; use `ș ț`).
- [ ] No placeholder text, no emoji, no secrets or personal data in screenshots.
- [ ] Credits shown where needed (dataset, map tiles).
- [ ] Risk or status has an icon or a word, not only colour.
- [ ] `N` and `T` work and are off by default; every slide has `data-seconds` and notes.
- [ ] Opens from `file://` with no network, no console errors, fonts load.
- [ ] Exported PNGs looked at one by one: no overflow, no cut text, no wrapped orphan words.
- [ ] No attribution lines anywhere (commits, PR, files). Conventional Commits with the folder as scope, for example `docs(presentations): ...`.
