// Mise au point : une page « se fige »-t-elle après une action ? Mesure le temps de réponse du thread principal.
//   ./lancer.sh node src/outils/diag-gel.mjs --compte email mdp <chemin> <clic>
import { chromium } from '/opt/pw/node_modules/playwright/index.mjs';
const args = process.argv.slice(2);
const compte = { email: args[1], mdp: args[2] };
const [chemin, clic] = args.slice(3);
const BASE = process.env.BASE_URL ?? 'http://localhost:4200';
const b = await chromium.launch({ args: ['--lang=fr-FR'] });
const page = await (await b.newContext({ viewport: { width: 1280, height: 720 }, locale: 'fr-FR' })).newPage();
page.on('console', (m) => { if (['error', 'warning'].includes(m.type())) console.log('console', m.type(), m.text().slice(0, 200)); });
page.on('pageerror', (e) => console.log('pageerror', String(e).slice(0, 200)));
await page.goto(`${BASE}/auth/login`, { waitUntil: 'networkidle' });
await page.fill('#email', compte.email); await page.fill('#password', compte.mdp);
await Promise.all([page.waitForURL((u) => !u.pathname.startsWith('/auth')), page.click('button[type=submit]')]);
await page.goto(`${BASE}${chemin}`, { waitUntil: 'networkidle' });
const t0 = Date.now();
await page.locator(clic).first().click({ timeout: 10000 });
for (let i = 0; i < 6; i++) {
  const t = Date.now();
  const ok = await Promise.race([page.evaluate(() => document.body.innerText.length), new Promise((r) => setTimeout(() => r('GEL'), 4000))]);
  console.log(`t+${((Date.now() - t0) / 1000).toFixed(1)}s réponse en ${Date.now() - t} ms →`, ok);
  if (ok === 'GEL') break;
}
await page.waitForTimeout(3000);
console.log('après 3 s : longueur du texte', await Promise.race([page.evaluate(() => document.body.innerText.length), new Promise((r) => setTimeout(() => r('GEL'), 4000))]));
const n = await Promise.race([page.locator('button:text-is("Conforme")').count(), new Promise((r) => setTimeout(() => r('GEL'), 4000))]);
console.log('boutons Conforme :', n);
if (typeof n === 'number' && n > 0) {
  const t1 = Date.now();
  await page.locator('button:text-is("Conforme")').first().click({ timeout: 8000 }).catch((e) => console.log('clic Conforme échec', e.message.split('\n')[0]));
  console.log('clic Conforme en', Date.now() - t1, 'ms');
  console.log('réponse après :', await Promise.race([page.evaluate(() => 1), new Promise((r) => setTimeout(() => r('GEL'), 4000))]));
}
await b.close();
