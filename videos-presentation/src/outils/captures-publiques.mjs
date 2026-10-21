// Captures de pages publiques (sans connexion). ./lancer.sh node src/outils/captures-publiques.mjs <dossier> /chemin1 /chemin2 …
import { chromium } from '/opt/pw/node_modules/playwright/index.mjs';
import fs from 'node:fs';
const [, , nom, ...chemins] = process.argv;
const BASE = process.env.BASE_URL ?? 'http://localhost:4200';
const dossier = `/work/sortie/captures/${nom}`; fs.mkdirSync(dossier, { recursive: true });
const b = await chromium.launch({ args: ['--hide-scrollbars', '--force-color-profile=srgb', '--force-device-scale-factor=1', '--window-size=1280,720'] });
const ctx = await b.newContext({ viewport: null, locale: 'fr-FR', timezoneId: 'Africa/Dakar' });
const page = await ctx.newPage();
const problemes = [];
page.on('response', (r) => r.status() >= 400 && r.url().includes('/api/') && problemes.push(`HTTP ${r.status()} ${r.url().replace(BASE, '')}`));
for (const c of chemins) {
  problemes.length = 0;
  await page.goto(`${BASE}${c}`, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle', { timeout: 8000 }).catch(() => {});
  await page.waitForTimeout(900);
  const decalages = (process.env.DEFILE ?? '0').split(',').map(Number);
  const f = c.replace(/^\//, '').replace(/[^a-z0-9]+/gi, '-') || 'racine';
  for (const d of decalages) {
    await page.evaluate((delta) => {
      let e = document.elementFromPoint(window.innerWidth * 0.6, window.innerHeight * 0.55);
      while (e && !(e.scrollHeight > e.clientHeight + 4 && /(auto|scroll)/.test(getComputedStyle(e).overflowY))) e = e.parentElement;
      (e || document.scrollingElement).scrollTop = delta;
    }, d);
    await page.waitForTimeout(500);
    await page.screenshot({ path: `${dossier}/${f}${decalages.length > 1 ? '-' + d : ''}.png` });
  }
  console.log(`${c.padEnd(50)} ${problemes.length ? '⚠ ' + [...new Set(problemes)].join(' | ') : 'ok'}`);
}
await b.close();
