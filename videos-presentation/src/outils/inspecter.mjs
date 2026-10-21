// Aide à l'écriture des scénarios : liste ce qu'une page expose (liens, boutons, champs, titres) ou ce que correspond
// un sélecteur, avec sa boîte à l'écran.
//   ./lancer.sh node src/outils/inspecter.mjs [--compte email mdp] [--clic <sel>]... <chemin> [selecteur]
import { chromium } from '/opt/pw/node_modules/playwright/index.mjs';
const args = process.argv.slice(2);
let compte = null;
if (args[0] === '--compte') { compte = { email: args[1], mdp: args[2] }; args.splice(0, 3); }
// --clic <sélecteur> (répétable) : clics joués avant l'inventaire, pour voir ce qu'une action fait apparaître.
const clics = [];
while (args[0] === '--clic') { clics.push(args[1]); args.splice(0, 2); }
const [chemin, selecteur] = args;
const BASE = process.env.BASE_URL ?? 'http://localhost:4200';
const b = await chromium.launch({ args: ['--hide-scrollbars', '--force-device-scale-factor=1', '--window-size=1280,720'] });
const ctx = await b.newContext({ viewport: null, locale: 'fr-FR' });
const page = await ctx.newPage();
if (compte) {
  await page.goto(`${BASE}/auth/login`, { waitUntil: 'networkidle' });
  await page.fill('#email', compte.email); await page.fill('#password', compte.mdp);
  await Promise.all([page.waitForURL((u) => !u.pathname.startsWith('/auth'), { timeout: 20000 }), page.click('button[type=submit]')]);
}
await page.goto(`${BASE}${chemin}`, { waitUntil: 'domcontentloaded' });
await page.waitForLoadState('networkidle', { timeout: 8000 }).catch(() => {});
await page.waitForTimeout(1200);
for (const c of clics) { await page.locator(c).first().click(); await page.waitForTimeout(900); }
const rapport = await page.evaluate((sel) => {
  const vis = (e) => { const r = e.getBoundingClientRect(); const s = getComputedStyle(e); return r.width > 2 && r.height > 2 && s.visibility !== 'hidden' && s.display !== 'none'; };
  const decrire = (e) => {
    const r = e.getBoundingClientRect();
    const txt = (e.innerText || e.value || e.getAttribute('aria-label') || e.getAttribute('placeholder') || '').replace(/\s+/g, ' ').trim().slice(0, 60);
    return `${e.tagName.toLowerCase()}${e.id ? '#' + e.id : ''}${e.getAttribute('formcontrolname') ? '[fc=' + e.getAttribute('formcontrolname') + ']' : ''}${e.getAttribute('type') ? '[type=' + e.getAttribute('type') + ']' : ''} (${Math.round(r.x)},${Math.round(r.y)} ${Math.round(r.width)}×${Math.round(r.height)}) « ${txt} »${e.checked ? ' ☑' : ''}`;
  };
  if (sel) return [...document.querySelectorAll(sel)].map((e) => (vis(e) ? '' : '[caché] ') + decrire(e));
  const groupes = { titres: 'h1,h2,h3', liens: 'a[href]', boutons: 'button', champs: 'input,select,textarea' };
  const sortie = [];
  for (const [nom, s] of Object.entries(groupes)) {
    sortie.push(`── ${nom}`);
    [...document.querySelectorAll(s)].filter(vis).slice(0, 60).forEach((e) => sortie.push('  ' + decrire(e) + (e.getAttribute('href') ? ' → ' + e.getAttribute('href') : '')));
  }
  return sortie;
}, selecteur);
console.log(rapport.join('\n'));
await b.close();
