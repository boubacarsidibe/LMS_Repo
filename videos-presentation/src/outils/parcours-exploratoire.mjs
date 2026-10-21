// Parcours exploratoire : pour chaque compte, se connecte comme un utilisateur réel, relève les liens de
// navigation de son espace, visite chaque page et signale ce qui cloche (erreur console, requête en échec,
// page vide, redirection inattendue). Les captures servent ensuite à écrire les scénarios vidéo.
//
//   docker run --rm --network host -v "$PWD":/work lms-videos node src/outils/parcours-exploratoire.mjs [role ...]
//
import { chromium } from '/opt/pw/node_modules/playwright/index.mjs';
import fs from 'node:fs';
import path from 'node:path';

const BASE = process.env.BASE_URL ?? 'http://localhost:4200';
const SORTIE = process.env.SORTIE ?? '/work/sortie/exploration';

const COMPTES = [
  { role: 'candidat', email: 'test.candidat2@inpg.sn', mdp: 'Test@2026' },
  { role: 'etudiant', email: 'test.etudiant@inpg.sn', mdp: 'Test@2026' },
  { role: 'formateur', email: 'prof.demo@inpg.sn', mdp: 'Demo@2026' },
  { role: 'admission', email: 'test.admission@inpg.sn', mdp: 'Test@2026' },
  { role: 'pedagogie', email: 'test.pedago@inpg.sn', mdp: 'Test@2026' },
  { role: 'alumni', email: 'test.alumni@inpg.sn', mdp: 'Test@2026' },
  { role: 'ecole', email: 'test.ecole@inpg.sn', mdp: 'Test@2026' },
  { role: 'evaluateur', email: 'test.evaluateur@inpg.sn', mdp: 'Test@2026' },
  { role: 'entreprise', email: 'test.entreprise@inpg.sn', mdp: 'Test@2026' },
  { role: 'candidat-formateur', email: 'test.candidat.formateur@inpg.sn', mdp: 'Test@2026' },
];

const demandes = process.argv.slice(2);
const aVisiter = demandes.length ? COMPTES.filter((c) => demandes.includes(c.role)) : COMPTES;

const slug = (s) => s.replace(/^\//, '').replace(/[^a-z0-9]+/gi, '-').replace(/-+$/, '') || 'racine';
const rapport = [];

const browser = await chromium.launch();
for (const compte of aVisiter) {
  const dossier = path.join(SORTIE, compte.role);
  fs.mkdirSync(dossier, { recursive: true });
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 720 }, locale: 'fr-FR' });
  const page = await ctx.newPage();

  let problemes = [];
  page.on('console', (m) => {
    if (m.type() === 'error') problemes.push(`console: ${m.text().slice(0, 200)}`);
  });
  page.on('pageerror', (e) => problemes.push(`exception: ${String(e).slice(0, 200)}`));
  page.on('response', (r) => {
    if (r.status() >= 400 && r.url().includes('/api/')) {
      problemes.push(`HTTP ${r.status()} ${r.request().method()} ${r.url().replace(BASE, '')}`);
    }
  });

  const fiche = { role: compte.role, email: compte.email, atterrissage: null, pages: [] };
  try {
    await page.goto(`${BASE}/auth/login`, { waitUntil: 'networkidle' });
    await page.fill('#email', compte.email);
    await page.fill('#password', compte.mdp);
    await Promise.all([
      page.waitForURL((u) => !u.pathname.startsWith('/auth/login'), { timeout: 15000 }).catch(() => {}),
      page.click('button[type=submit]'),
    ]);
    await page.waitForLoadState('networkidle').catch(() => {});
    fiche.atterrissage = new URL(page.url()).pathname;
    if (fiche.atterrissage.startsWith('/auth/login')) {
      const msg = await page.locator('.bg-error-container').first().textContent().catch(() => '');
      fiche.connexion = `ÉCHEC : ${msg?.trim()}`;
      rapport.push(fiche);
      await ctx.close();
      continue;
    }

    // Liens de navigation internes visibles (barres latérales, en-têtes, menus).
    const liens = await page.$$eval('a[href^="/"]', (as) =>
      [...new Set(as.map((a) => a.getAttribute('href')).filter((h) => h && !h.startsWith('/api')))]
    );
    const file = [fiche.atterrissage, ...liens.filter((l) => l !== fiche.atterrissage)];
    const vus = new Set();
    for (const lien of file) {
      const chemin = lien.split('?')[0];
      if (vus.has(chemin) || /\/:/.test(chemin) || /logout|deconnexion/i.test(chemin)) continue;
      vus.add(chemin);
      problemes = [];
      const t0 = Date.now();
      await page.goto(`${BASE}${lien}`, { waitUntil: 'domcontentloaded' }).catch((e) => problemes.push(`goto: ${e.message}`));
      await page.waitForLoadState('networkidle', { timeout: 12000 }).catch(() => {});
      await page.waitForTimeout(600);
      const finale = new URL(page.url()).pathname;
      const titre = (await page.locator('h1').first().textContent({ timeout: 800 }).catch(() => ''))?.trim() ?? '';
      const texte = (await page.locator('body').innerText().catch(() => '')) ?? '';
      const vide = /(aucun|aucune)\s[^\n]{0,60}(trouv|disponible|enregistr|pour le moment|à afficher)/i.test(texte);
      await page.screenshot({ path: path.join(dossier, `${slug(chemin)}.png`) }).catch(() => {});
      fiche.pages.push({
        lien: chemin,
        redirigeVers: finale !== chemin ? finale : undefined,
        titre,
        videApparente: vide || undefined,
        ms: Date.now() - t0,
        problemes: problemes.length ? [...new Set(problemes)] : undefined,
      });
    }
  } catch (e) {
    fiche.erreur = String(e).slice(0, 300);
  }
  rapport.push(fiche);
  await ctx.close();
}
await browser.close();

fs.mkdirSync(SORTIE, { recursive: true });
fs.writeFileSync(path.join(SORTIE, 'rapport.json'), JSON.stringify(rapport, null, 2));
for (const f of rapport) {
  console.log(`\n=== ${f.role} (${f.email}) → ${f.atterrissage ?? '—'} ${f.connexion ?? ''} ${f.erreur ?? ''}`);
  for (const p of f.pages) {
    const flags = [p.redirigeVers && `→ ${p.redirigeVers}`, p.videApparente && 'VIDE?', p.problemes && `⚠ ${p.problemes.join(' | ')}`].filter(Boolean).join('  ');
    console.log(`  ${p.lien.padEnd(52)} ${String(p.ms).padStart(5)}ms  « ${p.titre.slice(0, 50)} » ${flags}`);
  }
}
