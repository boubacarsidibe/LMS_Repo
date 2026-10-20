// Capture d'écran d'une page (optionnellement connecté) — pour vérifier un écran sans lancer une prise.
//   ./lancer.sh node src/outils/capture-rapide.mjs [--compte email mdp] [--pleine] <chemin> <fichier.png>
import { chromium } from '/opt/pw/node_modules/playwright/index.mjs';
const args = process.argv.slice(2);
let compte = null, pleine = false;
if (args[0] === '--compte') { compte = { email: args[1], mdp: args[2] }; args.splice(0, 3); }
if (args[0] === '--pleine') { pleine = true; args.shift(); }
const [chemin, fichier] = args;
const BASE = process.env.BASE_URL ?? 'http://localhost:4200';
const b = await chromium.launch({ args: ['--hide-scrollbars'] });
const ctx = await b.newContext({ viewport: { width: 1280, height: 720 }, locale: 'fr-FR' });
const page = await ctx.newPage();
page.on('response', (r) => { if (r.status() >= 400 && r.url().includes('/api/')) console.log(`HTTP ${r.status()} ${r.url().replace(BASE, '')}`); });
if (compte) {
  await page.goto(`${BASE}/auth/login`, { waitUntil: 'networkidle' });
  await page.fill('#email', compte.email); await page.fill('#password', compte.mdp);
  await Promise.all([page.waitForURL((u) => !u.pathname.startsWith('/auth'), { timeout: 20000 }), page.click('button[type=submit]')]);
}
await page.goto(`${BASE}${chemin}`, { waitUntil: 'domcontentloaded' });
await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {});
await page.waitForTimeout(1200);
await page.screenshot({ path: `/work/sortie/${fichier}`, fullPage: pleine });
console.log(`→ sortie/${fichier}`);
await b.close();
