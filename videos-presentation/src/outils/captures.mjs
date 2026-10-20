// Captures ciblées : se connecte puis photographie une liste de chemins (utile pour repérer les pages avant de scénariser).
//   ./lancer.sh node src/outils/captures.mjs <email> <mot-de-passe> <dossier> /chemin1 /chemin2 …
import { chromium } from '/opt/pw/node_modules/playwright/index.mjs';
import fs from 'node:fs';
const [, , email, mdp, nom, ...chemins] = process.argv;
const BASE = process.env.BASE_URL ?? 'http://localhost:4200';
const dossier = `/work/sortie/captures/${nom}`;
fs.mkdirSync(dossier, { recursive: true });
const b = await chromium.launch({ args: ['--hide-scrollbars', '--force-color-profile=srgb', '--force-device-scale-factor=1', '--window-size=1280,720'] });
const ctx = await b.newContext({ viewport: null, locale: 'fr-FR', timezoneId: 'Africa/Dakar' });
const page = await ctx.newPage();
const problemes = [];
page.on('response', (r) => r.status() >= 400 && r.url().includes('/api/') && problemes.push(`HTTP ${r.status()} ${r.url().replace(BASE, '')}`));
page.on('pageerror', (e) => problemes.push(`exception ${e}`));
await page.goto(`${BASE}/auth/login`, { waitUntil: 'networkidle' });
await page.fill('#email', email); await page.fill('#password', mdp);
await Promise.all([page.waitForURL((u) => !u.pathname.startsWith('/auth'), { timeout: 20000 }), page.click('button[type=submit]')]);
await page.waitForLoadState('networkidle').catch(() => {});
for (const c of chemins) {
  problemes.length = 0;
  await page.goto(`${BASE}${c}`, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle', { timeout: 8000 }).catch(() => {});
  await page.waitForTimeout(900);
  const f = c.replace(/^\//, '').replace(/[^a-z0-9]+/gi, '-') || 'racine';
  // DEFILE=600,1200 : prend une capture après chaque défilement (le contenu défile dans un conteneur interne, pas dans la page).
  const decalages = (process.env.DEFILE ?? '0').split(',').map(Number);
  for (const d of decalages) {
    await page.evaluate((delta) => {
      let e = document.elementFromPoint(window.innerWidth * 0.6, window.innerHeight * 0.55);
      while (e && !(e.scrollHeight > e.clientHeight + 4 && /(auto|scroll)/.test(getComputedStyle(e).overflowY))) e = e.parentElement;
      (e || document.scrollingElement).scrollTop = delta;
    }, d);
    await page.waitForTimeout(400);
    await page.screenshot({ path: `${dossier}/${f}${decalages.length > 1 ? '-' + d : ''}.png` });
  }
  console.log(`${c.padEnd(60)} ${problemes.length ? '⚠ ' + [...new Set(problemes)].join(' | ') : 'ok'}`);
}
await b.close();
