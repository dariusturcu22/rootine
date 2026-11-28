#!/usr/bin/env node
// Render every slide of a Rootine deck to a 1920x1080 PNG.
// Usage: node export-slides.mjs <deck-folder-or-index.html> <output-folder>
// Needs Playwright: `npm i -D playwright` (or playwright-core) and a Chromium build.
// Optional env: CHROME_PATH=<path to chrome/chromium executable> to reuse an installed browser.
import { pathToFileURL } from 'node:url'
import { existsSync, mkdirSync, statSync } from 'node:fs'
import { resolve, join } from 'node:path'

const [, , deckArg, outArg] = process.argv
if (!deckArg || !outArg) {
  console.error('Usage: node export-slides.mjs <deck-folder-or-index.html> <output-folder>')
  process.exit(2)
}

let pw
try {
  pw = await import('playwright')
} catch {
  try {
    pw = await import('playwright-core')
  } catch {
    console.error('Playwright is not installed. Run: npm i -D playwright (then npx playwright install chromium)')
    process.exit(3)
  }
}

const deckPath = resolve(deckArg)
const indexFile = statSync(deckPath).isDirectory() ? join(deckPath, 'index.html') : deckPath
if (!existsSync(indexFile)) {
  console.error('index.html not found: ' + indexFile)
  process.exit(2)
}
const outDir = resolve(outArg)
mkdirSync(outDir, { recursive: true })

const launchOptions = process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH } : {}
const browser = await pw.chromium.launch(launchOptions)
const page = await browser.newPage({ viewport: { width: 1920, height: 1080 }, deviceScaleFactor: 1 })
const problems = []
page.on('pageerror', (e) => problems.push(e.message))
page.on('requestfailed', (r) => problems.push('request failed: ' + r.url()))

await page.goto(pathToFileURL(indexFile).href + '?export')
await page.evaluate(() => document.fonts.ready)
const count = await page.locator('.slide').count()

for (let n = 1; n <= count; n++) {
  await page.waitForTimeout(900) // let the short entry animation finish
  const file = join(outDir, 'slide-' + String(n).padStart(2, '0') + '.png')
  await page.screenshot({ path: file })
  console.log('saved ' + file)
  await page.keyboard.press('ArrowRight')
}
await browser.close()

if (problems.length) {
  console.error('Problems found while rendering:\n' + problems.join('\n'))
  process.exit(1)
}
console.log('Done: ' + count + ' slides in ' + outDir)
