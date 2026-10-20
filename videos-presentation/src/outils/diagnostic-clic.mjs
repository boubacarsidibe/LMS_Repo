// Mise au point : qu'est-ce qui recouvre un lien après une action ? Rejoue une séquence de clics puis, pour le sélecteur
// donné, affiche l'élément réellement situé au centre de la cible (elementFromPoint).
//   ./lancer.sh node src/outils/diagnostic-clic.mjs --compte email mdp [--clic sel]... <chemin> <cible>
import { chromium } from '/opt/pw/node_modules/playwright/index.mjs';
const args = process.argv.slice(2);
let compte = null;
if (args[0] === '--compte') { compte = { email: args[1], mdp: args[2] }; args.splice(0, 3); }
const clics = [];
while (args[0] === '--clic') { clics.push(args[1]); args.splice(0, 2); }
const [chemin, cible] = args;
const BASE = process.env.BASE_URL ?? 'http://localhost:4200';
const b = await chromium.launch({ args: ['--hide-scrollbars', '--lang=fr-FR'] });
const ctx = await b.newContext({ viewport: { width: 1280, height: 720 }, locale: 'fr-FR' });
const page = await ctx.newPage();
if (compte) {
  await page.goto(`${BASE}/auth/login`, { waitUntil: 'networkidle' });
  await page.fill('#email', compte.email); await page.fill('#password', compte.mdp);
  await Promise.all([page.waitForURL((u) => !u.pathname.startsWith('/auth'), { timeout: 20000 }), page.click('button[type=submit]')]);
}
await page.goto(`${BASE}${chemin}`, { waitUntil: 'networkidle' });
for (const c of clics) { await page.locator(c).first().click(); await page.waitForTimeout(700); }
await page.waitForTimeout(1500);
const r = await page.evaluate((sel) => {
  const el = document.querySelector(sel);
  if (!el) return 'cible introuvable';
  const bb = el.getBoundingClientRect();
  const x = bb.x + bb.width / 2, y = bb.y + bb.height / 2;
  const haut = document.elementFromPoint(x, y);
  const decrire = (e) => e ? `${e.tagName.toLowerCase()}.${(e.className || '').toString().slice(0, 120)} pe=${getComputedStyle(e).pointerEvents} href=${e.getAttribute && e.getAttribute('href')}` : 'null';
  return { cible: decrire(el), point: [Math.round(x), Math.round(y)], haut: decrire(haut), memeElement: haut === el, contient: !!haut && el.contains(haut), url: location.pathname };
}, cible);
console.log(JSON.stringify(r, null, 1));
await page.locator(cible).first().click({ timeout: 3000 }).catch((e) => console.log('clic playwright : échec', e.message.split('\n')[0]));
await page.waitForTimeout(1200);
console.log('URL après clic :', new URL(page.url()).pathname);
await b.close();
